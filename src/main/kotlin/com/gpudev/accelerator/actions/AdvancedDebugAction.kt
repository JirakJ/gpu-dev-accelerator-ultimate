package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.premium.AdvancedDebugger

class AdvancedDebugAction : AnAction("Advanced CUDA Debug", "Start advanced debugging with cuda-gdb", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val debugger = AdvancedDebugger(project)
        val session = debugger.startAdvancedDebugSession("/path/to/cuda/executable")
        
        if (session != null) {
            Messages.showInfoMessage(
                project,
                "Advanced debugging session started.\n\n" +
                "Features available:\n" +
                "• GPU thread inspection\n" +
                "• Memory watchpoints\n" +
                "• Warp-level stepping\n" +
                "• Device expression evaluation",
                "Advanced Debug Started"
            )
        }
    }
}
