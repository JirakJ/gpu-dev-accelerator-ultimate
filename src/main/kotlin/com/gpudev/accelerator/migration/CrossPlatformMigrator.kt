package com.gpudev.accelerator.migration

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

class CrossPlatformMigrator {
    
    enum class GPUPlatform {
        CUDA, METAL, OPENCL, VULKAN, HIP, SYCL
    }
    
    data class MigrationResult(
        val success: Boolean,
        val generatedCode: String,
        val warnings: List<String>,
        val performanceNotes: List<String>,
        val apiMappings: Map<String, String>
    )
    
    fun migrateKernel(
        sourceFile: PsiFile,
        sourcePlatform: GPUPlatform,
        targetPlatform: GPUPlatform
    ): MigrationResult {
        val sourceCode = sourceFile.text
        val migrations = when (sourcePlatform to targetPlatform) {
            GPUPlatform.CUDA to GPUPlatform.METAL -> cudaToMetal(sourceCode)
            GPUPlatform.CUDA to GPUPlatform.OPENCL -> cudaToOpenCL(sourceCode)
            GPUPlatform.CUDA to GPUPlatform.HIP -> cudaToHIP(sourceCode)
            GPUPlatform.METAL to GPUPlatform.CUDA -> metalToCuda(sourceCode)
            GPUPlatform.OPENCL to GPUPlatform.CUDA -> openCLToCuda(sourceCode)
            GPUPlatform.CUDA to GPUPlatform.VULKAN -> cudaToVulkan()
            else -> MigrationResult(
                false,
                "// Unsupported migration path",
                listOf("Migration from $sourcePlatform to $targetPlatform not yet supported"),
                emptyList(),
                emptyMap()
            )
        }
        
        return migrations
    }
    
    private fun cudaToMetal(cudaCode: String): MigrationResult {
        val warnings = mutableListOf<String>()
        val performanceNotes = mutableListOf<String>()
        val mappings = mutableMapOf<String, String>()
        
        var metalCode = cudaCode
        
        // Thread indexing
        metalCode = metalCode.replace(Regex("threadIdx\\.x"), "thread_position_in_threadgroup.x")
        metalCode = metalCode.replace(Regex("threadIdx\\.y"), "thread_position_in_threadgroup.y")
        metalCode = metalCode.replace(Regex("threadIdx\\.z"), "thread_position_in_threadgroup.z")
        mappings["threadIdx"] = "thread_position_in_threadgroup"
        
        metalCode = metalCode.replace(Regex("blockIdx\\.x"), "threadgroup_position_in_grid.x")
        metalCode = metalCode.replace(Regex("blockIdx\\.y"), "threadgroup_position_in_grid.y")
        metalCode = metalCode.replace(Regex("blockIdx\\.z"), "threadgroup_position_in_grid.z")
        mappings["blockIdx"] = "threadgroup_position_in_grid"
        
        metalCode = metalCode.replace(Regex("blockDim\\.x"), "threads_per_threadgroup.x")
        metalCode = metalCode.replace(Regex("blockDim\\.y"), "threads_per_threadgroup.y")
        metalCode = metalCode.replace(Regex("blockDim\\.z"), "threads_per_threadgroup.z")
        mappings["blockDim"] = "threads_per_threadgroup"
        
        // Kernel declaration
        metalCode = metalCode.replace(Regex("__global__\\s+void"), "kernel void")
        mappings["__global__"] = "kernel"
        
        // Memory qualifiers
        metalCode = metalCode.replace(Regex("__shared__"), "threadgroup")
        mappings["__shared__"] = "threadgroup"
        
        // Synchronization
        if (metalCode.contains("__syncthreads()")) {
            metalCode = metalCode.replace("__syncthreads()", "threadgroup_barrier(mem_flags::mem_threadgroup)")
            mappings["__syncthreads()"] = "threadgroup_barrier()"
            performanceNotes.add("Metal barriers may have different performance characteristics")
        }
        
        // Atomics
        metalCode = metalCode.replace(Regex("atomicAdd"), "atomic_fetch_add_explicit")
        metalCode = metalCode.replace(Regex("atomicSub"), "atomic_fetch_sub_explicit")
        mappings["atomicAdd"] = "atomic_fetch_add_explicit"
        
        // Math functions
        metalCode = metalCode.replace(Regex("rsqrtf"), "fast::rsqrt")
        metalCode = metalCode.replace(Regex("sqrtf"), "fast::sqrt")
        metalCode = metalCode.replace(Regex("expf"), "fast::exp")
        
        // Data types
        metalCode = metalCode.replace(Regex("\\bfloat4\\b"), "float4")
        metalCode = metalCode.replace(Regex("\\bfloat3\\b"), "float3")
        metalCode = metalCode.replace(Regex("\\bfloat2\\b"), "float2")
        
        // Add Metal header
        val metalHeader = """
            #include <metal_stdlib>
            using namespace metal;
            
        """.trimIndent()
        
        metalCode = metalHeader + metalCode
        
        if (cudaCode.contains("cudaMalloc") || cudaCode.contains("cudaMemcpy")) {
            warnings.add("Host-side CUDA API calls detected - manual Metal buffer management required")
        }
        
        if (cudaCode.contains("__device__")) {
            warnings.add("Device functions may need signature adjustments for Metal")
        }
        
        performanceNotes.add("Consider using Metal Performance Shaders for optimized operations")
        performanceNotes.add("Metal uses different memory hierarchy - review threadgroup memory usage")
        
        return MigrationResult(true, metalCode, warnings, performanceNotes, mappings)
    }
    
