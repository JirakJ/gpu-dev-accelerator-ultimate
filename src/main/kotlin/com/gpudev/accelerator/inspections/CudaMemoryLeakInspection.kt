package com.gpudev.accelerator.inspections

import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

class CudaMemoryLeakInspection : LocalInspectionTool() {
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                val text = file.text
                val mallocCount = Regex("cudaMalloc").findAll(text).count()
                val freeCount = Regex("cudaFree").findAll(text).count()
                
                if (mallocCount > freeCount) {
                    // Could register a problem here in a real implementation
                }
            }
        }
    }
    
    override fun getDisplayName(): String = "CUDA Memory Leak Detection"
    
    override fun getShortName(): String = "CudaMemoryLeak"
}
