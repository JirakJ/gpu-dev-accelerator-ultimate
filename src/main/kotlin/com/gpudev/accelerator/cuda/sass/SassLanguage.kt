package com.gpudev.accelerator.cuda.sass

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType

object SassLanguage : Language("CUDA_SASS") {
    override fun getDisplayName(): String = "CUDA SASS"
}

object SassTokenTypes {
    val DIRECTIVE = IElementType("SASS_DIRECTIVE", SassLanguage)
    val INSTRUCTION = IElementType("SASS_INSTRUCTION", SassLanguage)
    val REGISTER = IElementType("SASS_REGISTER", SassLanguage)
    val PREDICATE = IElementType("SASS_PREDICATE", SassLanguage)
    val ADDRESS = IElementType("SASS_ADDRESS", SassLanguage)
    val LABEL = IElementType("SASS_LABEL", SassLanguage)
    val IDENTIFIER = IElementType("SASS_IDENTIFIER", SassLanguage)
    val NUMBER = IElementType("SASS_NUMBER", SassLanguage)
    val STRING = IElementType("SASS_STRING", SassLanguage)
    val COMMENT = IElementType("SASS_COMMENT", SassLanguage)
    val OPERATOR = IElementType("SASS_OPERATOR", SassLanguage)
}
