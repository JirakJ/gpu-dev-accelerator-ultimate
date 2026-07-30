/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.gpudev.accelerator.cuda.CudaFileType
import com.gpudev.accelerator.settings.GpuDevSettings
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain
import com.gpudev.accelerator.tools.NvccOutputParser
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
        GpuToolchain.inBackground(project, "Compiling ${file.name} with nvcc", { _ ->
            val nvcc = GpuToolchain.nvcc() ?: return@inBackground null
            val command = buildList {
                add(nvcc)
                add("-c")
                add(filePath)
                add("-o")
                add("$filePath.o")
                if (settings.gpuArch.isNotBlank()) add("-arch=${settings.gpuArch}")
                if (settings.extraNvccFlags.isNotBlank()) addAll(settings.extraNvccFlags.split(Regex("\\s+")))
            }
            command to GpuToolchain.run(command, File(filePath).parentFile)
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

    companion object {
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