    private fun cudaToOpenCL(cudaCode: String): MigrationResult {
        val warnings = mutableListOf<String>()
        val performanceNotes = mutableListOf<String>()
        val mappings = mutableMapOf<String, String>()
        
        var openclCode = cudaCode
        
        // Thread indexing
        openclCode = openclCode.replace(Regex("threadIdx\\.x"), "get_local_id(0)")
        openclCode = openclCode.replace(Regex("threadIdx\\.y"), "get_local_id(1)")
        openclCode = openclCode.replace(Regex("threadIdx\\.z"), "get_local_id(2)")
        mappings["threadIdx"] = "get_local_id()"
        
        openclCode = openclCode.replace(Regex("blockIdx\\.x"), "get_group_id(0)")
        openclCode = openclCode.replace(Regex("blockIdx\\.y"), "get_group_id(1)")
        openclCode = openclCode.replace(Regex("blockIdx\\.z"), "get_group_id(2)")
        mappings["blockIdx"] = "get_group_id()"
        
        openclCode = openclCode.replace(Regex("blockDim\\.x"), "get_local_size(0)")
        openclCode = openclCode.replace(Regex("blockDim\\.y"), "get_local_size(1)")
        openclCode = openclCode.replace(Regex("blockDim\\.z"), "get_local_size(2)")
        mappings["blockDim"] = "get_local_size()"
        
        // Kernel declaration
        openclCode = openclCode.replace(Regex("__global__\\s+void"), "__kernel void")
        mappings["__global__"] = "__kernel"
        
        // Memory qualifiers
        openclCode = openclCode.replace(Regex("__shared__"), "__local")
        mappings["__shared__"] = "__local"
        
        openclCode = openclCode.replace(Regex("__constant__"), "__constant")
        
        // Synchronization
        openclCode = openclCode.replace("__syncthreads()", "barrier(CLK_LOCAL_MEM_FENCE)")
        mappings["__syncthreads()"] = "barrier()"
        
        // Atomics
        openclCode = openclCode.replace(Regex("atomicAdd\\((.*?),\\s*(.*?)\\)"), "atomic_add($1, $2)")
        mappings["atomicAdd"] = "atomic_add"
        
        performanceNotes.add("OpenCL may require explicit address space qualifiers")
        performanceNotes.add("Test on target hardware - OpenCL performance varies by vendor")
        
        return MigrationResult(true, openclCode, warnings, performanceNotes, mappings)
    }
    
    private fun cudaToHIP(cudaCode: String): MigrationResult {
        val mappings = mutableMapOf<String, String>()
        
        var hipCode = cudaCode
        
        // Simple prefix replacement for most CUDA APIs
        hipCode = hipCode.replace(Regex("\\bcuda([A-Z])"), "hip$1")
        mappings["cuda*"] = "hip*"
        
        // Specific mappings
        hipCode = hipCode.replace("cudaSuccess", "hipSuccess")
        hipCode = hipCode.replace("cudaError_t", "hipError_t")
        hipCode = hipCode.replace("cudaStream_t", "hipStream_t")
        hipCode = hipCode.replace("cudaEvent_t", "hipEvent_t")
        
        val performanceNotes = listOf(
            "HIP provides near 1:1 compatibility with CUDA",
            "Review AMD-specific optimizations for best performance"
        )
        
        return MigrationResult(true, hipCode, emptyList(), performanceNotes, mappings)
    }
    
