/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.gpudev.accelerator.analysis.OccupancyCalculator
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

/**
 * Interactive theoretical-occupancy calculator — the numbers GPU developers
 * otherwise open NVIDIA's Excel sheet for.
 */
class OccupancyCalculatorAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        OccupancyDialog(project).show()
    }

    private class OccupancyDialog(project: Project) : DialogWrapper(project) {

        private val deviceCombo = ComboBox(OccupancyCalculator.PRESETS.map { it.label }.toTypedArray())
        private val threadsSpinner = JSpinner(SpinnerNumberModel(256, 32, 1024, 32))
        private val regsSpinner = JSpinner(SpinnerNumberModel(32, 0, 255, 1))
        private val smemSpinner = JSpinner(SpinnerNumberModel(0, 0, 228 * 1024, 1024))
        private val resultLabel = JBLabel()

        init {
            title = "GPU Occupancy Calculator"
            deviceCombo.selectedIndex = 3 // CC 8.6, the most common dev card
            listOf<JComponent>(deviceCombo, threadsSpinner, regsSpinner, smemSpinner).forEach { c ->
                when (c) {
                    is ComboBox<*> -> c.addActionListener { recalculate() }
                    is JSpinner -> c.addChangeListener { recalculate() }
                }
            }
            init()
            recalculate()
        }

        private fun recalculate() {
            val device = OccupancyCalculator.PRESETS[deviceCombo.selectedIndex]
            val result = OccupancyCalculator.calculate(
                device,
                threadsSpinner.value as Int,
                regsSpinner.value as Int,
                smemSpinner.value as Int
            )
            resultLabel.text = "<html><b>Occupancy: ${"%.1f".format(result.occupancy * 100)}%</b><br>" +
                "${result.activeWarpsPerSm}/${result.maxWarpsPerSm} warps per SM, " +
                "${result.activeBlocksPerSm} active block(s)<br>" +
                "Limited by: ${result.limitedBy}</html>"
        }

        override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
            .addLabeledComponent("Device:", deviceCombo)
            .addLabeledComponent("Threads per block:", threadsSpinner)
            .addLabeledComponent("Registers per thread (0 = ignore):", regsSpinner)
            .addLabeledComponent("Shared memory per block (bytes):", smemSpinner)
            .addSeparator()
            .addComponent(resultLabel)
            .panel

        override fun createActions() = arrayOf(okAction)
    }
}
