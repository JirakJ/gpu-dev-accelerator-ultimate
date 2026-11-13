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
import com.gpudev.accelerator.ptx.AssemblyComparisonData
import com.gpudev.accelerator.ptx.AssemblyComparisonRequest
import com.gpudev.accelerator.ptx.AssemblyOutputKind
import com.gpudev.accelerator.ptx.AssemblyOutputLoadResult
import com.gpudev.accelerator.ptx.AssemblyOutputLoader
import com.gpudev.accelerator.ptx.AssemblyPaneData
import com.gpudev.accelerator.ptx.PtxFileType
import com.gpudev.accelerator.ptx.PtxGenerationService
import com.gpudev.accelerator.ptx.SassFileType
import com.gpudev.accelerator.ptx.SassGenerationService
import com.gpudev.accelerator.tools.GpuConsoleService
import com.gpudev.accelerator.tools.GpuToolchain

class ShowAssemblyAction : AnAction() {

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
        val sourcePath = file.path
        val sourceName = file.name
        GpuConsoleService.getInstance(project)
            .print("Generating SASS for $sourceName", "Starting nvcc -cubin and nvdisasm… (see Progress for status)")

        GpuToolchain.inBackground(project, "Generating SASS for $sourceName", { _ ->
            SassGenerationService.generate(project, sourcePath)
        }, { sass ->
            if (sass.success && sass.sassText != null) {
                showComparison(
                    project,
                    sourcePath,
                    AssemblyComparisonData(
                        sourceName,
                        sourceText,
                        AssemblyPaneData(
                            AssemblyOutputKind.SASS,
                            sass.sassText,
                            SassFileType.INSTANCE,
                            sass.references,
                            "SASS · ${sass.architecture}"
                        )
                    )
                )
            } else {
                val message = sass.result.combined.ifBlank { "Could not produce SASS with the active toolkit" }
                GpuConsoleService.getInstance(project).print("SASS unavailable", message, error = true)
                showPtxFallback(project, sourcePath, sourceName, sourceText, message)
            }
        })
    }

    private fun showPtxFallback(
        project: com.intellij.openapi.project.Project,
        sourcePath: String,
        sourceName: String,
        sourceText: String,
        sassFailure: String
    ) {
        GpuToolchain.inBackground(project, "Generating PTX for $sourceName", { _ ->
            PtxGenerationService.generate(project, sourcePath)
        }, { ptx ->
            if (ptx?.ptxText == null || !ptx.result.success) {
                val message = ptx?.result?.combined?.ifBlank { "nvcc -ptx failed" } ?: "nvcc was not found"
                GpuConsoleService.getInstance(project).print("PTX fallback failed", message, error = true)
                return@inBackground
            }
            val pane = AssemblyPaneData(
                AssemblyOutputKind.PTX,
                ptx.ptxText,
                PtxFileType.INSTANCE,
                PtxGenerationService.sourceMappings(project, sourcePath, ptx.ptxText)
            )
            showComparison(
                project,
                sourcePath,
                AssemblyComparisonData(sourceName, sourceText, pane, "SASS unavailable: $sassFailure")
            )
        })
    }

    private fun showComparison(
        project: com.intellij.openapi.project.Project,
        sourcePath: String,
        comparison: AssemblyComparisonData
    ) {
        AssemblyComparisonRequest.show(project, comparison, outputLoader(project, sourcePath))
    }

    private fun outputLoader(project: com.intellij.openapi.project.Project, sourcePath: String) =
        AssemblyOutputLoader { kind, onComplete ->
            when (kind) {
                AssemblyOutputKind.PTX -> GpuToolchain.inBackground(
                    project,
                    "Generating PTX for ${sourcePath.substringAfterLast('/')}",
                    { _ -> PtxGenerationService.generate(project, sourcePath) },
                    { ptx ->
                        if (ptx?.ptxText != null && ptx.result.success) {
                            onComplete(
                                AssemblyOutputLoadResult.Success(
                                    AssemblyPaneData(
                                        AssemblyOutputKind.PTX,
                                        ptx.ptxText,
                                        PtxFileType.INSTANCE,
                                        PtxGenerationService.sourceMappings(project, sourcePath, ptx.ptxText)
                                    )
                                )
                            )
                        } else {
                            onComplete(
                                AssemblyOutputLoadResult.Failure(
                                    ptx?.result?.combined?.ifBlank { "nvcc -ptx failed" } ?: "nvcc was not found"
                                )
                            )
                        }
                    }
                )
                AssemblyOutputKind.SASS -> GpuToolchain.inBackground(
                    project,
                    "Generating SASS for ${sourcePath.substringAfterLast('/')}",
                    { _ -> SassGenerationService.generate(project, sourcePath) },
                    { sass ->
                        if (sass.success && sass.sassText != null) {
                            onComplete(
                                AssemblyOutputLoadResult.Success(
                                    AssemblyPaneData(
                                        AssemblyOutputKind.SASS,
                                        sass.sassText,
                                        SassFileType.INSTANCE,
                                        sass.references,
                                        "SASS · ${sass.architecture}"
                                    )
                                )
                            )
                        } else {
                            onComplete(
                                AssemblyOutputLoadResult.Failure(
                                    sass.result.combined.ifBlank { "Could not produce SASS with the active toolkit" }
                                )
                            )
                        }
                    }
                )
            }
        }
}
