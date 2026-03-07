/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.premium

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AdvancedDebuggerTest {

    @Test
    fun `GPUThreadInfo data class construction`() {
        val info = AdvancedDebugger.GPUThreadInfo(
            blockIdx = Triple(0, 0, 0),
            threadIdx = Triple(1, 2, 3),
            warpId = 0,
            isActive = true
        )
        assertEquals(Triple(0, 0, 0), info.blockIdx)
        assertEquals(Triple(1, 2, 3), info.threadIdx)
        assertEquals(0, info.warpId)
        assertTrue(info.isActive)
    }

    @Test
    fun `GPUThreadInfo equality`() {
        val info1 = AdvancedDebugger.GPUThreadInfo(Triple(0, 0, 0), Triple(0, 0, 0), 0, true)
        val info2 = AdvancedDebugger.GPUThreadInfo(Triple(0, 0, 0), Triple(0, 0, 0), 0, true)
        assertEquals(info1, info2)
    }

    @Test
    fun `GPUThreadInfo inequality on different thread`() {
        val info1 = AdvancedDebugger.GPUThreadInfo(Triple(0, 0, 0), Triple(0, 0, 0), 0, true)
        val info2 = AdvancedDebugger.GPUThreadInfo(Triple(0, 0, 0), Triple(1, 0, 0), 0, true)
        assertNotEquals(info1, info2)
    }

    @Test
    fun `MemoryWatch data class construction`() {
        val watch = AdvancedDebugger.MemoryWatch(
            address = "0xDEADBEEF",
            size = 4096L,
            type = "float",
            value = "3.14"
        )
        assertEquals("0xDEADBEEF", watch.address)
        assertEquals(4096L, watch.size)
        assertEquals("float", watch.type)
        assertEquals("3.14", watch.value)
    }

    @Test
    fun `MemoryWatch copy preserves fields`() {
        val original = AdvancedDebugger.MemoryWatch("0x1000", 256L, "int", "42")
        val copy = original.copy(value = "100")
        assertEquals("0x1000", copy.address)
        assertEquals(256L, copy.size)
        assertEquals("int", copy.type)
        assertEquals("100", copy.value)
    }

    @Test
    fun `GPUThreadInfo inactive thread`() {
        val info = AdvancedDebugger.GPUThreadInfo(
            Triple(1, 0, 0), Triple(31, 0, 0), 0, false
        )
        assertFalse(info.isActive)
        assertEquals(Triple(1, 0, 0), info.blockIdx)
    }
}
