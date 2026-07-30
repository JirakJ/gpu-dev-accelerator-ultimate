/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.gpu

/**
 * Real GPU topology model, populated from `nvidia-smi` output.
 */
data class GpuNode(
    val index: Int,
    val name: String,
    val memoryTotalMiB: Int,
    val memoryUsedMiB: Int,
    val utilizationPercent: Int,
    val temperatureC: Int,
    val powerDrawW: Double
)

data class GpuLink(val from: Int, val to: Int, val linkType: String)

data class GpuTopology(val nodes: List<GpuNode>, val links: List<GpuLink>)

object GpuTopologyParser {

    /**
     * Parses `nvidia-smi --query-gpu=index,name,memory.total,memory.used,utilization.gpu,temperature.gpu,power.draw --format=csv,noheader,nounits`
     */
    fun parseDeviceQuery(csv: String): List<GpuNode> {
        val nodes = mutableListOf<GpuNode>()
        for (rawLine in csv.lineSequence()) {
            val parts = rawLine.split(",").map { it.trim() }
            if (parts.size < 7) continue
            val index = parts[0].toIntOrNull() ?: continue
            nodes.add(
                GpuNode(
                    index = index,
                    name = parts[1],
                    memoryTotalMiB = parts[2].toIntOrNull() ?: 0,
                    memoryUsedMiB = parts[3].toIntOrNull() ?: 0,
                    utilizationPercent = parts[4].toIntOrNull() ?: 0,
                    temperatureC = parts[5].toIntOrNull() ?: 0,
                    powerDrawW = parts[6].toDoubleOrNull() ?: 0.0
                )
            )
        }
        return nodes
    }

    /**
     * Parses the matrix printed by `nvidia-smi topo -m`. Rows/columns are GPU0..GPUn,
     * cells hold link types (X, NV1, NV2, PIX, PXB, PHB, NODE, SYS).
     */
    fun parseTopologyMatrix(matrix: String): List<GpuLink> {
        val lines = matrix.lines().filter { it.isNotBlank() }
        val headerIdx = lines.indexOfFirst { it.trimStart().startsWith("GPU0") || it.contains("\tGPU0") }
        if (headerIdx < 0) return emptyList()
        val header = lines[headerIdx].split(Regex("\\s+")).filter { it.startsWith("GPU") }
        val links = mutableListOf<GpuLink>()
        for (line in lines.drop(headerIdx + 1)) {
            val cells = line.trim().split(Regex("\\s+"))
            val rowLabel = cells.firstOrNull() ?: continue
            if (!rowLabel.matches(Regex("GPU\\d+"))) continue
            val from = rowLabel.removePrefix("GPU").toIntOrNull() ?: continue
            cells.drop(1).take(header.size).forEachIndexed { colIdx, cell ->
                val to = header.getOrNull(colIdx)?.removePrefix("GPU")?.toIntOrNull() ?: return@forEachIndexed
                if (from < to && cell != "X") {
                    links.add(GpuLink(from, to, cell))
                }
            }
        }
        return links
    }
}