    private fun metalToCuda(metalCode: String): MigrationResult {
        val warnings = mutableListOf<String>()
        val mappings = mutableMapOf<String, String>()
        
        var cudaCode = metalCode
        
        // Remove Metal header
        cudaCode = cudaCode.replace(Regex("#include <metal_stdlib>.*\n"), "")
        cudaCode = cudaCode.replace(Regex("using namespace metal;.*\n"), "")
        
        // Kernel declaration
        cudaCode = cudaCode.replace(Regex("kernel void"), "__global__ void")
        mappings["kernel"] = "__global__"
        
        // Thread indexing
        cudaCode = cudaCode.replace(Regex("thread_position_in_threadgroup\\.x"), "threadIdx.x")
        cudaCode = cudaCode.replace(Regex("threadgroup_position_in_grid\\.x"), "blockIdx.x")
        cudaCode = cudaCode.replace(Regex("threads_per_threadgroup\\.x"), "blockDim.x")
        
        // Memory
        cudaCode = cudaCode.replace(Regex("\\bthreadgroup\\b"), "__shared__")
        mappings["threadgroup"] = "__shared__"
        
        warnings.add("Manual review required for Metal-specific features")
        
        return MigrationResult(true, cudaCode, warnings, emptyList(), mappings)
    }
    
    private fun openCLToCuda(openclCode: String): MigrationResult {
        val mappings = mutableMapOf<String, String>()
        
        var cudaCode = openclCode
        
        // Kernel declaration
        cudaCode = cudaCode.replace(Regex("__kernel void"), "__global__ void")
        mappings["__kernel"] = "__global__"
        
        // Thread indexing
        cudaCode = cudaCode.replace(Regex("get_local_id\\(0\\)"), "threadIdx.x")
        cudaCode = cudaCode.replace(Regex("get_local_id\\(1\\)"), "threadIdx.y")
        cudaCode = cudaCode.replace(Regex("get_local_id\\(2\\)"), "threadIdx.z")
        
        cudaCode = cudaCode.replace(Regex("get_group_id\\(0\\)"), "blockIdx.x")
        cudaCode = cudaCode.replace(Regex("get_group_id\\(1\\)"), "blockIdx.y")
        cudaCode = cudaCode.replace(Regex("get_group_id\\(2\\)"), "blockIdx.z")
        
        // Memory
        cudaCode = cudaCode.replace(Regex("__local"), "__shared__")
        mappings["__local"] = "__shared__"
        
        // Synchronization
        cudaCode = cudaCode.replace(Regex("barrier\\(CLK_LOCAL_MEM_FENCE\\)"), "__syncthreads()")
        
        return MigrationResult(true, cudaCode, emptyList(), emptyList(), mappings)
    }
    
    private fun cudaToVulkan(): MigrationResult {
        val warnings = listOf(
            "Vulkan Compute requires significant restructuring",
            "SPIR-V generation recommended for production code",
            "Descriptor sets and pipeline configuration needed"
        )
        
        val performanceNotes = listOf(
            "Vulkan provides explicit control over GPU resources",
            "Consider using existing SPIR-V compiler tools"
        )
        
        return MigrationResult(
            false,
            "// Vulkan migration requires SPIR-V toolchain",
            warnings,
            performanceNotes,
            emptyMap()
        )
    }
    
    fun analyzePortability(file: PsiFile): PortabilityReport {
        val code = file.text
        val issues = mutableListOf<PortabilityIssue>()
        val recommendations = mutableListOf<String>()
        
        // Check for platform-specific intrinsics
        if (code.contains("__ballot") || code.contains("__shfl")) {
            issues.add(PortabilityIssue(
                "Warp-level primitives",
                "CUDA warp functions are not portable",
                Severity.HIGH
            ))
            recommendations.add("Use cooperative groups API for better portability")
        }
        
        if (code.contains("texture<")) {
            issues.add(PortabilityIssue(
                "CUDA textures",
                "Texture objects have different APIs across platforms",
                Severity.MEDIUM
            ))
        }
        
        // Check for dynamic parallelism
        if (code.contains("<<<") && code.contains(">>>") && code.contains("__global__")) {
            issues.add(PortabilityIssue(
                "Dynamic parallelism",
                "Not supported on all platforms",
                Severity.HIGH
            ))
        }
        
        val portabilityScore = calculatePortabilityScore(issues)
        
        return PortabilityReport(issues, recommendations, portabilityScore)
    }
    
    private fun calculatePortabilityScore(issues: List<PortabilityIssue>): Double {
        val highIssues = issues.count { it.severity == Severity.HIGH }
        val mediumIssues = issues.count { it.severity == Severity.MEDIUM }
        val lowIssues = issues.count { it.severity == Severity.LOW }
        
        val penalty = (highIssues * 30) + (mediumIssues * 15) + (lowIssues * 5)
        return maxOf(0.0, 100.0 - penalty)
    }
    
    data class PortabilityReport(
        val issues: List<PortabilityIssue>,
        val recommendations: List<String>,
        val score: Double
    )
    
    data class PortabilityIssue(
        val category: String,
        val description: String,
        val severity: Severity
    )
    
    enum class Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
