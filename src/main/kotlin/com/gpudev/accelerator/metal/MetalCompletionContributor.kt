/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.metal

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.gpudev.accelerator.common.GpuCompletionProvider

class MetalCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), GpuCompletionProvider("Metal"))
    }
}
