package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class MemoryCheckAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        Messages.showInfoMessage(
            project,
            "Memory Check tool available.\n\n" +
            "Tools:\n" +
            "✓ cuda-memcheck\n" +
            "✓ Leak detection\n" +
            "✓ Race conditions\n" +
            "✓ Invalid memory access",
            "Memory Check"
        )
    }
}
