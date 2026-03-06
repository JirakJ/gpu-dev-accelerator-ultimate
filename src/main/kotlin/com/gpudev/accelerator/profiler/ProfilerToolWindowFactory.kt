package com.gpudev.accelerator.profiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import java.awt.BorderLayout

class ProfilerToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val profilerPanel = ProfilerPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(profilerPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class ProfilerPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val textArea = JTextArea()
    
    init {
        textArea.isEditable = false
        add(JScrollPane(textArea), BorderLayout.CENTER)
        
        textArea.text = """
            GPU Profiler Active
            
            Ready to profile CUDA and Metal kernels.
            
            Available Tools:
            ✓ nvprof integration
            ✓ Nsight Compute
            ✓ Metal Performance Shaders
            ✓ Real-time metrics
            
            Run a GPU kernel to start profiling.
        """.trimIndent()
    }
}
