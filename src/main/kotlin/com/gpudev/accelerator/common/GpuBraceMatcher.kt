/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.common

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.gpudev.accelerator.cuda.CudaTokenTypes
import com.gpudev.accelerator.metal.MetalTokenTypes
import com.gpudev.accelerator.opencl.OpenCLTokenTypes

/** Highlights matching (), {}, [] pairs. */
abstract class GpuBraceMatcher(tokens: LanguageTokenTypes) : PairedBraceMatcher {

    private val pairs = arrayOf(
        BracePair(tokens.LPAREN, tokens.RPAREN, false),
        BracePair(tokens.LBRACE, tokens.RBRACE, true),
        BracePair(tokens.LBRACKET, tokens.RBRACKET, false)
    )

    override fun getPairs(): Array<BracePair> = pairs
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}

class CudaBraceMatcher : GpuBraceMatcher(CudaTokenTypes)
class MetalBraceMatcher : GpuBraceMatcher(MetalTokenTypes)
class OpenCLBraceMatcher : GpuBraceMatcher(OpenCLTokenTypes)
