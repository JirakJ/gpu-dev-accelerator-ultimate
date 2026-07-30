/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.gpudev.accelerator.gpu.GpuTopology
import com.gpudev.accelerator.gpu.GpuTopologyPanel
import com.gpudev.accelerator.gpu.GpuTopologyParser
import com.gpudev.accelerator.tools.GpuToolchain
import javax.swing.JComponent

/**
 * Shows the actual multi-GPU topology of this machine, read from
 * `nvidia-smi topo -m` and the device query — no simulated data.
 */
class VisualizeMultiGPUAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val smi = GpuToolchain.nvidiaSmi()
        if (smi == null) {
            notifyNoGpus(project)
            return
        }
        GpuToolchain.inBackground(project, "Reading GPU topology", { _ ->
            val devices = GpuToolchain.run(
                listOf(smi, "--query-gpu=index,name,memory.total,memory.used,utilization.gpu,temperature.gpu,power.draw",
                    "--format=csv,noheader,nounits")
            )
            val matrix = GpuToolchain.run(listOf(smi, "topo", "-m"))
            GpuTopology(
                nodes = GpuTopologyParser.parseDeviceQuery(devices.stdout),
                links = GpuTopologyParser.parseTopologyMatrix(matrix.stdout)
            )
        }, { topology ->
            if (topology.nodes.isEmpty()) {
                notifyNoGpus(project)
            } else {
                TopologyDialog(project, topology).show()
            }
        })
    }

    private fun notifyNoGpus(project: Project) {
        NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
            .createNotification(
                "No NVIDIA GPUs detected",
                "Multi-GPU topology requires nvidia-smi. On this machine none was found.",
                NotificationType.INFORMATION
            ).notify(project)
    }

    private class TopologyDialog(project: Project, private val topology: GpuTopology) : DialogWrapper(project) {
        init {
            title = "Multi-GPU Topology (${topology.nodes.size} device(s))"
            init()
        }

        override fun createCenterPanel(): JComponent = GpuTopologyPanel(topology)
    }
}
