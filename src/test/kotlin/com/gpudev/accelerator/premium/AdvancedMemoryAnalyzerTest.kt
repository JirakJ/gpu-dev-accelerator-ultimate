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

class AdvancedMemoryAnalyzerTest {

    private lateinit var analyzer: AdvancedMemoryAnalyzer

    @BeforeEach
    fun setUp() {
        val project = mockk<Project>(relaxed = true)
        analyzer = AdvancedMemoryAnalyzer(project)
    }

    @Test
    fun `detectRaceConditions finds shared memory without sync`() {
        val code = "__shared__ int buf[256]; buf[tid] = data[tid];"
        val issues = analyzer.detectRaceConditions(code)
        assertTrue(issues.any { it.contains("race condition") })
    }

    @Test
    fun `detectRaceConditions no issue when syncthreads present`() {
        val code = "__shared__ int buf[256]; __syncthreads(); buf[tid] = data[tid];"
        val issues = analyzer.detectRaceConditions(code)
        assertFalse(issues.any { it.contains("race condition") })
    }

    @Test
    fun `detectRaceConditions finds atomic float precision issue`() {
        val code = "atomicAdd(&result, float_val); float sum = 0.0f;"
        val issues = analyzer.detectRaceConditions(code)
        assertTrue(issues.any { it.contains("precision") })
    }

    @Test
    fun `detectRaceConditions returns empty for clean code`() {
        val code = "int x = 42; x += 1;"
        val issues = analyzer.detectRaceConditions(code)
        assertTrue(issues.isEmpty())
    }

    @Test
    fun `detectRaceConditions detects multiple issues`() {
        val code = "__shared__ float buf[256]; atomicAdd(&buf[0], 1.0f); float x = 1.0f;"
        val issues = analyzer.detectRaceConditions(code)
        assertEquals(2, issues.size)
    }

    @Test
    fun `MemoryUsage totalBytes sums all memory types`() {
        val usage = AdvancedMemoryAnalyzer.MemoryUsage(
            deviceBytes = 1000L,
            hostBytes = 2000L,
            unifiedBytes = 3000L
        )
        assertEquals(6000L, usage.totalBytes)
    }

    @Test
    fun `MemoryUsage deviceGB converts correctly`() {
        val oneGB = (1024L * 1024L * 1024L)
        val usage = AdvancedMemoryAnalyzer.MemoryUsage(
            deviceBytes = oneGB,
            hostBytes = 0L,
            unifiedBytes = 0L
        )
        assertEquals(1.0, usage.deviceGB, 0.001)
    }

    @Test
    fun `MemoryUsage hostGB converts correctly`() {
        val twoGB = (2L * 1024L * 1024L * 1024L)
        val usage = AdvancedMemoryAnalyzer.MemoryUsage(
            deviceBytes = 0L,
            hostBytes = twoGB,
            unifiedBytes = 0L
        )
        assertEquals(2.0, usage.hostGB, 0.001)
    }

    @Test
    fun `MemoryUsage zero bytes`() {
        val usage = AdvancedMemoryAnalyzer.MemoryUsage(0L, 0L, 0L)
        assertEquals(0L, usage.totalBytes)
        assertEquals(0.0, usage.deviceGB, 0.001)
        assertEquals(0.0, usage.hostGB, 0.001)
    }

    @Test
    fun `MemoryAllocation data class construction`() {
        val alloc = AdvancedMemoryAnalyzer.MemoryAllocation(
            address = "0xDEADBEEF",
            size = 4096L,
            type = AdvancedMemoryAnalyzer.AllocationType.DEVICE,
            stackTrace = "at main.cu:10",
            timestamp = 1000L
        )
        assertEquals("0xDEADBEEF", alloc.address)
        assertEquals(4096L, alloc.size)
        assertEquals(AdvancedMemoryAnalyzer.AllocationType.DEVICE, alloc.type)
    }

    @Test
    fun `AllocationType enum has all expected values`() {
        val values = AdvancedMemoryAnalyzer.AllocationType.values()
        assertEquals(4, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("DEVICE", "HOST", "UNIFIED", "PINNED")
        ))
    }

    @Test
    fun `Severity enum has all expected values`() {
        val values = AdvancedMemoryAnalyzer.Severity.values()
        assertEquals(4, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("CRITICAL", "HIGH", "MEDIUM", "LOW")
        ))
    }

    @Test
    fun `MemoryLeak data class construction`() {
        val alloc = AdvancedMemoryAnalyzer.MemoryAllocation(
            "0x1000", 512L, AdvancedMemoryAnalyzer.AllocationType.UNIFIED, "trace", 0L
        )
        val leak = AdvancedMemoryAnalyzer.MemoryLeak(
            allocation = alloc,
            severity = AdvancedMemoryAnalyzer.Severity.CRITICAL,
            recommendation = "Free this memory"
        )
        assertEquals(AdvancedMemoryAnalyzer.Severity.CRITICAL, leak.severity)
        assertEquals("Free this memory", leak.recommendation)
        assertEquals(alloc, leak.allocation)
    }

    @Test
    fun `MemoryReport data class construction`() {
        val allocations = listOf(
            AdvancedMemoryAnalyzer.MemoryAllocation(
                "0x1000", 1024L, AdvancedMemoryAnalyzer.AllocationType.DEVICE, "trace", 0L
            )
        )
        val usage = AdvancedMemoryAnalyzer.MemoryUsage(1024L, 0L, 0L)
        val report = AdvancedMemoryAnalyzer.MemoryReport(allocations, emptyList(), usage)
        assertEquals(1, report.allocations.size)
        assertTrue(report.leaks.isEmpty())
        assertEquals(1024L, report.usage.totalBytes)
    }

    @Test
    fun `analyzeMemory returns non-null report`() {
        val report = analyzer.analyzeMemory("test_executable")
        assertNotNull(report)
        assertTrue(report!!.allocations.isNotEmpty())
        assertTrue(report.leaks.isNotEmpty())
    }
}
