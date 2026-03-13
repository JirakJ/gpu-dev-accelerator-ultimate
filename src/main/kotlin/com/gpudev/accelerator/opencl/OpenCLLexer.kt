package com.gpudev.accelerator.opencl

import com.gpudev.accelerator.common.CLikeLexer

/**
 * Lexer for OpenCL C shading language.
 * Tokenizes OpenCL source code including address-space qualifiers,
 * kernel qualifiers, built-in functions, and vector types.
 */
class OpenCLLexer : CLikeLexer(
    tokens = OpenCLTokenTypes,
    keywords = KEYWORDS,
    typeKeywords = TYPE_KEYWORDS,
    builtinFunctions = BUILTIN_FUNCTIONS,
    qualifiers = QUALIFIERS
) {
    companion object {
        private val KEYWORDS = setOf(
            // C keywords
            "auto", "break", "case", "const", "continue", "default", "do", "else",
            "enum", "extern", "for", "goto", "if", "inline", "register", "restrict",
            "return", "signed", "sizeof", "static", "struct", "switch", "typedef",
            "union", "unsigned", "void", "volatile", "while",
            // OpenCL-specific
            "true", "false", "NULL"
        )

        private val TYPE_KEYWORDS = setOf(
            // Scalar types
            "bool", "char", "uchar", "short", "ushort", "int", "uint",
            "long", "ulong", "float", "double", "half", "size_t",
            "ptrdiff_t", "intptr_t", "uintptr_t",
            // Vector types
            "char2", "char3", "char4", "char8", "char16",
            "uchar2", "uchar3", "uchar4", "uchar8", "uchar16",
            "short2", "short3", "short4", "short8", "short16",
            "ushort2", "ushort3", "ushort4", "ushort8", "ushort16",
            "int2", "int3", "int4", "int8", "int16",
            "uint2", "uint3", "uint4", "uint8", "uint16",
            "long2", "long3", "long4", "long8", "long16",
            "ulong2", "ulong3", "ulong4", "ulong8", "ulong16",
            "float2", "float3", "float4", "float8", "float16",
            "double2", "double3", "double4", "double8", "double16",
            "half2", "half3", "half4", "half8", "half16",
            // Image and other types
            "image1d_t", "image1d_array_t", "image1d_buffer_t",
            "image2d_t", "image2d_array_t", "image3d_t",
            "sampler_t", "event_t", "cl_mem_fence_flags",
            "ndrange_t", "queue_t", "reserve_id_t"
        )

        private val BUILTIN_FUNCTIONS = setOf(
            // Work-item functions
            "get_global_id", "get_local_id", "get_group_id",
            "get_global_size", "get_local_size", "get_num_groups",
            "get_work_dim", "get_global_offset",
            // Synchronization
            "barrier", "mem_fence", "read_mem_fence", "write_mem_fence",
            // Math functions
            "acos", "asin", "atan", "atan2", "ceil", "copysign", "cos", "exp",
            "exp2", "fabs", "floor", "fma", "fmax", "fmin", "fmod",
            "log", "log2", "log10", "pow", "round", "sin", "sqrt", "tan",
            "clamp", "degrees", "max", "min", "mix", "radians", "sign",
            "step", "smoothstep",
            // Geometric functions
            "cross", "dot", "distance", "length", "normalize", "fast_distance",
            "fast_length", "fast_normalize",
            // Integer functions
            "abs", "abs_diff", "clz", "mad_hi", "mad_sat", "mul_hi", "rotate",
            // Conversion functions
            "convert_char", "convert_uchar", "convert_short", "convert_ushort",
            "convert_int", "convert_uint", "convert_long", "convert_ulong",
            "convert_float", "convert_double",
            "as_char", "as_uchar", "as_short", "as_ushort", "as_int", "as_uint",
            "as_long", "as_ulong", "as_float", "as_double",
            // Atomic functions
            "atom_add", "atom_sub", "atom_xchg", "atom_inc", "atom_dec",
            "atom_cmpxchg", "atom_min", "atom_max", "atom_and", "atom_or", "atom_xor",
            "atomic_add", "atomic_sub", "atomic_xchg", "atomic_inc", "atomic_dec",
            "atomic_cmpxchg", "atomic_min", "atomic_max", "atomic_and", "atomic_or",
            // I/O
            "printf",
            // Image functions
            "read_imagef", "read_imagei", "read_imageui",
            "write_imagef", "write_imagei", "write_imageui",
            "get_image_width", "get_image_height", "get_image_depth",
            // Vector load/store
            "vload2", "vload3", "vload4", "vload8", "vload16",
            "vstore2", "vstore3", "vstore4", "vstore8", "vstore16",
            // Misc
            "select"
        )

        private val QUALIFIERS = setOf(
            "__kernel", "kernel",
            "__global", "global", "__local", "local",
            "__constant", "constant", "__private", "private",
            "__generic", "generic",
            "__read_only", "read_only", "__write_only", "write_only",
            "__read_write", "read_write",
            "__attribute__",
            "CLK_LOCAL_MEM_FENCE", "CLK_GLOBAL_MEM_FENCE"
        )
    }
}
