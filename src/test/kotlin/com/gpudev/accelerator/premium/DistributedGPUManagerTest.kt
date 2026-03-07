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

class DistributedGPUManagerTest {

    private lateinit var manager: DistributedGPUManager

    @BeforeEach
    fun setUp() {
        val project = mockk<Project>(relaxed = true)
        manager = DistributedGPUManager(project)
    }

    @Test
    fun `discoverCluster returns multiple nodes`() {
        val nodes = manager.discoverCluster()
        assertTrue(nodes.size >= 3, "Should discover at least 3 nodes")
    }

    @Test
    fun `discoverCluster includes local node`() {
        val nodes = manager.discoverCluster()
        assertTrue(nodes.any { it.id == "local" })
    }

    @Test
    fun `discoverCluster nodes have GPUs`() {
        val nodes = manager.discoverCluster()
        nodes.forEach { node ->
            assertTrue(node.gpus.isNotEmpty(), "Node ${node.id} should have GPUs")
        }
    }

    @Test
    fun `scheduleWorkload assigns to available node`() {
        manager.discoverCluster()
        val workload = DistributedGPUManager.GPUWorkload(
            id = "test-1",
            name = "TestKernel",
            kernelFile = "test.cu",
            requiredMemory = 1_000_000_000L,
            estimatedRuntime = 5000L,
            priority = 1,
            gpuType = null,
            minComputeCapability = null
        )
        val distribution = manager.scheduleWorkload(workload)
        assertNotNull(distribution)
        assertEquals("test-1", distribution!!.workloadId)
    }

    @Test
    fun `scheduleWorkload filters by GPU type`() {
        manager.discoverCluster()
        val workload = DistributedGPUManager.GPUWorkload(
            id = "amd-task",
            name = "AMDKernel",
            kernelFile = "test.hip",
            requiredMemory = 1_000_000_000L,
            estimatedRuntime = 3000L,
            priority = 1,
            gpuType = DistributedGPUManager.GPUType.AMD_ROCM,
            minComputeCapability = null
        )
        // compute-2 has AMD GPUs but is BUSY, so should return null
        val distribution = manager.scheduleWorkload(workload)
        assertNull(distribution, "No AVAILABLE node with AMD GPUs")
    }

    @Test
    fun `scheduleWorkload returns null and queues when no suitable node`() {
        manager.discoverCluster()
        val workload = DistributedGPUManager.GPUWorkload(
            id = "huge-task",
            name = "HugeKernel",
            kernelFile = "huge.cu",
            requiredMemory = 999_999_999_999L,
            estimatedRuntime = 100000L,
            priority = 1,
            gpuType = null,
            minComputeCapability = null
        )
        val distribution = manager.scheduleWorkload(workload)
        assertNull(distribution, "No node with enough memory")
    }

    @Test
    fun `getClusterStatistics returns valid data after discovery`() {
        manager.discoverCluster()
        val stats = manager.getClusterStatistics()
        assertTrue(stats.totalGPUs > 0)
        assertTrue(stats.activeNodes > 0)
        assertTrue(stats.totalMemoryGB > 0.0)
    }

    @Test
    fun `getClusterStatistics counts queued workloads`() {
        manager.discoverCluster()
        // Schedule an impossible workload to queue it
        manager.scheduleWorkload(DistributedGPUManager.GPUWorkload(
            "q1", "Queued", "q.cu", 999_999_999_999L, 1000L, 1, null, null
        ))
        val stats = manager.getClusterStatistics()
        assertEquals(1, stats.queuedWorkloads)
    }

    @Test
    fun `optimizeDistribution detects underutilized GPUs`() {
        manager.discoverCluster()
        val optimizations = manager.optimizeDistribution()
        assertTrue(optimizations.any {
            it.type == DistributedGPUManager.OptimizationType.UNDERUTILIZED_RESOURCE
        })
    }

    @Test
    fun `GPUDevice data class construction`() {
        val gpu = DistributedGPUManager.GPUDevice(
            id = 0,
            name = "Test GPU",
            computeCapability = "9.0",
            totalMemory = 24_000_000_000L,
            freeMemory = 20_000_000_000L,
            temperature = 65,
            powerUsage = 300,
            utilization = 50,
            type = DistributedGPUManager.GPUType.NVIDIA_CUDA
        )
        assertEquals(0, gpu.id)
        assertEquals("Test GPU", gpu.name)
        assertEquals("9.0", gpu.computeCapability)
        assertEquals(DistributedGPUManager.GPUType.NVIDIA_CUDA, gpu.type)
    }

    @Test
    fun `GPUType enum has all expected values`() {
        val values = DistributedGPUManager.GPUType.values()
        assertEquals(4, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("NVIDIA_CUDA", "AMD_ROCM", "INTEL_ONEAPI", "APPLE_METAL")
        ))
    }

    @Test
    fun `NodeStatus enum has all expected values`() {
        val values = DistributedGPUManager.NodeStatus.values()
        assertEquals(4, values.size)
        assertTrue(values.map { it.name }.containsAll(
            listOf("AVAILABLE", "BUSY", "DEGRADED", "OFFLINE")
        ))
    }

    @Test
    fun `OptimizationType enum has all expected values`() {
        val values = DistributedGPUManager.OptimizationType.values()
        assertEquals(5, values.size)
    }

    @Test
    fun `ClusterStatistics data class construction`() {
        val stats = DistributedGPUManager.ClusterStatistics(
            totalGPUs = 8,
            activeNodes = 3,
            queuedWorkloads = 2,
            averageUtilization = 45.0,
            totalMemoryGB = 192.0,
            hotspots = listOf("node1:GPU0")
        )
        assertEquals(8, stats.totalGPUs)
        assertEquals(3, stats.activeNodes)
        assertEquals(2, stats.queuedWorkloads)
        assertEquals(45.0, stats.averageUtilization)
        assertEquals(192.0, stats.totalMemoryGB)
        assertEquals(1, stats.hotspots.size)
    }

    @Test
    fun `WorkloadDistribution has valid estimated times`() {
        manager.discoverCluster()
        val workload = DistributedGPUManager.GPUWorkload(
            "w1", "Kernel", "k.cu", 1_000_000L, 5000L, 1, null, null
        )
        val dist = manager.scheduleWorkload(workload)
        assertNotNull(dist)
        assertTrue(dist!!.estimatedCompletionTime.isAfter(dist.estimatedStartTime))
    }

    @Test
    fun `scheduleWorkload respects compute capability filter`() {
        manager.discoverCluster()
        // Request compute capability higher than any available GPU
        val workload = DistributedGPUManager.GPUWorkload(
            "cc-task", "HighCC", "test.cu", 1_000_000L, 1000L, 1,
            DistributedGPUManager.GPUType.NVIDIA_CUDA, "99.0"
        )
        val dist = manager.scheduleWorkload(workload)
        assertNull(dist, "No GPU meets compute capability 99.0")
    }
}
