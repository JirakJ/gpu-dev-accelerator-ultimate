/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.analysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KernelStaticAnalyzerTest {

    private fun titles(code: String) = KernelStaticAnalyzer.analyze(code).map { it.title }

    @Test
    fun `clean kernel produces no findings`() {
        val code = """
            __global__ void add(const float* __restrict__ a, float* __restrict__ out, int n) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx < n) {
                    out[idx] = a[idx] + 1.0f;
                }
            }
        """.trimIndent()
        assertTrue(KernelStaticAnalyzer.analyze(code).isEmpty(), "unexpected: ${titles(code)}")
    }

    @Test
    fun `detects strided access`() {
        val code = "out[threadIdx.x * stride] = 0;"
        assertTrue(titles(code).any { it.contains("Strided") })
    }

    @Test
    fun `detects syncthreads in divergent branch with line`() {
        val code = """
            __global__ void k(float* d, int n) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                if (idx < n) { d[idx] = 1.0f; }
                if (threadIdx.x < 16) {
                    __syncthreads();
                }
            }
        """.trimIndent()
        val finding = KernelStaticAnalyzer.analyze(code).first { it.title.contains("__syncthreads") }
        assertEquals(KernelStaticAnalyzer.Impact.HIGH, finding.impact)
        assertEquals(3, finding.line)
    }

    @Test
    fun `detects missing restrict on kernel pointers`() {
        val code = "__global__ void k(float* a, float* b) { }"
        assertTrue(titles(code).any { it.contains("__restrict__") })
    }

    @Test
    fun `detects missing bounds check`() {
        val code = """
            __global__ void k(float* __restrict__ d) {
                int idx = blockIdx.x * blockDim.x + threadIdx.x;
                d[idx] = 0.0f;
            }
        """.trimIndent()
        assertTrue(titles(code).any { it.contains("bounds check") })
    }

    @Test
    fun `detects pow with small integer exponent`() {
        assertTrue(titles("float y = pow(x, 2.0);").any { it.contains("pow()") })
        assertTrue(titles("float y = powf(x, 3);").any { it.contains("pow()") })
        assertFalse(titles("float y = powf(x, 2.5f);").any { it.contains("pow()") })
    }

    @Test
    fun `detects double literal in float init`() {
        assertTrue(titles("float x = 3.14;").any { it.contains("Double-precision") })
        assertFalse(titles("float x = 3.14f;").any { it.contains("Double-precision") })
    }

    @Test
    fun `detects atomic in loop`() {
        val code = "for (int i = 0; i < n; i++) { atomicAdd(&sum, d[i]); }"
        assertTrue(titles(code).any { it.contains("atomicAdd") })
    }

    @Test
    fun `findings carry valid line numbers`() {
        val code = "int a;\nint b;\nout[threadIdx.x * 4] = 0;"
        val finding = KernelStaticAnalyzer.analyze(code).first { it.title.contains("Strided") }
        assertEquals(2, finding.line)
    }
}
