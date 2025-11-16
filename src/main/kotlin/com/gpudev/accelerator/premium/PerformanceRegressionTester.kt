package com.gpudev.accelerator.premium

import com.intellij.openapi.project.Project
import java.time.Instant
import kotlin.math.abs
import kotlin.math.sqrt

class PerformanceRegressionTester(private val project: Project) {
    
    private val benchmarkHistory = mutableMapOf<String, MutableList<BenchmarkResult>>()
    
    data class BenchmarkResult(
        val kernelName: String,
        val timestamp: Instant,
        val commitHash: String?,
        val executionTimeMs: Double,
        val throughputGBps: Double,
        val memoryUsageMB: Long,
        val powerUsageW: Int,
        val gridSize: Triple<Int, Int, Int>,
        val blockSize: Triple<Int, Int, Int>,
        val metrics: Map<String, Double>
    )
    
    data class RegressionAnalysis(
        val hasRegression: Boolean,
        val regressionType: RegressionType?,
        val severity: Severity,
        val currentValue: Double,
        val baselineValue: Double,
        val percentageChange: Double,
        val details: String,
        val recommendations: List<String>
    )
    
    enum class RegressionType {
        PERFORMANCE, MEMORY, POWER, THROUGHPUT
    }
    
    enum class Severity {
        NONE, LOW, MEDIUM, HIGH, CRITICAL
    }
    
    fun runBenchmark(
        kernelName: String,
        kernelCode: String,
        inputSize: Int,
        iterations: Int = 100
    ): BenchmarkResult {
        if (!PremiumFeatureGate.isPremiumEnabled(project)) {
            return BenchmarkResult(
                kernelName, Instant.now(), null, 0.0, 0.0, 0, 0,
                Triple(0, 0, 0), Triple(0, 0, 0), emptyMap()
            )
        }
        
        // Simulate kernel execution and benchmarking
        val warmupRuns = 10
        val timings = mutableListOf<Double>()
        
        // Warmup
        repeat(warmupRuns) {
            simulateKernelExecution(inputSize)
        }
        
        // Actual benchmark
        repeat(iterations) {
            val executionTime = simulateKernelExecution(inputSize)
            timings.add(executionTime)
        }
        
        val avgTime = timings.average()
        val stdDev = calculateStdDev(timings)
        val dataProcessedGB = (inputSize * 4.0 * iterations) / 1_000_000_000.0
        val throughput = dataProcessedGB / (avgTime / 1000.0)
        
        val result = BenchmarkResult(
            kernelName = kernelName,
            timestamp = Instant.now(),
            commitHash = getCurrentGitCommit(),
            executionTimeMs = avgTime,
            throughputGBps = throughput,
            memoryUsageMB = estimateMemoryUsage(inputSize),
            powerUsageW = estimatePowerUsage(avgTime),
            gridSize = calculateGridSize(inputSize),
            blockSize = Triple(256, 1, 1),
            metrics = mapOf(
                "stdDev" to stdDev,
                "minTime" to timings.minOrNull()!!,
                "maxTime" to timings.maxOrNull()!!,
                "occupancy" to 0.85,
                "cacheHitRate" to 0.92
            )
        )
        
        // Store in history
        benchmarkHistory.getOrPut(kernelName) { mutableListOf() }.add(result)
        
        return result
    }
    
    private fun simulateKernelExecution(inputSize: Int): Double {
        // Simulate realistic GPU kernel execution time with variance
        val baseTime = inputSize / 1_000_000.0
        val variance = (Math.random() - 0.5) * 0.1
        return baseTime * (1.0 + variance)
    }
    
