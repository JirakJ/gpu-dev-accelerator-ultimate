package com.gpudev.accelerator.cuda

import com.gpudev.accelerator.common.CLikeLexer

/**
 * Lexer for CUDA C/C++ shading language.
 * Tokenizes CUDA source code including execution space qualifiers,
 * kernel launch syntax, built-in variables, and CUDA API functions.
 */
class CudaLexer : CLikeLexer(
    tokens = CudaTokenTypes,
    keywords = KEYWORDS,
    typeKeywords = TYPE_KEYWORDS,
    builtinFunctions = BUILTIN_FUNCTIONS,
    qualifiers = QUALIFIERS
) {
    companion object {
        private val KEYWORDS = setOf(
            // C/C++ keywords
            "auto", "break", "case", "catch", "class", "const", "constexpr",
            "continue", "default", "delete", "do", "else", "enum", "explicit",
            "extern", "false", "for", "friend", "goto", "if", "inline",
            "namespace", "new", "noexcept", "nullptr", "operator", "private",
            "protected", "public", "register", "return", "signed", "sizeof",
            "static", "static_cast", "struct", "switch", "template", "this",
            "throw", "true", "try", "typedef", "typeid", "typename",
            "union", "unsigned", "using", "virtual", "void", "volatile", "while",
            // CUDA-specific keywords
            "launch_bounds", "noinline"
        )

        private val TYPE_KEYWORDS = setOf(
            // Standard C/C++ types
            "bool", "char", "short", "int", "long", "float", "double",
            "int8_t", "int16_t", "int32_t", "int64_t",
            "uint8_t", "uint16_t", "uint32_t", "uint64_t",
            "size_t", "ptrdiff_t",
            // CUDA types
            "dim3", "cudaStream_t", "cudaEvent_t", "cudaError_t",
            "cudaDeviceProp", "cudaMemcpyKind",
            // CUDA vector types
            "char1", "char2", "char3", "char4",
            "uchar1", "uchar2", "uchar3", "uchar4",
            "short1", "short2", "short3", "short4",
            "ushort1", "ushort2", "ushort3", "ushort4",
            "int1", "int2", "int3", "int4",
            "uint1", "uint2", "uint3", "uint4",
            "long1", "long2", "long3", "long4",
            "ulong1", "ulong2", "ulong3", "ulong4",
            "float1", "float2", "float3", "float4",
            "double1", "double2", "double3", "double4",
            "longlong1", "longlong2",
            "ulonglong1", "ulonglong2",
            // Memory copy enums
            "cudaMemcpyHostToHost", "cudaMemcpyHostToDevice",
            "cudaMemcpyDeviceToHost", "cudaMemcpyDeviceToDevice"
        )

        private val BUILTIN_FUNCTIONS = setOf(
            // Built-in variables
            "threadIdx", "blockIdx", "blockDim", "gridDim", "warpSize",
            // Memory management
            "cudaMalloc", "cudaFree", "cudaMemcpy", "cudaMemcpyAsync",
            "cudaMallocManaged", "cudaMallocHost", "cudaFreeHost",
            "cudaMemset", "cudaMemsetAsync",
            "cudaMalloc3D", "cudaMalloc3DArray",
            // Device management
            "cudaGetDeviceProperties", "cudaSetDevice", "cudaGetDevice",
            "cudaGetDeviceCount", "cudaDeviceReset",
            // Synchronization
            "cudaDeviceSynchronize", "cudaStreamSynchronize", "cudaEventSynchronize",
            "__syncthreads", "__syncwarp", "__threadfence", "__threadfence_block",
            "__threadfence_system",
            // Stream management
            "cudaStreamCreate", "cudaStreamDestroy", "cudaStreamWaitEvent",
            // Event management
            "cudaEventCreate", "cudaEventDestroy", "cudaEventRecord",
            "cudaEventElapsedTime",
            // Error handling
            "cudaGetLastError", "cudaGetErrorString", "cudaPeekAtLastError",
            // Atomic functions
            "atomicAdd", "atomicSub", "atomicExch", "atomicMin", "atomicMax",
            "atomicInc", "atomicDec", "atomicCAS", "atomicAnd", "atomicOr", "atomicXor",
            // Math functions
            "sqrtf", "rsqrtf", "sinf", "cosf", "tanf", "expf", "logf",
            "powf", "fabsf", "fminf", "fmaxf", "ceilf", "floorf",
            "__expf", "__logf", "__sinf", "__cosf", "__powf",
            // Warp-level primitives
            "__shfl_sync", "__shfl_up_sync", "__shfl_down_sync", "__shfl_xor_sync",
            "__ballot_sync", "__any_sync", "__all_sync",
            // Texture/surface
            "tex1Dfetch", "tex2D", "tex3D", "surf2Dwrite", "surf2Dread",
            // Printf
            "printf"
        )

        private val QUALIFIERS = setOf(
            "__global__", "__device__", "__host__", "__shared__",
            "__constant__", "__managed__", "__restrict__",
            "__noinline__", "__forceinline__",
            "__launch_bounds__", "__grid_constant__"
        )
    }
}
