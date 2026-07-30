/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.gpu

import com.intellij.ui.JBColor
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import javax.swing.JPanel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders a real [GpuTopology] (from nvidia-smi) as a ring diagram with
 * link types on the edges and live utilization/memory on the nodes.
 */
class GpuTopologyPanel(private val topology: GpuTopology) : JPanel() {

    init {
        preferredSize = Dimension(760, 560)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val cx = width / 2
        val cy = height / 2
        val radius = (minOf(width, height) / 2 - 90).coerceAtLeast(80)
        val positions = topology.nodes.associate { node ->
            val angle = 2 * Math.PI * topology.nodes.indexOf(node) / topology.nodes.size
            node.index to Point(cx + (radius * cos(angle)).toInt(), cy + (radius * sin(angle)).toInt())
        }

        g2.stroke = BasicStroke(2f)
        g2.color = JBColor.GRAY
        for (link in topology.links) {
            val from = positions[link.from] ?: continue
            val to = positions[link.to] ?: continue
            g2.drawLine(from.x, from.y, to.x, to.y)
            g2.drawString(link.linkType, (from.x + to.x) / 2, (from.y + to.y) / 2 - 4)
        }

        for (node in topology.nodes) {
            val p = positions[node.index] ?: continue
            val boxW = 170
            val boxH = 64
            g2.color = JBColor.background().brighter()
            g2.fillRoundRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH, 12, 12)
            g2.color = if (node.utilizationPercent > 80) JBColor.RED else JBColor.foreground()
            g2.drawRoundRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH, 12, 12)
            g2.font = font.deriveFont(Font.BOLD)
            g2.drawString("GPU${node.index}  ${node.name.take(18)}", p.x - boxW / 2 + 8, p.y - boxH / 2 + 18)
            g2.font = font.deriveFont(Font.PLAIN)
            g2.drawString("util ${node.utilizationPercent}%  ${node.temperatureC}°C  ${node.powerDrawW.toInt()} W",
                p.x - boxW / 2 + 8, p.y - boxH / 2 + 36)
            g2.drawString("mem ${node.memoryUsedMiB}/${node.memoryTotalMiB} MiB",
                p.x - boxW / 2 + 8, p.y - boxH / 2 + 54)
        }
    }
}
