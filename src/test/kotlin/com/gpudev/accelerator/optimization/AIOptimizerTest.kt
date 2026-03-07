/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.optimization

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AIOptimizerTest {

    @Test
    fun `analyzeKernel detects kernel declaration`() {
        val code = "__global__ void myKernel() { }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("✓ Kernel detected"), "Should detect __global__ kernel")
    }

    @Test
    fun `analyzeKernel suggests shared memory when not used`() {
        val code = "__global__ void myKernel() { int x = 1; }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("Consider using shared memory"), "Should suggest shared memory")
    }

    @Test
    fun `analyzeKernel does not suggest shared memory when already used`() {
        val code = "__global__ void myKernel() { __shared__ int buf[256]; }"
        val result = AIOptimizer.analyzeKernel(code)
        assertFalse(result.contains("Consider using shared memory"))
    }

    @Test
    fun `analyzeKernel suggests syncthreads when not present`() {
        val code = "__global__ void myKernel() { int x = 1; }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("Add __syncthreads()"), "Should suggest syncthreads")
    }

    @Test
    fun `analyzeKernel does not suggest syncthreads when already present`() {
        val code = "__global__ void myKernel() { __syncthreads(); }"
        val result = AIOptimizer.analyzeKernel(code)
        assertFalse(result.contains("Add __syncthreads()"))
    }

    @Test
    fun `analyzeKernel suggests restrict for pointer parameters`() {
        val code = "__global__ void myKernel(float *data) { }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("__restrict__"), "Should suggest __restrict__")
    }

    @Test
    fun `analyzeKernel does not suggest restrict when already present`() {
        val code = "__global__ void myKernel(float * __restrict__ data) { }"
        val result = AIOptimizer.analyzeKernel(code)
        assertFalse(result.contains("Use __restrict__ on pointers"))
    }

    @Test
    fun `analyzeKernel returns no suggestions for non-kernel code`() {
        val code = "int main() { return 0; }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("No specific optimization suggestions"))
    }

    @Test
    fun `analyzeKernel suggests profiling`() {
        val code = "__global__ void myKernel() { }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("nvprof"), "Should suggest profiling")
    }

    @Test
    fun `analyzeKernel suggests block size`() {
        val code = "__global__ void myKernel() { }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("256 or 512"), "Should suggest optimal block sizes")
    }

    @Test
    fun `analyzeKernel returns optimization suggestions header for kernel code`() {
        val code = "__global__ void myKernel() { }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.startsWith("Optimization Suggestions:"))
    }

    @Test
    fun `analyzeKernel suggests coalesced access when pattern not found`() {
        val code = "__global__ void myKernel() { int x = data[threadIdx.x]; }"
        val result = AIOptimizer.analyzeKernel(code)
        assertTrue(result.contains("Ensure memory accesses are coalesced"))
    }
}
