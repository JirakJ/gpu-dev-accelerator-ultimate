/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.gpudev.accelerator.analysis.KernelStaticAnalyzer
import com.gpudev.accelerator.tools.GpuConsoleService

/**
 * Runs the deterministic kernel heuristics over the current editor and
 * prints findings with file:line references into the GPU Dev console.
 */
class AnalyzeKernelAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val findings = KernelStaticAnalyzer.analyze(editor.document.text)

        val body = if (findings.isEmpty()) {
            "No issues found by the static heuristics."
        } else {
            findings.joinToString("\n\n") { f ->
                val location = if (f.line >= 0 && file != null) "${file.path}:${f.line + 1}" else "(file-wide)"
                "[${f.impact}] ${f.title}\n  $location\n  ${f.detail}"
            }
        }
        GpuConsoleService.getInstance(project)
            .print("Static kernel analysis${file?.let { ": ${it.name}" } ?: ""}", body)
    }
}
