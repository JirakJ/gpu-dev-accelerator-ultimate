/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.common

/**
 * Curated API reference shared by code completion and quick documentation.
 */
data class ApiEntry(
    val name: String,
    /** Signature or type shown as completion tail text and doc header. */
    val signature: String,
    val doc: String
)

object GpuApiCatalog {

    val cuda: List<ApiEntry> = listOf(
        // Execution-space qualifiers
        ApiEntry("__global__", "execution space", "Declares a kernel: runs on the device, callable from the host via <<<grid, block>>>."),
        ApiEntry("__device__", "execution space", "Runs on the device, callable only from device code."),
        ApiEntry("__host__", "execution space", "Runs on the host. Combine with __device__ to compile for both."),
        ApiEntry("__shared__", "memory space", "Places a variable in on-chip shared memory, visible to all threads of a block."),
        ApiEntry("__constant__", "memory space", "Places a variable in constant memory — cached, read-only from kernels."),
        ApiEntry("__restrict__", "qualifier", "Promises pointers don't alias, enabling reordering and read-only cache loads."),
        ApiEntry("__managed__", "memory space", "Unified memory accessible from both host and device."),
        // Built-in variables
        ApiEntry("threadIdx", "uint3", "Thread index within its block (threadIdx.x/y/z)."),
        ApiEntry("blockIdx", "uint3", "Block index within the grid."),
        ApiEntry("blockDim", "dim3", "Threads per block in each dimension."),
        ApiEntry("gridDim", "dim3", "Blocks per grid in each dimension."),
        ApiEntry("warpSize", "int", "Threads per warp — 32 on all current architectures."),
        // Memory management
        ApiEntry("cudaMalloc", "cudaError_t cudaMalloc(void** devPtr, size_t size)", "Allocates device global memory. Pair with cudaFree."),
        ApiEntry(
            "cudaMallocManaged",
            "cudaError_t cudaMallocManaged(void** devPtr, size_t size, unsigned flags = cudaMemAttachGlobal)",
            "Allocates unified memory, migrated on demand between host and device."
        ),
        ApiEntry(
            "cudaMallocHost",
            "cudaError_t cudaMallocHost(void** ptr, size_t size)",
            "Allocates page-locked host memory for fast async transfers. Pair with cudaFreeHost."
        ),
        ApiEntry("cudaFree", "cudaError_t cudaFree(void* devPtr)", "Releases memory from cudaMalloc/cudaMallocManaged."),
        ApiEntry("cudaFreeHost", "cudaError_t cudaFreeHost(void* ptr)", "Releases pinned host memory from cudaMallocHost."),
        ApiEntry(
            "cudaMemcpy",
            "cudaError_t cudaMemcpy(void* dst, const void* src, size_t count, cudaMemcpyKind kind)",
            "Synchronous copy; kind = cudaMemcpyHostToDevice, DeviceToHost, DeviceToDevice."
        ),
        ApiEntry(
            "cudaMemcpyAsync",
            "cudaError_t cudaMemcpyAsync(void* dst, const void* src, size_t count, cudaMemcpyKind kind, cudaStream_t stream = 0)",
            "Asynchronous copy on a stream; requires pinned host memory to overlap."
        ),
        ApiEntry("cudaMemset", "cudaError_t cudaMemset(void* devPtr, int value, size_t count)", "Fills device memory with a byte value."),
        // Synchronization & errors
        ApiEntry(
            "cudaDeviceSynchronize",
            "cudaError_t cudaDeviceSynchronize(void)",
            "Blocks the host until all device work completes. Expensive — avoid in loops."
        ),
        ApiEntry(
            "cudaGetLastError",
            "cudaError_t cudaGetLastError(void)",
            "Returns and clears the sticky error — check after kernel launches."
        ),
        ApiEntry("cudaPeekAtLastError", "cudaError_t cudaPeekAtLastError(void)", "Returns the sticky error without clearing it."),
        ApiEntry("cudaGetErrorString", "const char* cudaGetErrorString(cudaError_t error)", "Human-readable message for an error code."),
        ApiEntry(
            "__syncthreads",
            "void __syncthreads(void)",
            "Barrier for all threads of a block. Every thread must reach it — divergent calls deadlock."
        ),
        ApiEntry("__syncwarp", "void __syncwarp(unsigned mask = 0xffffffff)", "Synchronizes threads of a warp under the given mask."),
        ApiEntry("__threadfence", "void __threadfence(void)", "Orders global memory writes as observed by the whole device."),
        ApiEntry("__threadfence_block", "void __threadfence_block(void)", "Orders memory writes as observed within the block."),
        // Streams & events
        ApiEntry(
            "cudaStreamCreate",
            "cudaError_t cudaStreamCreate(cudaStream_t* stream)",
            "Creates an async work queue; independent streams may overlap."
        ),
        ApiEntry("cudaStreamDestroy", "cudaError_t cudaStreamDestroy(cudaStream_t stream)", "Destroys a stream after its work drains."),
        ApiEntry(
            "cudaStreamSynchronize",
            "cudaError_t cudaStreamSynchronize(cudaStream_t stream)",
            "Blocks the host until the stream's work completes."
        ),
        ApiEntry(
            "cudaEventCreate",
            "cudaError_t cudaEventCreate(cudaEvent_t* event)",
            "Creates an event for timing or cross-stream dependencies."
        ),
        ApiEntry(
            "cudaEventRecord",
            "cudaError_t cudaEventRecord(cudaEvent_t event, cudaStream_t stream = 0)",
            "Enqueues an event marker into a stream."
        ),
        ApiEntry(
            "cudaEventElapsedTime",
            "cudaError_t cudaEventElapsedTime(float* ms, cudaEvent_t start, cudaEvent_t end)",
            "Milliseconds between two recorded events — the standard kernel timing tool."
        ),
        ApiEntry(
            "cudaEventSynchronize",
            "cudaError_t cudaEventSynchronize(cudaEvent_t event)",
            "Blocks the host until the event completes."
        ),
        // Device management
        ApiEntry("cudaGetDeviceCount", "cudaError_t cudaGetDeviceCount(int* count)", "Number of CUDA devices present."),
        ApiEntry("cudaSetDevice", "cudaError_t cudaSetDevice(int device)", "Selects the device for subsequent calls on this host thread."),
        ApiEntry(
            "cudaGetDeviceProperties",
            "cudaError_t cudaGetDeviceProperties(cudaDeviceProp* prop, int device)",
            "Fills a cudaDeviceProp with SM count, memory size, compute capability, limits."
        ),
        // Atomics
        ApiEntry(
            "atomicAdd",
            "T atomicAdd(T* address, T val)",
            "Atomic read-modify-write add; returns the old value. Contended atomics serialize."
        ),
        ApiEntry("atomicSub", "T atomicSub(T* address, T val)", "Atomic subtraction; returns the old value."),
        ApiEntry("atomicMax", "T atomicMax(T* address, T val)", "Atomic maximum; returns the old value."),
        ApiEntry("atomicMin", "T atomicMin(T* address, T val)", "Atomic minimum; returns the old value."),
        ApiEntry("atomicExch", "T atomicExch(T* address, T val)", "Atomic exchange; returns the old value."),
        ApiEntry(
            "atomicCAS",
            "T atomicCAS(T* address, T compare, T val)",
            "Atomic compare-and-swap — the building block for custom atomics."
        ),
        // Warp primitives
        ApiEntry(
            "__shfl_sync",
            "T __shfl_sync(unsigned mask, T var, int srcLane, int width = warpSize)",
            "Reads a register from another lane of the warp — no shared memory needed."
        ),
        ApiEntry(
            "__shfl_down_sync",
            "T __shfl_down_sync(unsigned mask, T var, unsigned delta, int width = warpSize)",
            "Shift values down the warp — the core of warp-level reductions."
        ),
        ApiEntry(
            "__shfl_up_sync",
            "T __shfl_up_sync(unsigned mask, T var, unsigned delta, int width = warpSize)",
            "Shift values up the warp (prefix-scan style)."
        ),
        ApiEntry("__ballot_sync", "unsigned __ballot_sync(unsigned mask, int predicate)", "Bitmask of lanes whose predicate is non-zero."),
        ApiEntry("__any_sync", "int __any_sync(unsigned mask, int predicate)", "True if any lane's predicate is non-zero."),
        ApiEntry("__all_sync", "int __all_sync(unsigned mask, int predicate)", "True if every lane's predicate is non-zero."),
        // Math intrinsics
        ApiEntry("__fdividef", "float __fdividef(float a, float b)", "Fast approximate float division."),
        ApiEntry("__expf", "float __expf(float x)", "Fast approximate exponential (device intrinsic)."),
        ApiEntry("__logf", "float __logf(float x)", "Fast approximate natural log (device intrinsic)."),
        ApiEntry("__sinf", "float __sinf(float x)", "Fast approximate sine (device intrinsic)."),
        ApiEntry("__cosf", "float __cosf(float x)", "Fast approximate cosine (device intrinsic)."),
        ApiEntry("rsqrtf", "float rsqrtf(float x)", "Reciprocal square root — single instruction on the SFU."),
        ApiEntry("fmaf", "float fmaf(float x, float y, float z)", "Fused multiply-add: x*y+z in one instruction, one rounding.")
    )

