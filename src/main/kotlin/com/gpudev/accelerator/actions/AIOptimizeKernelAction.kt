package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.premium.AIKernelOptimizer

class AIOptimizeKernelAction : AnAction("AI Optimize Kernel", "Get AI-powered optimization suggestions", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val kernelCode = document.text
        
        val optimizer = AIKernelOptimizer(project)
        val suggestions = optimizer.analyzeKernel(kernelCode)
        
        if (suggestions != null && suggestions.isNotEmpty()) {
            val message = buildString {
                append("AI Optimization Analysis Complete\n\n")
                append("Found ${suggestions.size} optimization opportunities:\n\n")
                
                suggestions.forEach { suggestion ->
                    append("${suggestion.impact} IMPACT: ${suggestion.title}\n")
                    append("└─ ${suggestion.description}\n")
                    append("└─ Estimated speedup: ${String.format("%.1f", suggestion.estimatedSpeedup)}x\n")
                    if (suggestion.codeChange != null) {
                        append("└─ Suggested fix: ${suggestion.codeChange}\n")
                    }
                    append("\n")
                }
            }
            
            Messages.showInfoMessage(project, message, "AI Kernel Optimization")
        }
    }
}
