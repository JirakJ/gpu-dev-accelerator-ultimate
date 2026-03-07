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
import java.time.Instant

class PerformanceRegressionTesterTest {

    private lateinit var tester: PerformanceRegressionTester

    @BeforeEach
    fun setUp() {
        val project = mockk<Project>(relaxed = true)
        tester = PerformanceRegressionTester(project)
    }

    @Test
    fun `runBenchmark returns valid result`() {
        val result = tester.runBenchmark("testKernel", "code", 1_000_000)
        assertEquals("testKernel", result.kernelName)
        assertTrue(result.executionTimeMs > 0.0)
        assertTrue(result.throughputGBps > 0.0)
        assertTrue(result.memoryUsageMB >= 0)
    }

    @Test
    fun `runBenchmark result has metrics`() {
        val result = tester.runBenchmark("testKernel", "code", 1_000_000)
        assertTrue(result.metrics.containsKey("stdDev"))
        assertTrue(result.metrics.containsKey("minTime"))
        assertTrue(result.metrics.containsKey("maxTime"))
        assertTrue(result.metrics.containsKey("occupancy"))
        assertTrue(result.metrics.containsKey("cacheHitRate"))
    }

    @Test
    fun `runBenchmark minTime is less than or equal to maxTime`() {
        val result = tester.runBenchmark("testKernel", "code", 1_000_000)
        assertTrue(result.metrics["minTime"]!! <= result.metrics["maxTime"]!!)
    }

    @Test
    fun `runBenchmark grid size matches input`() {
        val inputSize = 1024
        val result = tester.runBenchmark("testKernel", "code", inputSize)
        val expectedBlocks = (inputSize + 255) / 256
        assertEquals(Triple(expectedBlocks, 1, 1), result.gridSize)
        assertEquals(Triple(256, 1, 1), result.blockSize)
    }

    @Test
    fun `detectRegression with insufficient history`() {
        val analysis = tester.detectRegression("nonexistent")
        assertFalse(analysis.hasRegression)
        assertNull(analysis.regressionType)
        assertEquals(PerformanceRegressionTester.Severity.NONE, analysis.severity)
        assertTrue(analysis.details.contains("Insufficient"))
    }

    @Test
    fun `detectRegression with single benchmark has insufficient history`() {
        tester.runBenchmark("singleKernel", "code", 1_000_000)
        val analysis = tester.detectRegression("singleKernel")
        assertFalse(analysis.hasRegression)
        assertTrue(analysis.details.contains("Insufficient"))
    }

    @Test
    fun `detectRegression with stable performance shows no regression`() {
        // Run multiple benchmarks with same parameters for stable results
        repeat(5) {
            tester.runBenchmark("stableKernel", "code", 1_000_000)
        }
        val analysis = tester.detectRegression("stableKernel")
        // With simulated data and small variance, no regression expected
        assertFalse(analysis.hasRegression)
        assertEquals(PerformanceRegressionTester.Severity.NONE, analysis.severity)
    }

    @Test
    fun `compareBenchmarks with missing kernel returns empty differences`() {
        val report = tester.compareBenchmarks("missing", "abc", "def")
        assertNull(report.resultA)
        assertNull(report.resultB)
        assertTrue(report.differences.isEmpty())
    }

    @Test
    fun `compareBenchmarks with existing results returns differences`() {
        // Run benchmarks (they all get the same commitHash "abc123def456")
        tester.runBenchmark("compareKernel", "code", 1_000_000)
        tester.runBenchmark("compareKernel", "code", 1_000_000)
        val report = tester.compareBenchmarks("compareKernel", "abc123def456", "abc123def456")
        assertNotNull(report.resultA)
        assertNotNull(report.resultB)
        assertTrue(report.differences.isNotEmpty())
    }

    @Test
    fun `compareBenchmarks differences include all metrics`() {
        tester.runBenchmark("metricsKernel", "code", 1_000_000)
        tester.runBenchmark("metricsKernel", "code", 1_000_000)
        val report = tester.compareBenchmarks("metricsKernel", "abc123def456", "abc123def456")
        val metricNames = report.differences.map { it.metric }
        assertTrue(metricNames.contains("Execution Time"))
        assertTrue(metricNames.contains("Throughput"))
        assertTrue(metricNames.contains("Memory Usage"))
    }

    @Test
    fun `BenchmarkResult data class construction`() {
        val result = PerformanceRegressionTester.BenchmarkResult(
            kernelName = "test",
            timestamp = Instant.now(),
            commitHash = "abc123",
            executionTimeMs = 10.0,
            throughputGBps = 100.0,
            memoryUsageMB = 512,
            powerUsageW = 300,
            gridSize = Triple(4, 1, 1),
            blockSize = Triple(256, 1, 1),
            metrics = mapOf("stdDev" to 0.5)
        )
        assertEquals("test", result.kernelName)
        assertEquals("abc123", result.commitHash)
        assertEquals(10.0, result.executionTimeMs)
        assertEquals(Triple(4, 1, 1), result.gridSize)
    }

    @Test
    fun `RegressionType enum has all expected values`() {
        val values = PerformanceRegressionTester.RegressionType.values()
        assertEquals(4, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("PERFORMANCE", "MEMORY", "POWER", "THROUGHPUT")
        ))
    }

    @Test
    fun `Severity enum has all expected values`() {
        val values = PerformanceRegressionTester.Severity.values()
        assertEquals(5, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL")
        ))
    }

    @Test
    fun `MetricDifference data class construction`() {
        val diff = PerformanceRegressionTester.MetricDifference(
            metric = "Execution Time",
            valueA = 10.0,
            valueB = 15.0,
            percentageChange = 50.0,
            unit = "ms"
        )
        assertEquals("Execution Time", diff.metric)
        assertEquals(10.0, diff.valueA)
        assertEquals(15.0, diff.valueB)
        assertEquals(50.0, diff.percentageChange)
        assertEquals("ms", diff.unit)
    }

    @Test
    fun `runBenchmark with different input sizes produces different results`() {
        val smallResult = tester.runBenchmark("sizeTest", "code", 1_000)
        val largeResult = tester.runBenchmark("sizeTest", "code", 10_000_000)
        // Larger inputs should take longer
        assertTrue(largeResult.executionTimeMs > smallResult.executionTimeMs)
    }
}
