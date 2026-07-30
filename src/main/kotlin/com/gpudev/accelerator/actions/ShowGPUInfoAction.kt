/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.SystemInfo
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain

/**
 * Prints real GPU/toolchain inventory into the GPU Dev console:
 * NVIDIA devices via nvidia-smi, Apple GPUs via system_profiler, nvcc version.
 */
class ShowGPUInfoAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        GpuToolchain.inBackground(project, "Detecting GPU devices", { _ ->
            buildString {
                val smi = GpuToolchain.nvidiaSmi()
                if (smi != null) {
                    val result = GpuToolchain.run(
                        listOf(
                            smi,
                            "--query-gpu=index,name,driver_version,memory.total,memory.used,utilization.gpu,temperature.gpu,power.draw",
                            "--format=csv"
                        )
                    )
                    append("NVIDIA GPUs (nvidia-smi):\n")
                    append(if (result.success) result.stdout else result.combined)
                    append('\n')
                } else {
                    append("NVIDIA: nvidia-smi not found\n")
                }

                if (SystemInfo.isMac) {
                    val result = GpuToolchain.run(listOf("system_profiler", "SPDisplaysDataType"))
                    if (result.success) {
                        append("\nApple GPUs (system_profiler):\n")
                        append(result.stdout.trim())
                        append('\n')
                    }
                }

                val nvcc = GpuToolchain.nvcc()
                if (nvcc != null) {
                    val version = GpuToolchain.run(listOf(nvcc, "--version"))
                    append("\nnvcc: $nvcc\n")
                    append(version.stdout.lines().lastOrNull { it.isNotBlank() } ?: "")
                    append('\n')
                } else {
                    append("\nnvcc: not found (set path in Settings | Tools | GPU Dev Accelerator)\n")
                }
            }
        }, { info ->
            GpuConsoleService.getInstance(project).print("GPU information", info)
        })
    }
}
