/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.gpudev.accelerator.analysis.KernelStaticAnalyzer

/**
 * Surfaces [KernelStaticAnalyzer] performance/correctness heuristics as
 * editor annotations on the offending line.
 */
class CudaKernelOptimizationInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                val text = file.text
                val lineOffsets = buildLineOffsets(text)
                for (finding in KernelStaticAnalyzer.analyze(text)) {
                    if (finding.line !in lineOffsets.indices) continue
                    val start = lineOffsets[finding.line]
                    val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
                    if (start >= end) continue
                    val highlight = when (finding.impact) {
                        KernelStaticAnalyzer.Impact.HIGH -> ProblemHighlightType.WARNING
                        else -> ProblemHighlightType.WEAK_WARNING
                    }
                    holder.registerProblem(file, "${finding.title}: ${finding.detail}", highlight, TextRange(start, end))
                }
            }
        }
    }

    private fun buildLineOffsets(text: String): List<Int> {
        val offsets = mutableListOf(0)
        text.forEachIndexed { i, c -> if (c == '\n') offsets.add(i + 1) }
        return offsets
    }

    override fun getDisplayName(): String = "CUDA kernel performance heuristics"
    override fun getShortName(): String = "CudaKernelOptimization"
}
