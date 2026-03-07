/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.premium

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RealTimeProfilerTest {

    @Test
    fun `KernelMetrics data class construction`() {
        val metrics = RealTimeProfiler.KernelMetrics(
            name = "myKernel",
            executionTimeMs = 5.0,
            occupancy = 0.85,
            memoryBandwidthGB = 400.0,
            registerUsage = 64,
            sharedMemoryBytes = 16384,
            warpEfficiency = 0.95
        )
        assertEquals("myKernel", metrics.name)
        assertEquals(5.0, metrics.executionTimeMs)
        assertEquals(0.85, metrics.occupancy)
        assertEquals(400.0, metrics.memoryBandwidthGB)
        assertEquals(64, metrics.registerUsage)
        assertEquals(16384, metrics.sharedMemoryBytes)
        assertEquals(0.95, metrics.warpEfficiency)
    }

    @Test
    fun `FlameGraphNode construction without children`() {
        val node = RealTimeProfiler.FlameGraphNode("leaf", 100)
        assertEquals("leaf", node.name)
        assertEquals(100, node.duration)
        assertTrue(node.children.isEmpty())
    }

    @Test
    fun `FlameGraphNode construction with children`() {
        val child1 = RealTimeProfiler.FlameGraphNode("child1", 40)
        val child2 = RealTimeProfiler.FlameGraphNode("child2", 60)
        val parent = RealTimeProfiler.FlameGraphNode("parent", 100, listOf(child1, child2))
        assertEquals(2, parent.children.size)
        assertEquals("child1", parent.children[0].name)
        assertEquals("child2", parent.children[1].name)
    }

    @Test
    fun `FlameGraphNode nested hierarchy`() {
        val grandchild = RealTimeProfiler.FlameGraphNode("gc", 20)
        val child = RealTimeProfiler.FlameGraphNode("child", 50, listOf(grandchild))
        val root = RealTimeProfiler.FlameGraphNode("root", 100, listOf(child))
        assertEquals(1, root.children.size)
        assertEquals(1, root.children[0].children.size)
        assertEquals("gc", root.children[0].children[0].name)
    }

    @Test
    fun `KernelMetrics equality`() {
        val m1 = RealTimeProfiler.KernelMetrics("k", 1.0, 0.5, 200.0, 32, 1024, 0.9)
        val m2 = RealTimeProfiler.KernelMetrics("k", 1.0, 0.5, 200.0, 32, 1024, 0.9)
        assertEquals(m1, m2)
    }

    @Test
    fun `KernelMetrics copy with modification`() {
        val original = RealTimeProfiler.KernelMetrics("k", 1.0, 0.5, 200.0, 32, 1024, 0.9)
        val modified = original.copy(occupancy = 0.95)
        assertEquals(0.95, modified.occupancy)
        assertEquals(original.name, modified.name)
    }
}
