/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.cuda

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.gpudev.accelerator.common.GpuCompletionProvider

class CudaCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), GpuCompletionProvider("CUDA"))
    }
}
