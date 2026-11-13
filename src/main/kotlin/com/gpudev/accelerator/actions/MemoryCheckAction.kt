/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain
import com.gpudev.accelerator.tools.SanitizerOutputParser
import com.gpudev.accelerator.tools.ToolArtifact
import com.gpudev.accelerator.tools.ToolArtifactKind
import com.gpudev.accelerator.tools.ToolInvocation

/**
 * Runs compute-sanitizer (fallback: cuda-memcheck) against a CUDA executable
 * picked by the user and reports leaks/invalid accesses in the console.
 */
class MemoryCheckAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Select CUDA Executable to Check")
        val executable = FileChooser.chooseFile(descriptor, project, null) ?: return

        GpuToolchain.inBackground(project, "Running compute-sanitizer on ${executable.name}", { _ ->
            val sanitizer = GpuToolchain.sanitizer(project) ?: return@inBackground null
            val command = listOf(sanitizer, "--tool", "memcheck", executable.path)
            GpuToolchain.execute(
                project,
                ToolInvocation(
                    command,
                    artifacts = listOf(ToolArtifact(java.io.File(executable.path), ToolArtifactKind.INPUT))
                )
            )
        }, { result ->
            if (result == null) {
                notifyMissingSanitizer(project)
                return@inBackground
            }
            val console = GpuConsoleService.getInstance(project)
            console.print("compute-sanitizer: ${executable.path}", result.combined.ifBlank { "(no output)" }, !result.success)
            val summary = SanitizerOutputParser.parse(result.combined)
            val group = NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
            when {
                summary.errorCount == 0 -> group.createNotification(
                    "Memory check passed", "No CUDA memory errors detected in ${executable.name}", NotificationType.INFORMATION
                ).notify(project)
                summary.errorCount > 0 -> group.createNotification(
                    "Memory check found ${summary.errorCount} error(s)",
                    summary.findings.take(3).joinToString("\n").ifBlank { "See GPU Dev console" },
                    NotificationType.ERROR
                ).notify(project)
                else -> group.createNotification(
                    "Memory check finished without a summary",
                    "Tool output did not contain an ERROR SUMMARY — see GPU Dev console",
                    NotificationType.WARNING
                ).notify(project)
            }
        })
    }

    private fun notifyMissingSanitizer(project: Project) {
        NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
            .createNotification(
                "compute-sanitizer not found",
                "Install CUDA Toolkit or set the compute-sanitizer path in Settings | Tools | GPU Dev Accelerator.",
                NotificationType.WARNING
            ).notify(project)
    }
}
