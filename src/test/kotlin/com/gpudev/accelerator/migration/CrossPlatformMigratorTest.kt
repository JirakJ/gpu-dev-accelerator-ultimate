/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.migration

import com.intellij.psi.PsiFile
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CrossPlatformMigratorTest {

    private lateinit var migrator: CrossPlatformMigrator

    @BeforeEach
    fun setUp() {
        migrator = CrossPlatformMigrator()
    }

    private fun mockPsiFile(text: String): PsiFile {
        val file = mockk<PsiFile>(relaxed = true)
        every { file.text } returns text
        return file
    }

    // --- CUDA to Metal ---

    @Test
    fun `cudaToMetal converts threadIdx`() {
        val cuda = "int tid = threadIdx.x;"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.success)
        assertTrue(result.generatedCode.contains("thread_position_in_threadgroup.x"))
        assertFalse(result.generatedCode.contains("threadIdx.x"))
    }

    @Test
    fun `cudaToMetal converts blockIdx`() {
        val cuda = "int bid = blockIdx.y;"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.generatedCode.contains("threadgroup_position_in_grid.y"))
    }

    @Test
    fun `cudaToMetal converts blockDim`() {
        val cuda = "int size = blockDim.z;"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.generatedCode.contains("threads_per_threadgroup.z"))
    }

    @Test
    fun `cudaToMetal converts kernel declaration`() {
        val cuda = "__global__ void myKernel() {}"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.generatedCode.contains("kernel void"))
        assertFalse(result.generatedCode.contains("__global__"))
    }

    @Test
    fun `cudaToMetal converts shared memory`() {
        val cuda = "__shared__ int buf[256];"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.generatedCode.contains("threadgroup"))
        assertFalse(result.generatedCode.contains("__shared__"))
    }

    @Test
    fun `cudaToMetal converts syncthreads`() {
        val cuda = "__syncthreads();"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.generatedCode.contains("threadgroup_barrier"))
    }

    @Test
    fun `cudaToMetal adds metal header`() {
        val cuda = "__global__ void myKernel() {}"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.generatedCode.contains("#include <metal_stdlib>"))
        assertTrue(result.generatedCode.contains("using namespace metal;"))
    }

    @Test
    fun `cudaToMetal warns about host-side CUDA APIs`() {
        val cuda = "cudaMalloc(&dev_ptr, size); cudaMemcpy(dev, host, size, cudaMemcpyHostToDevice);"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.warnings.any { it.contains("Host-side CUDA API") })
    }

    @Test
    fun `cudaToMetal populates API mappings`() {
        val cuda = "__global__ void k() { __shared__ int x; }"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.METAL
        )
        assertTrue(result.apiMappings.isNotEmpty())
        assertEquals("kernel", result.apiMappings["__global__"])
        assertEquals("threadgroup", result.apiMappings["__shared__"])
    }

    // --- CUDA to OpenCL ---

    @Test
    fun `cudaToOpenCL converts threadIdx`() {
        val cuda = "int tid = threadIdx.x + threadIdx.y;"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.OPENCL
        )
        assertTrue(result.success)
        assertTrue(result.generatedCode.contains("get_local_id(0)"))
        assertTrue(result.generatedCode.contains("get_local_id(1)"))
    }

    @Test
    fun `cudaToOpenCL converts kernel declaration`() {
        val cuda = "__global__ void myKernel() {}"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.OPENCL
        )
        assertTrue(result.generatedCode.contains("__kernel void"))
    }

    @Test
    fun `cudaToOpenCL converts shared to local`() {
        val cuda = "__shared__ float buf[128];"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.OPENCL
        )
        assertTrue(result.generatedCode.contains("__local"))
    }

    @Test
    fun `cudaToOpenCL converts syncthreads to barrier`() {
        val cuda = "__syncthreads();"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.OPENCL
        )
        assertTrue(result.generatedCode.contains("barrier(CLK_LOCAL_MEM_FENCE)"))
    }

    // --- CUDA to HIP ---

    @Test
    fun `cudaToHIP replaces cuda prefix with hip`() {
        val cuda = "cudaMalloc(&ptr, size);"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.HIP
        )
        assertTrue(result.success)
        assertTrue(result.generatedCode.contains("hipMalloc"))
    }

    @Test
    fun `cudaToHIP replaces specific types`() {
        val cuda = "cudaError_t err = cudaSuccess; cudaStream_t stream; cudaEvent_t event;"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.HIP
        )
        assertTrue(result.generatedCode.contains("hipError_t"))
        assertTrue(result.generatedCode.contains("hipSuccess"))
        assertTrue(result.generatedCode.contains("hipStream_t"))
        assertTrue(result.generatedCode.contains("hipEvent_t"))
    }

    // --- Metal to CUDA ---

    @Test
    fun `metalToCuda converts kernel declaration`() {
        val metal = "kernel void myKernel() {}"
        val result = migrator.migrateKernel(
            mockPsiFile(metal),
            CrossPlatformMigrator.GPUPlatform.METAL,
            CrossPlatformMigrator.GPUPlatform.CUDA
        )
        assertTrue(result.success)
        assertTrue(result.generatedCode.contains("__global__ void"))
    }

    @Test
    fun `metalToCuda converts thread indexing`() {
        val metal = "int tid = thread_position_in_threadgroup.x;"
        val result = migrator.migrateKernel(
            mockPsiFile(metal),
            CrossPlatformMigrator.GPUPlatform.METAL,
            CrossPlatformMigrator.GPUPlatform.CUDA
        )
        assertTrue(result.generatedCode.contains("threadIdx.x"))
    }

    // --- OpenCL to CUDA ---

    @Test
    fun `openCLToCuda converts kernel declaration`() {
        val opencl = "__kernel void myKernel() {}"
        val result = migrator.migrateKernel(
            mockPsiFile(opencl),
            CrossPlatformMigrator.GPUPlatform.OPENCL,
            CrossPlatformMigrator.GPUPlatform.CUDA
        )
        assertTrue(result.success)
        assertTrue(result.generatedCode.contains("__global__ void"))
    }

    @Test
    fun `openCLToCuda converts thread indexing`() {
        val opencl = "int tid = get_local_id(0); int gid = get_group_id(1);"
        val result = migrator.migrateKernel(
            mockPsiFile(opencl),
            CrossPlatformMigrator.GPUPlatform.OPENCL,
            CrossPlatformMigrator.GPUPlatform.CUDA
        )
        assertTrue(result.generatedCode.contains("threadIdx.x"))
        assertTrue(result.generatedCode.contains("blockIdx.y"))
    }

    @Test
    fun `openCLToCuda converts barrier to syncthreads`() {
        val opencl = "barrier(CLK_LOCAL_MEM_FENCE);"
        val result = migrator.migrateKernel(
            mockPsiFile(opencl),
            CrossPlatformMigrator.GPUPlatform.OPENCL,
            CrossPlatformMigrator.GPUPlatform.CUDA
        )
        assertTrue(result.generatedCode.contains("__syncthreads()"))
    }

    // --- Unsupported paths ---

    @Test
    fun `unsupported migration returns failure`() {
        val code = "some code"
        val result = migrator.migrateKernel(
            mockPsiFile(code),
            CrossPlatformMigrator.GPUPlatform.VULKAN,
            CrossPlatformMigrator.GPUPlatform.SYCL
        )
        assertFalse(result.success)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `cudaToVulkan returns failure with warnings`() {
        val cuda = "__global__ void k() {}"
        val result = migrator.migrateKernel(
            mockPsiFile(cuda),
            CrossPlatformMigrator.GPUPlatform.CUDA,
            CrossPlatformMigrator.GPUPlatform.VULKAN
        )
        assertFalse(result.success)
        assertTrue(result.warnings.any { it.contains("Vulkan") })
    }

    // --- GPUPlatform enum ---

    @Test
    fun `GPUPlatform enum has all expected values`() {
        val values = CrossPlatformMigrator.GPUPlatform.values()
        assertEquals(6, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("CUDA", "METAL", "OPENCL", "VULKAN", "HIP", "SYCL")
        ))
    }

    // --- Portability score ---

    @Test
    fun `MigrationResult data class construction`() {
        val result = CrossPlatformMigrator.MigrationResult(
            success = true,
            generatedCode = "code",
            warnings = listOf("warn1"),
            performanceNotes = listOf("note1"),
            apiMappings = mapOf("a" to "b")
        )
        assertTrue(result.success)
        assertEquals("code", result.generatedCode)
        assertEquals(1, result.warnings.size)
        assertEquals(1, result.performanceNotes.size)
        assertEquals("b", result.apiMappings["a"])
    }

    @Test
    fun `PortabilityIssue data class construction`() {
        val issue = CrossPlatformMigrator.PortabilityIssue(
            category = "Warp primitives",
            description = "Not portable",
            severity = CrossPlatformMigrator.Severity.HIGH
        )
        assertEquals("Warp primitives", issue.category)
        assertEquals(CrossPlatformMigrator.Severity.HIGH, issue.severity)
    }

    @Test
    fun `Severity enum has all expected values`() {
        val values = CrossPlatformMigrator.Severity.values()
        assertEquals(4, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("LOW", "MEDIUM", "HIGH", "CRITICAL")
        ))
    }
}
