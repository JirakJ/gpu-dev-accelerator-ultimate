package com.gpudev.accelerator.opencl

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class OpenCLCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            OpenCLCompletionProvider()
        )
    }
}

class OpenCLCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        COMPLETIONS.forEach { (text, category) ->
            result.addElement(
                LookupElementBuilder.create(text)
                    .withTypeText(category)
                    .withBoldness(category == "qualifier" || category == "keyword")
            )
        }
    }

    companion object {
        private val COMPLETIONS = listOf(
            // Qualifiers
            "__kernel" to "qualifier", "kernel" to "qualifier",
            "__global" to "qualifier", "global" to "qualifier",
            "__local" to "qualifier", "local" to "qualifier",
            "__constant" to "qualifier", "constant" to "qualifier",
            "__private" to "qualifier", "private" to "qualifier",
            "__read_only" to "qualifier", "__write_only" to "qualifier",
            "__read_write" to "qualifier",
            // Work-item functions
            "get_global_id" to "work-item", "get_local_id" to "work-item",
            "get_group_id" to "work-item", "get_global_size" to "work-item",
            "get_local_size" to "work-item", "get_num_groups" to "work-item",
            "get_work_dim" to "work-item", "get_global_offset" to "work-item",
            // Synchronization
            "barrier" to "sync", "mem_fence" to "sync",
            "CLK_LOCAL_MEM_FENCE" to "sync", "CLK_GLOBAL_MEM_FENCE" to "sync",
            // Vector types
            "float2" to "type", "float3" to "type", "float4" to "type",
            "float8" to "type", "float16" to "type",
            "int2" to "type", "int3" to "type", "int4" to "type",
            "uint2" to "type", "uint3" to "type", "uint4" to "type",
            "half" to "type", "half2" to "type", "half4" to "type",
            // Image types
            "image2d_t" to "type", "image3d_t" to "type", "sampler_t" to "type",
            // Math functions
            "clamp" to "math", "mix" to "math", "step" to "math",
            "smoothstep" to "math", "fabs" to "math", "sqrt" to "math",
            "sin" to "math", "cos" to "math", "pow" to "math",
            // Atomic functions
            "atomic_add" to "atomic", "atomic_sub" to "atomic",
            "atomic_xchg" to "atomic", "atomic_cmpxchg" to "atomic",
            "atomic_min" to "atomic", "atomic_max" to "atomic",
            // Image functions
            "read_imagef" to "image", "write_imagef" to "image",
            "read_imagei" to "image", "write_imagei" to "image",
            // I/O
            "printf" to "io"
        )
    }
}
