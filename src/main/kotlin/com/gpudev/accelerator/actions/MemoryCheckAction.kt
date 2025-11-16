package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.license.LicenseManager

class MemoryCheckAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val licenseManager = LicenseManager.getInstance()
        
        if (!licenseManager.isFeatureEnabled(LicenseManager.Feature.MEMORY_ANALYSIS)) {
            Messages.showInfoMessage(
                project,
                "Memory analysis requires Individual or Enterprise license.\n\n" +
                "Features:\n" +
                "- cuda-memcheck integration\n" +
                "- Memory leak detection\n" +
                "- Race condition detection\n" +
                "- Allocation tracking",
                "Premium Feature"
            )
            return
        }
        
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
