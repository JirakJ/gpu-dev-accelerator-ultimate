/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.analysis

/**
 * Pure text analysis backing the CUDA inspections. Kept free of IntelliJ
 * types so it unit-tests without a platform fixture.
 */
object CudaInspectionLogic {

    data class TextFinding(val startOffset: Int, val endOffset: Int, val message: String)

    private data class AllocPair(val allocPattern: Regex, val freeName: String)

    private val allocPairs = listOf(
        AllocPair(Regex("""cudaMalloc\s*\(\s*(?:\(void\s*\*\*\)\s*)?&\s*([A-Za-z_]\w*)"""), "cudaFree"),
        AllocPair(Regex("""cudaMallocManaged\s*\(\s*(?:\(void\s*\*\*\)\s*)?&\s*([A-Za-z_]\w*)"""), "cudaFree"),
        AllocPair(Regex("""cudaMallocHost\s*\(\s*(?:\(void\s*\*\*\)\s*)?&\s*([A-Za-z_]\w*)"""), "cudaFreeHost")
    )

    /** Allocations whose pointer variable is never passed to the matching free call. */
    fun memoryLeaks(text: String): List<TextFinding> {
        val findings = mutableListOf<TextFinding>()
        for (pair in allocPairs) {
            for (m in pair.allocPattern.findAll(text)) {
                val variable = m.groupValues[1]
                val freed = Regex("""${pair.freeName}\s*\(\s*(?:\(void\s*\*\)\s*)?${Regex.escape(variable)}\s*\)""")
                    .containsMatchIn(text)
                if (!freed) {
                    findings.add(
                        TextFinding(
                            m.range.first, m.range.last + 1,
                            "GPU memory allocated into '$variable' is never released with ${pair.freeName}()"
                        )
                    )
                }
            }
        }
        return findings
    }

    private val checkedApis = listOf(
        "cudaMalloc", "cudaMallocManaged", "cudaMallocHost", "cudaMemcpy", "cudaMemcpyAsync",
        "cudaMemset", "cudaFree", "cudaFreeHost", "cudaDeviceSynchronize", "cudaStreamSynchronize",
        "cudaEventRecord", "cudaLaunchKernel", "cudaGetDeviceProperties", "cudaSetDevice"
    )
    private val callPattern = Regex("""^\s*((?:${checkedApis.joinToString("|")})\s*\()""", RegexOption.MULTILINE)

    /**
     * CUDA runtime calls whose cudaError_t result is silently discarded —
     * a statement that starts with the call, not wrapped in a check macro or assignment.
     */
    fun uncheckedCalls(text: String): List<TextFinding> {
        val findings = mutableListOf<TextFinding>()
        for (m in callPattern.findAll(text)) {
            val callStart = m.range.first + (m.value.length - m.groupValues[1].length)
            val lineStart = text.lastIndexOf('\n', m.range.first).let { if (it < 0) 0 else it + 1 }
            val prevLineStart = text.lastIndexOf('\n', (lineStart - 2).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
            val context = text.substring(prevLineStart, callStart)
            if (context.contains("CUDA_CHECK") || context.contains("checkCudaErrors") ||
                context.contains("=") || context.contains("return") || context.contains("if")
            ) continue
            val callName = m.groupValues[1].removeSuffix("(").trim()
            findings.add(
                TextFinding(
                    callStart, callStart + callName.length,
                    "Return value of $callName() is ignored — CUDA errors are sticky and will surface later at an unrelated call"
                )
            )
        }
        return findings
    }

    const val CHECK_MACRO = """#define CUDA_CHECK(call)                                                    \
    do {                                                                    \
        cudaError_t err__ = (call);                                         \
        if (err__ != cudaSuccess) {                                         \
            fprintf(stderr, "CUDA error %s at %s:%d\n",                     \
                    cudaGetErrorString(err__), __FILE__, __LINE__);         \
            exit(EXIT_FAILURE);                                             \
        }                                                                   \
    } while (0)
"""
}
