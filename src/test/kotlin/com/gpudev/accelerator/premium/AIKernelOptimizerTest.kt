/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.premium

import com.intellij.openapi.project.Project
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AIKernelOptimizerTest {

    private lateinit var optimizer: AIKernelOptimizer

    @BeforeEach
    fun setUp() {
        val project = mockk<Project>(relaxed = true)
        optimizer = AIKernelOptimizer(project)
    }

    @Test
    fun `analyzeKernel detects non-coalesced memory access`() {
        val code = "data = array[threadIdx.x * stride];"
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.any { it.title.contains("Non-coalesced") })
        assertTrue(suggestions.any { it.impact == AIKernelOptimizer.Impact.HIGH })
    }

    @Test
    fun `analyzeKernel detects shared memory bank conflict`() {
        val code = "__shared__ int buf[256]; int x = buf[threadIdx.x];"
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.any { it.title.contains("Bank Conflict") })
        assertTrue(suggestions.any { it.impact == AIKernelOptimizer.Impact.MEDIUM })
    }

    @Test
    fun `analyzeKernel detects high register usage from doubles`() {
        val code = (1..11).joinToString("\n") { "double val$it = $it.0;" }
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.any { it.title.contains("High Register Usage") })
    }

    @Test
    fun `analyzeKernel detects high register usage from floats`() {
        val code = (1..16).joinToString("\n") { "float val$it = ${it}f;" }
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.any { it.title.contains("High Register Usage") })
    }

    @Test
    fun `analyzeKernel detects warp divergence`() {
        val code = "if (threadIdx.x > 16) { doWork(); }"
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.any { it.title.contains("Warp Divergence") })
        assertTrue(suggestions.any { it.impact == AIKernelOptimizer.Impact.HIGH })
    }

    @Test
    fun `analyzeKernel detects loop unrolling opportunity`() {
        val code = "for (int i = 0; i < N; i++) { sum += data[i]; }"
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.any { it.title.contains("Loop Unrolling") })
    }

    @Test
    fun `analyzeKernel does not suggest loop unrolling with pragma`() {
        val code = "#pragma unroll\nfor (int i = 0; i < N; i++) { sum += data[i]; }"
        val suggestions = optimizer.analyzeKernel(code)!!
        assertFalse(suggestions.any { it.title.contains("Loop Unrolling") })
    }

    @Test
    fun `analyzeKernel returns empty list for clean code`() {
        val code = "int x = 42;"
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `analyzeKernel detects multiple issues`() {
        val code = """
            __shared__ int buf[256];
            int val = buf[threadIdx.x];
            if (threadIdx.x > 0) { work(); }
            for (int i = 0; i < N; i++) { sum += i; }
        """.trimIndent()
        val suggestions = optimizer.analyzeKernel(code)!!
        assertTrue(suggestions.size >= 3, "Should detect bank conflict, warp divergence, and loop unrolling")
    }

    @Test
    fun `OptimizationSuggestion data class properties`() {
        val suggestion = AIKernelOptimizer.OptimizationSuggestion(
            title = "Test",
            description = "Description",
            impact = AIKernelOptimizer.Impact.HIGH,
            codeChange = "fix code",
            estimatedSpeedup = 2.0
        )
        assertEquals("Test", suggestion.title)
        assertEquals("Description", suggestion.description)
        assertEquals(AIKernelOptimizer.Impact.HIGH, suggestion.impact)
        assertEquals("fix code", suggestion.codeChange)
        assertEquals(2.0, suggestion.estimatedSpeedup)
    }

    @Test
    fun `OptimizationSuggestion allows null codeChange`() {
        val suggestion = AIKernelOptimizer.OptimizationSuggestion(
            title = "Test",
            description = "Desc",
            impact = AIKernelOptimizer.Impact.LOW,
            codeChange = null,
            estimatedSpeedup = 1.0
        )
        assertNull(suggestion.codeChange)
    }

    @Test
    fun `Impact enum has all expected values`() {
        val values = AIKernelOptimizer.Impact.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(AIKernelOptimizer.Impact.HIGH))
        assertTrue(values.contains(AIKernelOptimizer.Impact.MEDIUM))
        assertTrue(values.contains(AIKernelOptimizer.Impact.LOW))
    }

    @Test
    fun `benchmarkOptimization returns positive speedup`() {
        val result = optimizer.benchmarkOptimization("original", "optimized")
        assertTrue(result > 0.0)
    }

    @Test
    fun `analyzeKernel suggestions have positive estimated speedup`() {
        val code = "if (threadIdx.x > 0) { for (int i=0;i<N;i++) {} }"
        val suggestions = optimizer.analyzeKernel(code)!!
        suggestions.forEach { suggestion ->
            assertTrue(suggestion.estimatedSpeedup > 1.0,
                "Speedup should be > 1.0 for ${suggestion.title}")
        }
    }
}
