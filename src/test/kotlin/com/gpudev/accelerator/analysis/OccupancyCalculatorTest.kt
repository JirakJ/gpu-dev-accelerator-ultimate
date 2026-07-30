/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.analysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OccupancyCalculatorTest {

    private val cc86 = OccupancyCalculator.PRESETS.first { it.computeCapability == "8.6" }
    private val cc80 = OccupancyCalculator.PRESETS.first { it.computeCapability == "8.0" }

    @Test
    fun `full occupancy on CC 8_6 with 256 threads and 32 regs`() {
        // 1536 threads/SM = 48 warps; 256-thread block = 8 warps → 6 blocks; regs allow 8
        val r = OccupancyCalculator.calculate(cc86, 256, 32, 0)
        assertEquals(6, r.activeBlocksPerSm)
        assertEquals(48, r.activeWarpsPerSm)
        assertEquals(1.0, r.occupancy, 1e-9)
    }

    @Test
    fun `register-limited kernel`() {
        // 64 regs * 256 threads = 16384 regs/block; 65536/16384 = 4 blocks = 32 warps of 48
        val r = OccupancyCalculator.calculate(cc86, 256, 64, 0)
        assertEquals(4, r.activeBlocksPerSm)
        assertEquals("registers", r.limitedBy)
        assertEquals(32.0 / 48.0, r.occupancy, 1e-9)
    }

    @Test
    fun `shared-memory-limited kernel`() {
        // 48 KiB per block on 100 KiB SM → 2 blocks
        val r = OccupancyCalculator.calculate(cc86, 256, 0, 48 * 1024)
        assertEquals(2, r.activeBlocksPerSm)
        assertEquals("shared memory", r.limitedBy)
    }

    @Test
    fun `block-count-limited small blocks`() {
        // 32-thread blocks on CC 8.0: warp limit would allow 64 blocks, hardware caps at 32
        val r = OccupancyCalculator.calculate(cc80, 32, 0, 0)
        assertEquals(32, r.activeBlocksPerSm)
        assertEquals("max blocks per SM", r.limitedBy)
        assertEquals(0.5, r.occupancy, 1e-9)
    }

    @Test
    fun `oversized block yields zero occupancy`() {
        val r = OccupancyCalculator.calculate(cc86, 2048, 0, 0)
        assertEquals(0.0, r.occupancy)
        assertEquals(0, r.activeBlocksPerSm)
    }

    @Test
    fun `non-multiple-of-warp threads round up`() {
        // 100 threads = 4 warps (rounded); 48/4 = 12 but block cap = 16 → 12 blocks
        val r = OccupancyCalculator.calculate(cc86, 100, 0, 0)
        assertEquals(12, r.activeBlocksPerSm)
        assertEquals(1.0, r.occupancy, 1e-9)
    }

    @Test
    fun `block needing more shared memory than SM has yields zero`() {
        val r = OccupancyCalculator.calculate(cc86, 256, 0, 200 * 1024)
        assertEquals(0, r.activeBlocksPerSm)
        assertEquals(0.0, r.occupancy)
    }
}
