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

/**
 * Profiles a CUDA executable with Nsight Compute (ncu) or legacy nvprof —
 * whichever is installed — and streams the report into the console.
 */
class ProfileKernelAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val profiler = GpuToolchain.ncu()
        if (profiler == null) {
            NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
                .createNotification(
                    "No GPU profiler found",
                    "Install Nsight Compute (ncu) — it ships with the CUDA Toolkit — " +
                        "or set its path in Settings | Tools | GPU Dev Accelerator.",
                    NotificationType.WARNING
                ).notify(project)
            return
        }

        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Select CUDA Executable to Profile")
        val executable = FileChooser.chooseFile(descriptor, project, null) ?: return

        val isNcu = profiler.endsWith("ncu")
        GpuToolchain.inBackground(project, "Profiling ${executable.name}", { _ ->
            val command = if (isNcu) {
                listOf(profiler, "--set", "basic", "--print-summary", "per-kernel", executable.path)
            } else {
                listOf(profiler, "--print-gpu-summary", executable.path)
            }
            command to GpuToolchain.run(command)
        }, { (command, result) ->
            GpuConsoleService.getInstance(project).print(
                "${if (isNcu) "Nsight Compute" else "nvprof"}: ${command.joinToString(" ")}",
                result.combined.ifBlank { "(no output)" },
                !result.success
            )
        })
    }
}
