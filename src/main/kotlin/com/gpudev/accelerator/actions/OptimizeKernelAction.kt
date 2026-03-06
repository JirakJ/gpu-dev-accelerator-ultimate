package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.optimization.AIOptimizer

class OptimizeKernelAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        val document = editor.document
        val code = document.text
        
        val suggestions = AIOptimizer.analyzeKernel(code)
        Messages.showInfoMessage(project, suggestions, "Optimization Suggestions")
    }
}
