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
import com.gpudev.accelerator.tools.ToolArtifact
import com.gpudev.accelerator.tools.ToolArtifactKind
import com.gpudev.accelerator.tools.ToolInvocation

/**
 * Profiles a CUDA executable with Nsight Compute (ncu) or legacy nvprof —
 * whichever is installed — and streams the report into the console.
 */
class ProfileKernelAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
            .withTitle("Select CUDA Executable to Profile")
        val executable = FileChooser.chooseFile(descriptor, project, null) ?: return

        GpuToolchain.inBackground(project, "Profiling ${executable.name}", { _ ->
            val profiler = GpuToolchain.ncu(project) ?: return@inBackground null
            val isNcu = profiler.endsWith("ncu")
            val command = if (isNcu) {
                listOf(profiler, "--set", "basic", "--print-summary", "per-kernel", executable.path)
            } else {
                listOf(profiler, "--print-gpu-summary", executable.path)
            }
            val invocation = ToolInvocation(
                command,
                artifacts = listOf(ToolArtifact(java.io.File(executable.path), ToolArtifactKind.INPUT))
            )
            command to GpuToolchain.execute(project, invocation)
        }, { result ->
            if (result == null) {
                notifyMissingProfiler(project)
                return@inBackground
            }
            val (command, toolResult) = result
            val isNcu = command.first().endsWith("ncu")
            GpuConsoleService.getInstance(project).print(
                "${if (isNcu) "Nsight Compute" else "nvprof"}: ${command.joinToString(" ")}",
                toolResult.combined.ifBlank { "(no output)" },
                !toolResult.success
            )
        })
    }

    private fun notifyMissingProfiler(project: Project) {
        NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
            .createNotification(
                "No GPU profiler found",
                "Install Nsight Compute (ncu) — it ships with the CUDA Toolkit — " +
                    "or set its path in Settings | Tools | GPU Dev Accelerator.",
                NotificationType.WARNING
            ).notify(project)
    }
}
