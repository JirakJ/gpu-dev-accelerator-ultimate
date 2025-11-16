package com.gpudev.accelerator.cuda

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class CudaCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            CudaCompletionProvider()
        )
    }
}

class CudaCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val keywords = listOf(
            "__global__", "__device__", "__host__", "__shared__", "__constant__",
            "threadIdx", "blockIdx", "blockDim", "gridDim", "warpSize",
            "cudaMalloc", "cudaFree", "cudaMemcpy", "cudaMemcpyAsync",
            "cudaDeviceSynchronize", "cudaStreamCreate", "cudaStreamDestroy",
            "cudaGetDeviceProperties", "cudaGetLastError", "cudaGetErrorString",
            "__syncthreads", "__syncwarp", "__threadfence", "__threadfence_block"
        )
        
        keywords.forEach { keyword ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText("CUDA")
                    .withBoldness(true)
            )
        }
    }
}
