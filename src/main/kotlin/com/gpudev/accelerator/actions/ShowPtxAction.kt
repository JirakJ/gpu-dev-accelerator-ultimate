/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.LightVirtualFile
import com.gpudev.accelerator.cuda.CudaFileType
import com.gpudev.accelerator.settings.GpuDevSettings
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain
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
        GpuToolchain.inBackground(project, "Generating PTX for ${file.name}", { _ ->
            val nvcc = GpuToolchain.nvcc() ?: return@inBackground null
            val ptxFile = Files.createTempFile("gpudev-", ".ptx").toFile()
            val command = buildList {
                add(nvcc)
                add("-ptx")
                add(filePath)
                add("-o")
                add(ptxFile.absolutePath)
                if (settings.gpuArch.isNotBlank()) add("-arch=${settings.gpuArch}")
                if (settings.extraNvccFlags.isNotBlank()) addAll(settings.extraNvccFlags.split(Regex("\\s+")))
            }
            val result = GpuToolchain.run(command, File(filePath).parentFile)
            val ptx = if (result.success && ptxFile.exists()) ptxFile.readText() else null
            ptxFile.delete()
            Triple(command, result, ptx)
        }, { triple ->
            if (triple == null) {
                CompileCudaAction.notifyMissingNvcc(project)
                return@inBackground
            }
            val (command, result, ptx) = triple
            if (ptx != null) {
                val ptxVirtualFile = LightVirtualFile("${file.nameWithoutExtension}.ptx", PlainTextFileType.INSTANCE, ptx)
                FileEditorManager.getInstance(project).openFile(ptxVirtualFile, true)
            } else {
                GpuConsoleService.getInstance(project)
                    .print("nvcc -ptx: ${command.joinToString(" ")}", result.combined, error = true)
            }
        })
    }
}
