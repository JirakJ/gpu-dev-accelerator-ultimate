package com.gpudev.accelerator.metal

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class MetalCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            MetalCompletionProvider()
        )
    }
}

class MetalCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val keywords = listOf(
            "kernel", "vertex", "fragment", "constant", "device", "threadgroup",
            "thread_position_in_grid", "thread_position_in_threadgroup",
            "threadgroup_position_in_grid", "threads_per_threadgroup",
            "float4", "float3", "float2", "half4", "half3", "half2",
            "texture2d", "texture3d", "sampler", "depth2d"
        )
        
        keywords.forEach { keyword ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText("Metal")
                    .withBoldness(true)
            )
        }
    }
}
