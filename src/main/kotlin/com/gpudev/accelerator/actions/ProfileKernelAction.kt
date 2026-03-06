package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ProfileKernelAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        Messages.showInfoMessage(
            project,
            "GPU Profiler activated.\n\n" +
            "Profiling tools available:\n" +
            "- nvprof\n" +
            "- Nsight Compute\n" +
            "- Metal Performance Shaders\n" +
            "- Custom metrics",
            "GPU Profiler"
        )
    }
}
