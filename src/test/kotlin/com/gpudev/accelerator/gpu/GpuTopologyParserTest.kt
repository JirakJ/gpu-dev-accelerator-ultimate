/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.gpu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GpuTopologyParserTest {

    @Test
    fun `parses device query csv`() {
        val csv = """
            0, NVIDIA RTX 4090, 24564, 1024, 35, 62, 180.50
            1, NVIDIA RTX 4090, 24564, 512, 10, 45, 90.25
        """.trimIndent()
        val nodes = GpuTopologyParser.parseDeviceQuery(csv)
        assertEquals(2, nodes.size)
        assertEquals("NVIDIA RTX 4090", nodes[0].name)
        assertEquals(24564, nodes[0].memoryTotalMiB)
        assertEquals(35, nodes[0].utilizationPercent)
        assertEquals(90.25, nodes[1].powerDrawW, 1e-9)
    }

    @Test
    fun `ignores malformed lines`() {
        assertTrue(GpuTopologyParser.parseDeviceQuery("garbage\nshort, line").isEmpty())
    }

    @Test
    fun `parses topology matrix links`() {
        val matrix = """
            	GPU0	GPU1	GPU2	CPU Affinity
            GPU0	 X 	NV2	PHB	0-15
            GPU1	NV2	 X 	SYS	0-15
            GPU2	PHB	SYS	 X 	16-31
        """.trimIndent()
        val links = GpuTopologyParser.parseTopologyMatrix(matrix)
        assertEquals(3, links.size)
        assertTrue(links.contains(GpuLink(0, 1, "NV2")))
        assertTrue(links.contains(GpuLink(0, 2, "PHB")))
        assertTrue(links.contains(GpuLink(1, 2, "SYS")))
    }

    @Test
    fun `empty matrix yields no links`() {
        assertTrue(GpuTopologyParser.parseTopologyMatrix("").isEmpty())
    }
}