    private fun calculateStdDev(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    private fun getCurrentGitCommit(): String? {
        // Would integrate with Git to get current commit
        return "abc123def456"
    }
    
    private fun estimateMemoryUsage(inputSize: Int): Long {
        // Estimate memory based on input size (assuming float arrays)
        return ((inputSize * 4L * 3) / (1024 * 1024))
    }
    
    private fun estimatePowerUsage(executionTimeMs: Double): Int {
        // Simulate power usage (250W base + load-dependent)
        return (250 + (executionTimeMs * 10)).toInt()
    }
    
    private fun calculateGridSize(inputSize: Int): Triple<Int, Int, Int> {
        val threadsPerBlock = 256
        val blocks = (inputSize + threadsPerBlock - 1) / threadsPerBlock
        return Triple(blocks, 1, 1)
    }
    
    fun detectRegression(kernelName: String, threshold: Double = 5.0): RegressionAnalysis {
        if (!PremiumFeatureGate.isPremiumEnabled(project)) {
            return RegressionAnalysis(
                false, null, Severity.NONE, 0.0, 0.0, 0.0,
                "Premium feature required", emptyList()
            )
        }
        
        val history = benchmarkHistory[kernelName]
        if (history == null || history.size < 2) {
            return RegressionAnalysis(
                false, null, Severity.NONE, 0.0, 0.0, 0.0,
                "Insufficient benchmark history", emptyList()
            )
        }
        
        val current = history.last()
        val baseline = calculateBaseline(history.dropLast(1))
        
        // Check performance regression
        val perfChange = ((current.executionTimeMs - baseline.executionTimeMs) / baseline.executionTimeMs) * 100
        
        if (perfChange > threshold) {
            val severity = when {
                perfChange > 50 -> Severity.CRITICAL
                perfChange > 25 -> Severity.HIGH
                perfChange > 10 -> Severity.MEDIUM
                else -> Severity.LOW
            }
            
            val recommendations = generateRecommendations(current, baseline, RegressionType.PERFORMANCE)
            
            return RegressionAnalysis(
                hasRegression = true,
                regressionType = RegressionType.PERFORMANCE,
                severity = severity,
                currentValue = current.executionTimeMs,
                baselineValue = baseline.executionTimeMs,
                percentageChange = perfChange,
                details = "Execution time increased from ${baseline.executionTimeMs.format(2)}ms to ${current.executionTimeMs.format(2)}ms",
                recommendations = recommendations
            )
        }
        
        // Check memory regression
        val memChange = ((current.memoryUsageMB - baseline.memoryUsageMB).toDouble() / baseline.memoryUsageMB) * 100
        if (memChange > threshold) {
            return RegressionAnalysis(
                hasRegression = true,
                regressionType = RegressionType.MEMORY,
                severity = Severity.MEDIUM,
                currentValue = current.memoryUsageMB.toDouble(),
                baselineValue = baseline.memoryUsageMB.toDouble(),
                percentageChange = memChange,
                details = "Memory usage increased from ${baseline.memoryUsageMB}MB to ${current.memoryUsageMB}MB",
                recommendations = generateRecommendations(current, baseline, RegressionType.MEMORY)
            )
        }
        
        // Check throughput regression
        val throughputChange = ((baseline.throughputGBps - current.throughputGBps) / baseline.throughputGBps) * 100
        if (throughputChange > threshold) {
            return RegressionAnalysis(
                hasRegression = true,
                regressionType = RegressionType.THROUGHPUT,
                severity = Severity.HIGH,
                currentValue = current.throughputGBps,
                baselineValue = baseline.throughputGBps,
                percentageChange = -throughputChange,
                details = "Throughput decreased from ${baseline.throughputGBps.format(2)} GB/s to ${current.throughputGBps.format(2)} GB/s",
                recommendations = generateRecommendations(current, baseline, RegressionType.THROUGHPUT)
            )
        }
        
        return RegressionAnalysis(
            hasRegression = false,
            regressionType = null,
            severity = Severity.NONE,
            currentValue = current.executionTimeMs,
            baselineValue = baseline.executionTimeMs,
            percentageChange = perfChange,
            details = "No significant regression detected",
            recommendations = emptyList()
        )
    }
    
    private fun calculateBaseline(history: List<BenchmarkResult>): BenchmarkResult {
        // Use median of last N results for stable baseline
        val n = minOf(10, history.size)
        val recent = history.takeLast(n)
        
        return BenchmarkResult(
            kernelName = recent.first().kernelName,
            timestamp = recent.last().timestamp,
            commitHash = recent.last().commitHash,
            executionTimeMs = recent.map { it.executionTimeMs }.sorted()[n / 2],
            throughputGBps = recent.map { it.throughputGBps }.sorted()[n / 2],
            memoryUsageMB = recent.map { it.memoryUsageMB }.sorted()[n / 2],
            powerUsageW = recent.map { it.powerUsageW }.sorted()[n / 2],
            gridSize = recent.first().gridSize,
            blockSize = recent.first().blockSize,
            metrics = emptyMap()
        )
    }
    
    private fun generateRecommendations(
        current: BenchmarkResult,
        baseline: BenchmarkResult,
        type: RegressionType
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (type) {
            RegressionType.PERFORMANCE -> {
                recommendations.add("Review recent code changes in commit ${current.commitHash}")
                recommendations.add("Check for increased divergent branching in kernel code")
                recommendations.add("Verify memory access patterns are coalesced")
                
                val occupancyChange = (current.metrics["occupancy"] ?: 0.0) - (baseline.metrics["occupancy"] ?: 0.0)
                if (occupancyChange < -0.1) {
                    recommendations.add("Occupancy decreased - consider reducing register usage or shared memory")
                }
                
                val cacheChange = (current.metrics["cacheHitRate"] ?: 0.0) - (baseline.metrics["cacheHitRate"] ?: 0.0)
                if (cacheChange < -0.05) {
                    recommendations.add("Cache hit rate decreased - review data access patterns")
                }
            }
            
            RegressionType.MEMORY -> {
                recommendations.add("Memory usage increased - check for memory leaks")
                recommendations.add("Review allocation patterns and buffer sizes")
                recommendations.add("Consider using memory pooling or reusing buffers")
            }
            
            RegressionType.THROUGHPUT -> {
                recommendations.add("Throughput decreased - check memory bandwidth utilization")
                recommendations.add("Verify coalesced memory accesses")
                recommendations.add("Consider using shared memory for frequently accessed data")
                recommendations.add("Profile with nvprof/Nsight Compute for detailed memory metrics")
            }
            
            RegressionType.POWER -> {
                recommendations.add("Power consumption increased - review compute intensity")
                recommendations.add("Consider reducing clock speeds or optimizing algorithms")
            }
        }
        
        return recommendations
    }
    
    fun compareBenchmarks(kernelName: String, commitA: String, commitB: String): ComparisonReport {
        if (!PremiumFeatureGate.isPremiumEnabled(project)) {
            return ComparisonReport(null, null, emptyList())
        }
        
        val history = benchmarkHistory[kernelName] ?: return ComparisonReport(null, null, emptyList())
        
        val resultA = history.find { it.commitHash == commitA }
        val resultB = history.find { it.commitHash == commitB }
        
        if (resultA == null || resultB == null) {
            return ComparisonReport(resultA, resultB, emptyList())
        }
        
        val differences = mutableListOf<MetricDifference>()
        
        differences.add(MetricDifference(
            "Execution Time",
            resultA.executionTimeMs,
            resultB.executionTimeMs,
            ((resultB.executionTimeMs - resultA.executionTimeMs) / resultA.executionTimeMs) * 100,
            "ms"
        ))
        
        differences.add(MetricDifference(
            "Throughput",
            resultA.throughputGBps,
            resultB.throughputGBps,
            ((resultB.throughputGBps - resultA.throughputGBps) / resultA.throughputGBps) * 100,
            "GB/s"
        ))
        
        differences.add(MetricDifference(
            "Memory Usage",
            resultA.memoryUsageMB.toDouble(),
            resultB.memoryUsageMB.toDouble(),
            ((resultB.memoryUsageMB - resultA.memoryUsageMB).toDouble() / resultA.memoryUsageMB) * 100,
            "MB"
        ))
        
        return ComparisonReport(resultA, resultB, differences)
    }
    
    data class ComparisonReport(
        val resultA: BenchmarkResult?,
        val resultB: BenchmarkResult?,
        val differences: List<MetricDifference>
    )
    
    data class MetricDifference(
        val metric: String,
        val valueA: Double,
        val valueB: Double,
        val percentageChange: Double,
        val unit: String
    )
    
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
}
