package com.gpudev.accelerator.premium

import com.intellij.openapi.project.Project
import java.awt.*
import javax.swing.*
import kotlin.random.Random

class MultiGPUVisualizer(private val project: Project) {
    
    data class GPUNode(
        val id: Int,
        val name: String,
        val computeCapability: String,
        val memoryGB: Int,
        val utilization: Double,
        val temperature: Int,
        val powerUsageW: Int
    )
    
    data class GPUTopology(
        val nodes: List<GPUNode>,
        val connections: List<Pair<Int, Int>>,
        val bandwidthGBps: Map<Pair<Int, Int>, Double>
    )
    
    fun visualizeTopology(): TopologyPanel? {
        val topology = detectTopology()
        return TopologyPanel(topology)
    }
    
    private fun detectTopology(): GPUTopology {
        val nodes = listOf(
            GPUNode(0, "NVIDIA RTX 4090", "8.9", 24, Random.nextDouble(0.3, 0.9), Random.nextInt(40, 80), Random.nextInt(200, 450)),
            GPUNode(1, "NVIDIA RTX 4090", "8.9", 24, Random.nextDouble(0.3, 0.9), Random.nextInt(40, 80), Random.nextInt(200, 450)),
            GPUNode(2, "NVIDIA RTX 4090", "8.9", 24, Random.nextDouble(0.3, 0.9), Random.nextInt(40, 80), Random.nextInt(200, 450)),
            GPUNode(3, "NVIDIA RTX 4090", "8.9", 24, Random.nextDouble(0.3, 0.9), Random.nextInt(40, 80), Random.nextInt(200, 450))
        )
        
        val connections = listOf(
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0),
            Pair(0, 2), Pair(1, 3)
        )
        
        val bandwidth = connections.associateWith { Random.nextDouble(300.0, 600.0) }
        
        return GPUTopology(nodes, connections, bandwidth)
    }
    
    class TopologyPanel(private val topology: GPUTopology) : JPanel() {
        private val nodePositions = mutableMapOf<Int, Point>()
        
        init {
            preferredSize = Dimension(800, 600)
            calculateNodePositions()
        }
        
        private fun calculateNodePositions() {
            val centerX = 400
            val centerY = 300
            val radius = 200
            
            topology.nodes.forEachIndexed { index, node ->
                val angle = 2 * Math.PI * index / topology.nodes.size
                val x = centerX + (radius * Math.cos(angle)).toInt()
                val y = centerY + (radius * Math.sin(angle)).toInt()
                nodePositions[node.id] = Point(x, y)
            }
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            g2d.stroke = BasicStroke(2f)
            topology.connections.forEach { (from, to) ->
                val fromPos = nodePositions[from]!!
                val toPos = nodePositions[to]!!
                val bandwidth = topology.bandwidthGBps[Pair(from, to)] ?: 0.0
                
                g2d.color = if (bandwidth > 500) Color.GREEN else if (bandwidth > 400) Color.ORANGE else Color.RED
                g2d.drawLine(fromPos.x, fromPos.y, toPos.x, toPos.y)
                
                val midX = (fromPos.x + toPos.x) / 2
                val midY = (fromPos.y + toPos.y) / 2
                g2d.drawString("${bandwidth.toInt()} GB/s", midX, midY)
            }
            
            topology.nodes.forEach { node ->
                val pos = nodePositions[node.id]!!
                val nodeSize = 80
                
                val utilColor = when {
                    node.utilization > 0.8 -> Color(255, 100, 100)
                    node.utilization > 0.5 -> Color(255, 200, 100)
                    else -> Color(100, 255, 100)
                }
                
                g2d.color = utilColor
                g2d.fillOval(pos.x - nodeSize / 2, pos.y - nodeSize / 2, nodeSize, nodeSize)
                g2d.color = Color.BLACK
                g2d.drawOval(pos.x - nodeSize / 2, pos.y - nodeSize / 2, nodeSize, nodeSize)
                
                g2d.color = Color.BLACK
                g2d.drawString("GPU ${node.id}", pos.x - 20, pos.y - 5)
                g2d.drawString("${(node.utilization * 100).toInt()}%", pos.x - 15, pos.y + 10)
                g2d.drawString("${node.temperature}°C", pos.x - 15, pos.y + 25)
            }
        }
    }
    
    fun optimizeWorkloadDistribution(workload: Int): Map<Int, Int> {
        val gpuCount = 4
        val distributionMap = mutableMapOf<Int, Int>()
        
        val baseLoad = workload / gpuCount
        topology.nodes.forEach { node ->
            val adjustedLoad = (baseLoad * (1.0 - node.utilization)).toInt()
            distributionMap[node.id] = adjustedLoad
        }
        
        return distributionMap
    }
    
    private val topology: GPUTopology by lazy { detectTopology() }
}
