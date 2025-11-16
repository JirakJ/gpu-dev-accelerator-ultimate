package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.InputStreamReader

class ShowGPUInfoAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gpuInfo = detectGPUs()
        Messages.showInfoMessage(project, gpuInfo, "GPU Devices")
    }
    
    private fun detectGPUs(): String {
        val info = StringBuilder()
        
        // Try NVIDIA
        try {
            val process = ProcessBuilder("nvidia-smi", "--query-gpu=name,memory.total", "--format=csv,noheader").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            if (output.isNotBlank()) {
                info.append("NVIDIA GPUs:\n$output\n")
            }
        } catch (e: Exception) {
            info.append("NVIDIA GPUs: Not detected\n")
        }
        
        // Try Metal (macOS)
        if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
            info.append("\nMetal: Available\n")
            info.append("Use Metal shader debugging and profiling\n")
        }
        
        if (info.isEmpty()) {
            return "No GPU devices detected.\n\nPlease install:\n- CUDA Toolkit for NVIDIA GPUs\n- Xcode for Metal support (macOS)"
        }
        
        return info.toString()
    }
}
