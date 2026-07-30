/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.gpudev.accelerator.analysis.CudaInspectionLogic

/**
 * Flags CUDA runtime calls whose cudaError_t result is discarded and offers
 * a quick fix wrapping the statement in a CUDA_CHECK macro.
 */
class CudaErrorCheckInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                for (finding in CudaInspectionLogic.uncheckedCalls(file.text)) {
                    holder.registerProblem(
                        file,
                        finding.message,
                        ProblemHighlightType.WEAK_WARNING,
                        TextRange(finding.startOffset, finding.endOffset),
                        WrapInCudaCheckFix(finding.startOffset)
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = "Unchecked CUDA API call"
    override fun getShortName(): String = "CudaErrorCheck"
}

private class WrapInCudaCheckFix(private val callOffset: Int) : LocalQuickFix {

    override fun getFamilyName(): String = "Wrap in CUDA_CHECK(...)"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val file = descriptor.psiElement as? PsiFile ?: descriptor.psiElement.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val text = document.text

        // Wrap the full statement: from the call start to the terminating semicolon.
        val semi = text.indexOf(';', callOffset)
        if (semi < 0) return
        val statement = text.substring(callOffset, semi)
        document.replaceString(callOffset, semi, "CUDA_CHECK(${statement.trim()})")

        if (!text.contains("#define CUDA_CHECK") && !text.contains("checkCudaErrors")) {
            val insertAt = lastIncludeEnd(text)
            document.insertString(insertAt, "\n" + CudaInspectionLogic.CHECK_MACRO)
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }

    private fun lastIncludeEnd(text: String): Int {
        var end = 0
        for (m in Regex("""(?m)^#include[^\n]*$""").findAll(text)) {
            end = m.range.last + 1
        }
        return end
    }
}
