/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class GPUDevSettingsConfigurable : Configurable {

    private var panel: JPanel? = null
    private val nvccField = TextFieldWithBrowseButton()
    private val sanitizerField = TextFieldWithBrowseButton()
    private val ncuField = TextFieldWithBrowseButton()
    private val archField = JBTextField()
    private val flagsField = JBTextField()
    private val timeoutSpinner = JSpinner(SpinnerNumberModel(300, 10, 3600, 10))
    private val transferRemoteFilesCheckBox = JBCheckBox("Enable plugin-managed remote file transfers")

    override fun getDisplayName(): String = "GPU Dev Accelerator"

    override fun createComponent(): JComponent {
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        nvccField.addBrowseFolderListener("Select nvcc Executable", null, null, descriptor)
        sanitizerField.addBrowseFolderListener("Select compute-sanitizer Executable", null, null, descriptor)
        ncuField.addBrowseFolderListener("Select Nsight Compute (ncu) Executable", null, null, descriptor)

        nvccField.toolTipText = "Local executable path, or path on CLion's default SSH toolchain; empty = PATH"
        sanitizerField.toolTipText = "Local executable path, or path on CLion's default SSH toolchain"
        ncuField.toolTipText = "Local executable path, or path on CLion's default SSH toolchain"
        archField.toolTipText = "e.g. sm_86 — passed as -arch=<value>; empty = nvcc default"
        flagsField.toolTipText = "Extra flags appended to every nvcc invocation, e.g. -O3 --use_fast_math"
        transferRemoteFilesCheckBox.toolTipText =
            "Off by default for shared mounts or CLion-synchronized projects. " +
                "Enable only when plugin-managed source uploads and output downloads are needed."

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("nvcc path (empty = PATH):", nvccField)
            .addLabeledComponent("GPU architecture (-arch):", archField)
            .addLabeledComponent("Extra nvcc flags:", flagsField)
            .addLabeledComponent("compute-sanitizer path:", sanitizerField)
            .addLabeledComponent("Nsight Compute (ncu) path:", ncuField)
            .addLabeledComponent("Tool timeout (seconds):", timeoutSpinner)
            .addComponent(transferRemoteFilesCheckBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val s = GpuDevSettings.getInstance().state
        return nvccField.text != s.nvccPath ||
            archField.text != s.gpuArch ||
            flagsField.text != s.extraNvccFlags ||
            sanitizerField.text != s.sanitizerPath ||
            ncuField.text != s.ncuPath ||
            (timeoutSpinner.value as Int) != s.toolTimeoutSeconds ||
            transferRemoteFilesCheckBox.isSelected != s.transferRemoteFiles
    }

    override fun apply() {
        val s = GpuDevSettings.getInstance().state
        s.nvccPath = nvccField.text.trim()
        s.gpuArch = archField.text.trim()
        s.extraNvccFlags = flagsField.text.trim()
        s.sanitizerPath = sanitizerField.text.trim()
        s.ncuPath = ncuField.text.trim()
        s.toolTimeoutSeconds = timeoutSpinner.value as Int
        s.transferRemoteFiles = transferRemoteFilesCheckBox.isSelected
    }

    override fun reset() {
        val s = GpuDevSettings.getInstance().state
        nvccField.text = s.nvccPath
        archField.text = s.gpuArch
        flagsField.text = s.extraNvccFlags
        sanitizerField.text = s.sanitizerPath
        ncuField.text = s.ncuPath
        timeoutSpinner.value = s.toolTimeoutSeconds
        transferRemoteFilesCheckBox.isSelected = s.transferRemoteFiles
    }

    override fun disposeUIResources() {
        panel = null
    }
}
