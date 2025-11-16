package com.gpudev.accelerator.profiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.gpudev.accelerator.license.LicenseManager
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
    private val licenseManager = LicenseManager.getInstance()
    
    init {
        textArea.isEditable = false
        add(JScrollPane(textArea), BorderLayout.CENTER)
        
        if (!licenseManager.isFeatureEnabled(LicenseManager.Feature.ADVANCED_PROFILING)) {
            textArea.text = """
                GPU Profiler - Premium Feature
                
                Advanced profiling requires Individual or Enterprise license.
                
                Features:
                - Real-time kernel performance monitoring
                - Memory bandwidth analysis
                - Occupancy visualization
                - Warp execution analysis
                - Nsight Compute integration
                - Flame graphs
                
                Upgrade to unlock advanced profiling capabilities.
            """.trimIndent()
        } else {
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
}
