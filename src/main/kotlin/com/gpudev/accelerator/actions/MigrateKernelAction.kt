/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ui.FormBuilder
import com.gpudev.accelerator.cuda.CudaFileType
import com.gpudev.accelerator.metal.MetalFileType
import com.gpudev.accelerator.opencl.OpenCLFileType
import com.gpudev.accelerator.premium.CrossPlatformMigrator
import com.gpudev.accelerator.tools.GpuConsoleService
import javax.swing.JComponent

/**
 * Regex-based source port of the current kernel to another GPU platform.
 * Produces a starting point, not a drop-in replacement — warnings land in the console.
 */
class MigrateKernelAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = sourcePlatform(e) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val source = sourcePlatform(e) ?: return

        val targets = CrossPlatformMigrator.GPUPlatform.values().filter { it != source }
        val dialog = TargetDialog(project, source, targets)
        if (!dialog.showAndGet()) return
        val target = dialog.selectedTarget

        val result = CrossPlatformMigrator(project).migrateKernel(psiFile, source, target)
        if (result.warnings.isNotEmpty() || result.performanceNotes.isNotEmpty()) {
            GpuConsoleService.getInstance(project).print(
                "Migration $source → $target: ${psiFile.name}",
                (result.warnings.map { "warning: $it" } + result.performanceNotes.map { "note: $it" })
                    .joinToString("\n")
            )
        }
        val extension = when (target) {
            CrossPlatformMigrator.GPUPlatform.METAL -> "metal"
            CrossPlatformMigrator.GPUPlatform.OPENCL -> "cl"
            CrossPlatformMigrator.GPUPlatform.CUDA -> "cu"
            else -> "txt"
        }
        val virtualFile = LightVirtualFile(
            "${psiFile.virtualFile?.nameWithoutExtension ?: "kernel"}_${target.name.lowercase()}.$extension",
            PlainTextFileType.INSTANCE,
            result.generatedCode
        )
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    private fun sourcePlatform(e: AnActionEvent): CrossPlatformMigrator.GPUPlatform? =
        when (e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType) {
            CudaFileType.INSTANCE -> CrossPlatformMigrator.GPUPlatform.CUDA
            MetalFileType.INSTANCE -> CrossPlatformMigrator.GPUPlatform.METAL
            OpenCLFileType.INSTANCE -> CrossPlatformMigrator.GPUPlatform.OPENCL
            else -> null
        }

    private class TargetDialog(
        project: Project,
        source: CrossPlatformMigrator.GPUPlatform,
        private val targets: List<CrossPlatformMigrator.GPUPlatform>
    ) : DialogWrapper(project) {

        private val combo = ComboBox(targets.map { it.name }.toTypedArray())

        val selectedTarget: CrossPlatformMigrator.GPUPlatform get() = targets[combo.selectedIndex]

        init {
            title = "Migrate $source Kernel"
            init()
        }

        override fun createCenterPanel(): JComponent =
            FormBuilder.createFormBuilder().addLabeledComponent("Target platform:", combo).panel
    }
}
