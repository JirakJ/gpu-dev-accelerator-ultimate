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
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain
import com.gpudev.accelerator.tools.SanitizerOutputParser

/**
 * Runs compute-sanitizer (fallback: cuda-memcheck) against a CUDA executable
 * picked by the user and reports leaks/invalid accesses in the console.
 */
class MemoryCheckAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sanitizer = GpuToolchain.sanitizer()
        if (sanitizer == null) {
            NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
                .createNotification(
                    "compute-sanitizer not found",
                    "Install the CUDA Toolkit (compute-sanitizer ships with it) or set its path in Settings | Tools | GPU Dev Accelerator.",
                    NotificationType.WARNING
                ).notify(project)
            return
        }

        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Select CUDA Executable to Check")
        val executable = FileChooser.chooseFile(descriptor, project, null) ?: return

        GpuToolchain.inBackground(project, "Running compute-sanitizer on ${executable.name}", { _ ->
            GpuToolchain.run(listOf(sanitizer, "--tool", "memcheck", executable.path))
        }, { result ->
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
}
