package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.premium.AdvancedMemoryAnalyzer

class AdvancedMemoryCheckAction : AnAction("Advanced Memory Analysis", "Detect memory leaks and race conditions", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val analyzer = AdvancedMemoryAnalyzer(project)
        val report = analyzer.analyzeMemory("/path/to/executable")
        
        if (report != null) {
            val message = buildString {
                append("Memory Analysis Report\n\n")
                append("Total Allocations: ${report.allocations.size}\n")
                append("Device Memory: ${String.format("%.2f", report.usage.deviceGB)} GB\n")
                append("Host Memory: ${String.format("%.2f", report.usage.hostGB)} GB\n\n")
                
                if (report.leaks.isNotEmpty()) {
                    append("⚠️ Memory Leaks Detected: ${report.leaks.size}\n\n")
                    report.leaks.forEach { leak ->
                        append("${leak.severity}: ${leak.recommendation}\n")
                        append("  Address: ${leak.allocation.address}\n")
                        append("  Size: ${leak.allocation.size} bytes\n")
                        append("  Location: ${leak.allocation.stackTrace}\n\n")
                    }
                } else {
                    append("✓ No memory leaks detected\n")
                }
            }
            
            Messages.showInfoMessage(project, message, "Advanced Memory Analysis")
        }
    }
}
