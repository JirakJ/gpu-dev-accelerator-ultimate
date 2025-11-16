package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.license.LicenseManager
import com.gpudev.accelerator.optimization.AIOptimizer

class OptimizeKernelAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val licenseManager = LicenseManager.getInstance()
        
        if (!licenseManager.isFeatureEnabled(LicenseManager.Feature.AI_OPTIMIZATION)) {
            Messages.showInfoMessage(
                project,
                "AI-powered kernel optimization requires Individual or Enterprise license.\n\n" +
                "Get intelligent suggestions for:\n" +
                "- Memory coalescing\n" +
                "- Shared memory usage\n" +
                "- Thread block optimization\n" +
                "- Occupancy improvements",
                "Premium Feature"
            )
            return
        }
        
        val document = editor.document
        val code = document.text
        
        val suggestions = AIOptimizer.analyzeKernel(code)
        Messages.showInfoMessage(project, suggestions, "Optimization Suggestions")
    }
}
