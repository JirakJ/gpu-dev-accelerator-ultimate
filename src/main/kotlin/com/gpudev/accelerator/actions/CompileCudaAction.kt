/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.gpudev.accelerator.cuda.CudaFileType
import com.gpudev.accelerator.settings.GpuDevSettings
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain
import com.gpudev.accelerator.tools.NvccCommandBuilder
import com.gpudev.accelerator.tools.NvccCommandOptions
import com.gpudev.accelerator.tools.NvccOutputParser
import com.gpudev.accelerator.tools.ToolArtifact
import com.gpudev.accelerator.tools.ToolArtifactKind
import com.gpudev.accelerator.tools.ToolInvocation
import com.gpudev.accelerator.tools.ToolResult
import java.io.File

class CompileCudaAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = file?.fileType == CudaFileType.INSTANCE
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (file.fileType != CudaFileType.INSTANCE) return
        FileDocumentManager.getInstance().saveAllDocuments()

        val settings = GpuDevSettings.getInstance().state
        val filePath = file.path
        GpuConsoleService.getInstance(project)
            .print("Compiling ${file.name} with nvcc", "Starting… (see Progress for status)")
        GpuToolchain.inBackground(project, "Compiling ${file.name} with nvcc", { _ ->
            compileCuda(project, filePath, settings)
        }, { pair ->
            if (pair == null) {
                notifyMissingNvcc(project)
                return@inBackground
            }
            val (command, result) = pair
            val console = GpuConsoleService.getInstance(project)
            console.print("nvcc: ${command.joinToString(" ")}", result.combined.ifBlank { "(no output)" }, !result.success)
            val errors = NvccOutputParser.errorCount(result.combined)
            val warnings = NvccOutputParser.warningCount(result.combined)
            val group = NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
            if (result.success) {
                group.createNotification(
                    "CUDA compilation succeeded",
                    if (warnings > 0) "$warnings warning(s) — see GPU Dev console" else file.name,
                    NotificationType.INFORMATION
                ).notify(project)
            } else {
                group.createNotification(
                    "CUDA compilation failed",
                    if (result.timedOut) "nvcc timed out" else "$errors error(s) — see GPU Dev console",
                    NotificationType.ERROR
                ).notify(project)
            }
        })
    }

    private fun compileCuda(
        project: Project,
        filePath: String,
        settings: GpuDevSettings.State
    ): Pair<List<String>, ToolResult>? {
        val nvcc = GpuToolchain.nvcc(project)
        if (nvcc == null) {
            LOG.warn("nvcc not found (configured='${settings.nvccPath}', arch='${settings.gpuArch}')")
            return null
        }
        val localOutput = "$filePath.o"
        val includeRoot = project.basePath ?: File(filePath).parentFile.path
        val command = NvccCommandBuilder.compile(
            nvcc,
            filePath,
            localOutput,
            NvccCommandOptions(settings.gpuArch, settings.extraNvccFlags, listOf(includeRoot))
        )
        val invocation = ToolInvocation(
            command,
            File(filePath).parentFile,
            listOf(
                ToolArtifact(File(filePath), ToolArtifactKind.INPUT),
                ToolArtifact(File(localOutput), ToolArtifactKind.OUTPUT),
                ToolArtifact(File(includeRoot), ToolArtifactKind.PATH_REFERENCE)
            )
        )
        return command to GpuToolchain.execute(project, invocation)
    }

    companion object {
        private val LOG = Logger.getInstance(CompileCudaAction::class.java)

        fun notifyMissingNvcc(project: Project) {
            NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
                .createNotification(
                    "nvcc not found",
                    "Install the CUDA Toolkit and add nvcc to PATH, or set its path in Settings | Tools | GPU Dev Accelerator.",
                    NotificationType.WARNING
                ).notify(project)
        }
    }
}
