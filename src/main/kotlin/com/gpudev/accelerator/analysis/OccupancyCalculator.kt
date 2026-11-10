/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.analysis

/**
 * Theoretical SM occupancy calculator — same model as NVIDIA's occupancy
 * spreadsheet, at warp granularity.
 */
object OccupancyCalculator {

    data class DeviceSpec(
        val label: String,
        val computeCapability: String,
        val maxThreadsPerSm: Int,
        val maxBlocksPerSm: Int,
        val registersPerSm: Int,
        val sharedMemPerSmBytes: Int,
        val maxThreadsPerBlock: Int = 1024,
        val warpSize: Int = 32
    )

    val PRESETS = listOf(
        DeviceSpec("Volta (V100) — CC 7.0", "7.0", 2048, 32, 65536, 96 * 1024),
        DeviceSpec("Turing (RTX 20xx, T4) — CC 7.5", "7.5", 1024, 16, 65536, 64 * 1024),
        DeviceSpec("Ampere (A100) — CC 8.0", "8.0", 2048, 32, 65536, 164 * 1024),
        DeviceSpec("Ampere (RTX 30xx) — CC 8.6", "8.6", 1536, 16, 65536, 100 * 1024),
        DeviceSpec("Ada (RTX 40xx) — CC 8.9", "8.9", 1536, 24, 65536, 100 * 1024),
        DeviceSpec("Hopper (H100) — CC 9.0", "9.0", 2048, 32, 65536, 228 * 1024)
    )

    data class Result(
        val activeBlocksPerSm: Int,
        val activeWarpsPerSm: Int,
        val maxWarpsPerSm: Int,
        val occupancy: Double,
        val limitedBy: String
    )

    fun calculate(
        device: DeviceSpec,
        threadsPerBlock: Int,
        registersPerThread: Int,
        sharedMemPerBlockBytes: Int
    ): Result {
        require(threadsPerBlock > 0) { "threadsPerBlock must be positive" }
        val maxWarpsPerSm = device.maxThreadsPerSm / device.warpSize

        if (threadsPerBlock > device.maxThreadsPerBlock) {
            return Result(0, 0, maxWarpsPerSm, 0.0, "threads per block exceeds device limit (${device.maxThreadsPerBlock})")
        }

        val warpsPerBlock = (threadsPerBlock + device.warpSize - 1) / device.warpSize
        val limits = mutableMapOf<String, Int>()
        limits["max warps per SM"] = maxWarpsPerSm / warpsPerBlock
        limits["max blocks per SM"] = device.maxBlocksPerSm
        if (registersPerThread > 0) {
            // ponytail: ignores per-arch register allocation granularity; real HW rounds up per warp
            val regsPerBlock = registersPerThread * warpsPerBlock * device.warpSize
            limits["registers"] = if (regsPerBlock > device.registersPerSm) 0 else device.registersPerSm / regsPerBlock
        }
        if (sharedMemPerBlockBytes > 0) {
            limits["shared memory"] =
                if (sharedMemPerBlockBytes > device.sharedMemPerSmBytes) 0
                else device.sharedMemPerSmBytes / sharedMemPerBlockBytes
        }

        val limiting = limits.minByOrNull { it.value }!!
        val activeBlocks = limiting.value
        val activeWarps = activeBlocks * warpsPerBlock
        return Result(
            activeBlocksPerSm = activeBlocks,
            activeWarpsPerSm = activeWarps,
            maxWarpsPerSm = maxWarpsPerSm,
            occupancy = activeWarps.toDouble() / maxWarpsPerSm,
            limitedBy = limiting.key
        )
    }
}
