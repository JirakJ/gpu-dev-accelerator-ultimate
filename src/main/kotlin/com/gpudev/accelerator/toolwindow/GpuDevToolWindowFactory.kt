/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class GpuDevToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(12)
        panel.add(JBLabel(
            "<html><b>GPU Dev Accelerator</b><br><br>" +
                "Output from GPU tools appears here:<br>" +
                "&bull; Compile CUDA (Ctrl+Alt+C)<br>" +
                "&bull; Show PTX<br>" +
                "&bull; GPU Information<br>" +
                "&bull; Memory Check (compute-sanitizer)<br>" +
                "&bull; Kernel Profiling (Nsight Compute)<br>" +
                "&bull; Static Kernel Analysis (Ctrl+Alt+O)</html>"
        ), BorderLayout.NORTH)
        val content = ContentFactory.getInstance().createContent(panel, "Overview", false)
        toolWindow.contentManager.addContent(content)
    }
}
