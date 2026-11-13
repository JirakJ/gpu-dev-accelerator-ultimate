/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.gpudev.accelerator.cuda.CudaFileType
import com.gpudev.accelerator.settings.GpuDevSettings
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain
import com.gpudev.accelerator.tools.NvccCommandBuilder
import com.gpudev.accelerator.tools.NvccCommandOptions
import com.gpudev.accelerator.tools.ToolArtifact
import com.gpudev.accelerator.tools.ToolArtifactKind
import com.gpudev.accelerator.tools.ToolInvocation
import com.gpudev.accelerator.tools.ToolResult
import java.io.File
import java.nio.file.Files

/**
 * Compiles the current .cu file to PTX and opens the result in the editor —
 * the fastest way to see what the compiler actually generated.
 */
class ShowPtxAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == CudaFileType.INSTANCE
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (file.fileType != CudaFileType.INSTANCE) return
        FileDocumentManager.getInstance().saveAllDocuments()

        val settings = GpuDevSettings.getInstance().state
        val filePath = file.path
        GpuConsoleService.getInstance(project)
            .print("Generating PTX for ${file.name}", "Starting nvcc -ptx… (see Progress for status)")
        GpuToolchain.inBackground(project, "Generating PTX for ${file.name}", { _ ->
            generatePtx(project, filePath, settings)
        }, { triple ->
            if (triple == null) {
                CompileCudaAction.notifyMissingNvcc(project)
                return@inBackground
            }
            val (command, result, ptx) = triple
            if (ptx != null) {
                val ptxVirtualFile = LightVirtualFile(
                    "${file.nameWithoutExtension}.ptx",
                    PlainTextFileType.INSTANCE,
                    ptx
                )
                FileEditorManager.getInstance(project).openFile(ptxVirtualFile, true)
            } else {
                GpuConsoleService.getInstance(project)
                    .print("nvcc -ptx: ${command.joinToString(" ")}", result.combined, error = true)
            }
        })
    }

    private fun generatePtx(
        project: Project,
        filePath: String,
        settings: GpuDevSettings.State
    ): Triple<List<String>, ToolResult, String?>? {
        val nvcc = GpuToolchain.nvcc(project)
        if (nvcc == null) {
            LOG.warn(
                "nvcc not found for PTX generation " +
                    "(configured='${settings.nvccPath}', arch='${settings.gpuArch}')"
            )
            return null
        }
        val includeRoot = project.basePath ?: File(filePath).parentFile.path
        val ptxFile = Files.createTempFile("gpudev-", ".ptx").toFile()
        val command = NvccCommandBuilder.ptx(
            nvcc,
            filePath,
            ptxFile.absolutePath,
            NvccCommandOptions(settings.gpuArch, settings.extraNvccFlags, listOf(includeRoot))
        )
        val invocation = ToolInvocation(
            command,
            File(filePath).parentFile,
            listOf(
                ToolArtifact(File(filePath), ToolArtifactKind.INPUT),
                ToolArtifact(ptxFile, ToolArtifactKind.TEMPORARY_OUTPUT),
                ToolArtifact(File(includeRoot), ToolArtifactKind.PATH_REFERENCE)
            )
        )
        return try {
            val result = GpuToolchain.execute(project, invocation)
            val ptx = runCatching {
                ptxFile.takeIf { result.success && it.exists() }?.readText()?.takeIf { it.isNotBlank() }
            }.onFailure { error ->
                LOG.warn("Could not read generated PTX artifact at ${ptxFile.path}", error)
            }.getOrNull()
            val reportedResult = if (result.success && ptx == null) {
                val note = "nvcc succeeded but the PTX artifact was missing or unreadable"
                result.copy(
                    exitCode = -1,
                    stderr = if (result.stderr.isBlank()) note else result.stderr.trimEnd() + "\n" + note
                )
            } else {
                result
            }
            if (!reportedResult.success) {
                LOG.warn(
                    "nvcc -ptx failed for $filePath: exit=${reportedResult.exitCode}, " +
                        "timedOut=${reportedResult.timedOut}: " +
                        reportedResult.combined.take(500)
                )
            }
            Triple(command, reportedResult, ptx)
        } finally {
            runCatching { Files.deleteIfExists(ptxFile.toPath()) }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ShowPtxAction::class.java)
    }
}
