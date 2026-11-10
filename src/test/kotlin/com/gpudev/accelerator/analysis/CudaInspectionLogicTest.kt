/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.analysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CudaInspectionLogicTest {

    // --- memoryLeaks ---

    @Test
    fun `malloc without free is reported`() {
        val code = """
            float* d;
            cudaMalloc(&d, n * sizeof(float));
        """.trimIndent()
        val findings = CudaInspectionLogic.memoryLeaks(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("'d'"))
    }

    @Test
    fun `malloc with matching free is clean`() {
        val code = """
            float* d;
            cudaMalloc(&d, n * sizeof(float));
            cudaFree(d);
        """.trimIndent()
        assertTrue(CudaInspectionLogic.memoryLeaks(code).isEmpty())
    }

    @Test
    fun `void-cast malloc form is recognized`() {
        val code = "cudaMalloc((void**)&buf, 128);"
        val findings = CudaInspectionLogic.memoryLeaks(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("'buf'"))
    }

    @Test
    fun `mallocHost requires freeHost not free`() {
        val code = """
            cudaMallocHost(&h, 128);
            cudaFree(h);
        """.trimIndent()
        assertEquals(1, CudaInspectionLogic.memoryLeaks(code).size)
    }

    @Test
    fun `finding offsets point at the allocation`() {
        val code = "int x;\ncudaMalloc(&d, 4);"
        val f = CudaInspectionLogic.memoryLeaks(code).single()
        assertEquals(code.indexOf("cudaMalloc"), f.startOffset)
    }

    // --- uncheckedCalls ---

    @Test
    fun `bare cudaMemcpy statement is flagged`() {
        val code = "    cudaMemcpy(dst, src, n, cudaMemcpyHostToDevice);"
        val findings = CudaInspectionLogic.uncheckedCalls(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("cudaMemcpy"))
    }

    @Test
    fun `checked and assigned calls are not flagged`() {
        val checked = "CUDA_CHECK(cudaMemcpy(dst, src, n, cudaMemcpyHostToDevice));"
        val assigned = "cudaError_t err = cudaMemcpy(dst, src, n, cudaMemcpyHostToDevice);"
        assertTrue(CudaInspectionLogic.uncheckedCalls(checked).isEmpty())
        assertTrue(CudaInspectionLogic.uncheckedCalls(assigned).isEmpty())
    }

    @Test
    fun `call inside if condition is not flagged`() {
        val code = "if (cudaMalloc(&d, 4) != cudaSuccess) return;"
        assertTrue(CudaInspectionLogic.uncheckedCalls(code).isEmpty())
    }

    @Test
    fun `non-runtime identifiers are not flagged`() {
        val code = "myCudaMallocWrapper(&d, 4);\nkernel<<<1, 1>>>(d);"
        assertTrue(CudaInspectionLogic.uncheckedCalls(code).isEmpty())
    }
}