    val metal: List<ApiEntry> = listOf(
        ApiEntry("kernel", "function qualifier", "Declares a compute function dispatched from the host via MTLComputeCommandEncoder."),
        ApiEntry("vertex", "function qualifier", "Declares a vertex shader function."),
        ApiEntry("fragment", "function qualifier", "Declares a fragment shader function."),
        ApiEntry("device", "address space", "Read-write memory in the device address space (buffers)."),
        ApiEntry("constant", "address space", "Read-only device memory, cached and preloaded — for uniforms."),
        ApiEntry("threadgroup", "address space", "On-chip memory shared by a threadgroup — Metal's __shared__."),
        ApiEntry("thread", "address space", "Per-thread private memory (default for locals)."),
        ApiEntry(
            "thread_position_in_grid",
            "attribute",
            "[[thread_position_in_grid]] — global thread coordinates; CUDA's blockIdx*blockDim+threadIdx."
        ),
        ApiEntry(
            "thread_position_in_threadgroup",
            "attribute",
            "[[thread_position_in_threadgroup]] — local coordinates; CUDA's threadIdx."
        ),
        ApiEntry(
            "threadgroup_position_in_grid",
            "attribute",
            "[[threadgroup_position_in_grid]] — threadgroup coordinates; CUDA's blockIdx."
        ),
        ApiEntry("threads_per_threadgroup", "attribute", "[[threads_per_threadgroup]] — threadgroup size; CUDA's blockDim."),
        ApiEntry("threads_per_grid", "attribute", "[[threads_per_grid]] — total grid size in threads."),
        ApiEntry("thread_index_in_simdgroup", "attribute", "[[thread_index_in_simdgroup]] — lane index within the SIMD-group (warp)."),
        ApiEntry(
            "simdgroup_index_in_threadgroup",
            "attribute",
            "[[simdgroup_index_in_threadgroup]] — SIMD-group index within the threadgroup."
        ),
        ApiEntry("buffer", "argument attribute", "[[buffer(n)]] — binds a device/constant pointer argument to buffer slot n."),
        ApiEntry("texture", "argument attribute", "[[texture(n)]] — binds a texture argument to texture slot n."),
        ApiEntry("sampler", "argument attribute", "[[sampler(n)]] — binds a sampler argument to sampler slot n."),
        ApiEntry(
            "threadgroup_barrier",
            "void threadgroup_barrier(mem_flags flags)",
            "Barrier for the threadgroup; mem_flags::mem_threadgroup orders threadgroup memory — Metal's __syncthreads()."
        ),
        ApiEntry("simdgroup_barrier", "void simdgroup_barrier(mem_flags flags)", "Barrier within a SIMD-group."),
        ApiEntry(
            "simd_shuffle_down",
            "T simd_shuffle_down(T value, ushort delta)",
            "Reads a value from a higher lane — SIMD-group reductions."
        ),
        ApiEntry("simd_sum", "T simd_sum(T value)", "Sum across the SIMD-group in one call."),
        ApiEntry("simd_ballot", "simd_vote simd_ballot(bool expr)", "Bitmask of lanes where expr is true."),
        ApiEntry(
            "atomic_fetch_add_explicit",
            "T atomic_fetch_add_explicit(atomic<T>* obj, T operand, memory_order order)",
            "Atomic add on atomic_int/atomic_uint; use memory_order_relaxed unless ordering matters."
        ),
        ApiEntry("atomic_load_explicit", "T atomic_load_explicit(atomic<T>* obj, memory_order order)", "Atomic read."),
        ApiEntry("atomic_store_explicit", "void atomic_store_explicit(atomic<T>* obj, T value, memory_order order)", "Atomic write."),
        ApiEntry("float4", "vector type", "4-component float vector; also float2/float3, half4, int4, uint4…"),
        ApiEntry("half", "scalar type", "16-bit float — half bandwidth and often double the ALU rate of float on Apple GPUs."),
        ApiEntry("mix", "T mix(T x, T y, T a)", "Linear interpolation x*(1-a) + y*a."),
        ApiEntry("clamp", "T clamp(T x, T minval, T maxval)", "Clamps x into [minval, maxval]."),
        ApiEntry("saturate", "T saturate(T x)", "Clamps into [0, 1] — free on most GPU ALUs."),
        ApiEntry("fma", "T fma(T a, T b, T c)", "Fused multiply-add in one instruction."),
        ApiEntry("rsqrt", "T rsqrt(T x)", "Fast reciprocal square root.")
    )

