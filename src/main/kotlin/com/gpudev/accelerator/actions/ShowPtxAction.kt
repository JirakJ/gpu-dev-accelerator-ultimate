/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.gpudev.accelerator.cuda.CudaFileType
import com.gpudev.accelerator.ptx.PtxComparisonRequest
import com.gpudev.accelerator.ptx.PtxGenerationService
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain

/**
 * Compiles the current .cu file to PTX and opens the result in the editor —
 * the fastest way to see what the compiler actually generated.
 */
class ShowPtxAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.VIRTUAL_FILE)?.fileType == CudaFileType.INSTANCE
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (file.fileType != CudaFileType.INSTANCE) return
        FileDocumentManager.getInstance().saveAllDocuments()
        val sourceText = FileDocumentManager.getInstance().getDocument(file)?.text ?: return

        val filePath = file.path
        val fileName = file.name
        GpuConsoleService.getInstance(project)
            .print("Generating PTX for $fileName", "Starting nvcc -ptx… (see Progress for status)")
        GpuToolchain.inBackground(project, "Generating PTX for $fileName", { _ ->
            PtxGenerationService.generate(project, filePath)
        }, { generation ->
            if (generation == null) {
                CompileCudaAction.notifyMissingNvcc(project)
                return@inBackground
            }
            val ptx = generation.ptxText
            if (ptx != null) {
                val mappings = PtxGenerationService.sourceMappings(project, filePath, ptx)
                PtxComparisonRequest.show(project, fileName, sourceText, ptx, mappings)
            } else {
                GpuConsoleService.getInstance(project).print(
                    "nvcc -ptx: ${generation.command.joinToString(" ")}",
                    generation.result.combined,
                    error = true
                )
            }
        })
    }

}
