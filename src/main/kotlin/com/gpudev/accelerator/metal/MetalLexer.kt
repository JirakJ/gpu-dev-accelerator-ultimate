package com.gpudev.accelerator.metal

import com.gpudev.accelerator.common.CLikeLexer

/**
 * Lexer for Metal Shading Language (MSL).
 * Tokenizes Metal source code including function qualifiers,
 * address-space attributes, built-in types, and MSL-specific keywords.
 */
class MetalLexer : CLikeLexer(
    tokens = MetalTokenTypes,
    keywords = KEYWORDS,
    typeKeywords = TYPE_KEYWORDS,
    builtinFunctions = BUILTIN_FUNCTIONS,
    qualifiers = QUALIFIERS
) {
    companion object {
        private val KEYWORDS = setOf(
            // C++ keywords used in Metal
            "auto", "break", "case", "catch", "class", "const", "constexpr",
            "continue", "default", "delete", "do", "else", "enum", "explicit",
            "extern", "false", "for", "friend", "goto", "if", "inline",
            "namespace", "new", "noexcept", "nullptr", "operator", "private",
            "protected", "public", "register", "return", "signed", "sizeof",
            "static", "static_cast", "struct", "switch", "template", "this",
            "throw", "true", "try", "typedef", "typeid", "typename",
            "union", "unsigned", "using", "virtual", "void", "volatile", "while",
            // Metal-specific keywords
            "discard_fragment"
        )

        private val TYPE_KEYWORDS = setOf(
            // Scalar types
            "bool", "char", "short", "int", "long", "float", "double",
            "half", "uint", "ushort", "uchar", "ulong",
            "size_t", "ptrdiff_t",
            // Vector types
            "bool2", "bool3", "bool4",
            "char2", "char3", "char4",
            "uchar2", "uchar3", "uchar4",
            "short2", "short3", "short4",
            "ushort2", "ushort3", "ushort4",
            "int2", "int3", "int4",
            "uint2", "uint3", "uint4",
            "half2", "half3", "half4",
            "float2", "float3", "float4",
            "float2x2", "float3x3", "float4x4",
            "float2x3", "float2x4", "float3x2", "float3x4", "float4x2", "float4x3",
            "half2x2", "half3x3", "half4x4",
            // Texture types
            "texture1d", "texture1d_array",
            "texture2d", "texture2d_array", "texture2d_ms", "texture2d_ms_array",
            "texture3d", "texturecube", "texturecube_array",
            "depth2d", "depth2d_array", "depth2d_ms", "depth2d_ms_array",
            "depthcube", "depthcube_array",
            // Sampler and other
            "sampler",
            // Atomic types
            "atomic_int", "atomic_uint", "atomic_bool"
        )

        private val BUILTIN_FUNCTIONS = setOf(
            // Math functions
            "abs", "acos", "asin", "atan", "atan2", "ceil", "clamp", "cos",
            "cross", "degrees", "determinant", "distance", "dot", "exp",
            "exp2", "fabs", "floor", "fma", "fmax", "fmin", "fmod",
            "fract", "length", "log", "log2", "max", "min", "mix",
            "normalize", "pow", "radians", "reflect", "refract", "round",
            "rsqrt", "saturate", "sign", "sin", "smoothstep", "sqrt",
            "step", "tan", "transpose",
            // Texture functions
            "sample", "read", "write", "gather",
            "get_width", "get_height", "get_depth",
            "get_num_mip_levels", "get_num_samples",
            // Atomic functions
            "atomic_fetch_add_explicit", "atomic_fetch_sub_explicit",
            "atomic_fetch_min_explicit", "atomic_fetch_max_explicit",
            "atomic_fetch_and_explicit", "atomic_fetch_or_explicit",
            "atomic_fetch_xor_explicit", "atomic_exchange_explicit",
            "atomic_compare_exchange_weak_explicit",
            "atomic_load_explicit", "atomic_store_explicit",
            // Synchronization
            "threadgroup_barrier", "simdgroup_barrier",
            // SIMD functions
            "simd_sum", "simd_product", "simd_min", "simd_max",
            "simd_prefix_inclusive_sum", "simd_prefix_exclusive_sum",
            "simd_shuffle", "simd_shuffle_up", "simd_shuffle_down",
            "simd_shuffle_xor", "simd_broadcast",
            // Memory order
            "mem_flags"
        )

        private val QUALIFIERS = setOf(
            "kernel", "vertex", "fragment", "compute",
            "device", "constant", "threadgroup", "thread",
            "threadgroup_imageblock",
            // Attribute qualifiers
            "thread_position_in_grid", "thread_position_in_threadgroup",
            "threadgroup_position_in_grid", "threads_per_threadgroup",
            "thread_index_in_threadgroup", "threads_per_simdgroup",
            "simdgroup_index_in_threadgroup", "thread_index_in_simdgroup",
            "dispatch_threads_per_threadgroup",
            "stage_in", "position", "point_size",
            "color", "depth", "stencil",
            "access",
            "constexpr"
        )
    }
}