    val opencl: List<ApiEntry> = listOf(
        ApiEntry("__kernel", "function qualifier", "Declares a kernel entry point enqueued via clEnqueueNDRangeKernel."),
        ApiEntry("__global", "address space", "Global device memory (buffers)."),
        ApiEntry("__local", "address space", "On-chip memory shared by a work-group — CUDA's __shared__."),
        ApiEntry("__constant", "address space", "Read-only cached memory for uniforms."),
        ApiEntry("__private", "address space", "Per-work-item private memory (default)."),
        ApiEntry("get_global_id", "size_t get_global_id(uint dim)", "Global work-item index — CUDA's blockIdx*blockDim+threadIdx."),
        ApiEntry("get_local_id", "size_t get_local_id(uint dim)", "Index within the work-group — CUDA's threadIdx."),
        ApiEntry("get_group_id", "size_t get_group_id(uint dim)", "Work-group index — CUDA's blockIdx."),
        ApiEntry("get_local_size", "size_t get_local_size(uint dim)", "Work-group size — CUDA's blockDim."),
        ApiEntry("get_global_size", "size_t get_global_size(uint dim)", "Total work-items in the given dimension."),
        ApiEntry("get_num_groups", "size_t get_num_groups(uint dim)", "Number of work-groups — CUDA's gridDim."),
        ApiEntry(
            "barrier",
            "void barrier(cl_mem_fence_flags flags)",
            "Work-group barrier; CLK_LOCAL_MEM_FENCE orders local memory — CUDA's __syncthreads()."
        ),
        ApiEntry("work_group_barrier", "void work_group_barrier(cl_mem_fence_flags flags)", "OpenCL 2.0 name for barrier()."),
        ApiEntry("mem_fence", "void mem_fence(cl_mem_fence_flags flags)", "Orders memory operations of the calling work-item."),
        ApiEntry("atomic_add", "T atomic_add(volatile __global T* p, T val)", "Atomic add; returns the old value."),
        ApiEntry("atomic_inc", "T atomic_inc(volatile __global T* p)", "Atomic increment; returns the old value."),
        ApiEntry("atomic_cmpxchg", "T atomic_cmpxchg(volatile __global T* p, T cmp, T val)", "Atomic compare-and-swap."),
        ApiEntry("vload4", "T4 vload4(size_t offset, const T* p)", "Vector load of 4 elements — helps bandwidth on scalar architectures."),
        ApiEntry("vstore4", "void vstore4(T4 data, size_t offset, T* p)", "Vector store of 4 elements."),
        ApiEntry("mad", "T mad(T a, T b, T c)", "a*b+c, allowed to use a fused instruction."),
        ApiEntry("native_divide", "T native_divide(T x, T y)", "Fast approximate division."),
        ApiEntry("native_rsqrt", "T native_rsqrt(T x)", "Fast approximate reciprocal square root."),
        ApiEntry("clamp", "T clamp(T x, T minval, T maxval)", "Clamps x into [minval, maxval]."),
        ApiEntry("mix", "T mix(T x, T y, T a)", "Linear interpolation."),
        ApiEntry("float4", "vector type", "4-component float vector; also float2/float8/float16, int4, uint4…"),
        ApiEntry("CLK_LOCAL_MEM_FENCE", "cl_mem_fence_flags", "Barrier flag ordering __local memory."),
        ApiEntry("CLK_GLOBAL_MEM_FENCE", "cl_mem_fence_flags", "Barrier flag ordering __global memory.")
    )

    private val byLanguage = mapOf("CUDA" to cuda, "Metal" to metal, "OpenCL" to opencl)

    fun forLanguage(languageId: String): List<ApiEntry> = byLanguage[languageId] ?: emptyList()

    fun lookup(languageId: String, word: String): ApiEntry? =
        forLanguage(languageId).firstOrNull { it.name == word }
}
