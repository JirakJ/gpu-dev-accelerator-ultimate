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
import com.gpudev.accelerator.analysis.CudaInspectionLogic

class CudaMemoryLeakInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                for (finding in CudaInspectionLogic.memoryLeaks(file.text)) {
                    holder.registerProblem(
                        file,
                        finding.message,
                        ProblemHighlightType.WARNING,
                        TextRange(finding.startOffset, finding.endOffset)
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = "CUDA memory leak detection"
    override fun getShortName(): String = "CudaMemoryLeak"
}
