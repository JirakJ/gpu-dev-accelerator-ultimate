/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
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
    private val metalSdkField = JBTextField()
    private val timeoutSpinner = JSpinner(SpinnerNumberModel(300, 10, 3600, 10))

    override fun getDisplayName(): String = "GPU Dev Accelerator"

    override fun createComponent(): JComponent {
        val descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        nvccField.addBrowseFolderListener("Select nvcc Executable", null, null, descriptor)
        sanitizerField.addBrowseFolderListener("Select compute-sanitizer Executable", null, null, descriptor)
        ncuField.addBrowseFolderListener("Select Nsight Compute (ncu) Executable", null, null, descriptor)

        archField.toolTipText = "e.g. sm_86 — passed as -arch=<value>; empty = nvcc default"
        flagsField.toolTipText = "Extra flags appended to every nvcc invocation, e.g. -O3 --use_fast_math"
        metalSdkField.toolTipText = "SDK for xcrun metal compilation: macosx or iphoneos"

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("nvcc path (empty = PATH):", nvccField)
            .addLabeledComponent("GPU architecture (-arch):", archField)
            .addLabeledComponent("Extra nvcc flags:", flagsField)
            .addLabeledComponent("compute-sanitizer path:", sanitizerField)
            .addLabeledComponent("Nsight Compute (ncu) path:", ncuField)
            .addLabeledComponent("Metal SDK (xcrun -sdk):", metalSdkField)
            .addLabeledComponent("Tool timeout (seconds):", timeoutSpinner)
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
            metalSdkField.text != s.metalSdk ||
            (timeoutSpinner.value as Int) != s.toolTimeoutSeconds
    }

    override fun apply() {
        val s = GpuDevSettings.getInstance().state
        s.nvccPath = nvccField.text.trim()
        s.gpuArch = archField.text.trim()
        s.extraNvccFlags = flagsField.text.trim()
        s.sanitizerPath = sanitizerField.text.trim()
        s.ncuPath = ncuField.text.trim()
        s.metalSdk = metalSdkField.text.trim().ifBlank { "macosx" }
        s.toolTimeoutSeconds = timeoutSpinner.value as Int
    }

    override fun reset() {
        val s = GpuDevSettings.getInstance().state
        nvccField.text = s.nvccPath
        archField.text = s.gpuArch
        flagsField.text = s.extraNvccFlags
        sanitizerField.text = s.sanitizerPath
        ncuField.text = s.ncuPath
        metalSdkField.text = s.metalSdk
        timeoutSpinner.value = s.toolTimeoutSeconds
    }

    override fun disposeUIResources() {
        panel = null
    }
}
