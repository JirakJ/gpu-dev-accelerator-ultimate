package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.gpudev.accelerator.premium.MultiGPUVisualizer
import javax.swing.JFrame
import javax.swing.WindowConstants

class VisualizeMultiGPUAction : AnAction("Visualize Multi-GPU Topology", "Show GPU topology and workload distribution", null) {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        val visualizer = MultiGPUVisualizer(project)
        val panel = visualizer.visualizeTopology()
        
        if (panel != null) {
            val frame = JFrame("Multi-GPU Topology Visualization")
            frame.add(panel)
            frame.setSize(900, 700)
            frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            frame.isVisible = true
        }
    }
}
