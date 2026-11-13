package com.gpudev.accelerator.ptx

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType

object PtxLanguage : Language("PTX") {
    override fun getDisplayName(): String = "PTX"
}

object PtxTokenTypes {
    val DIRECTIVE = IElementType("PTX_DIRECTIVE", PtxLanguage)
    val INSTRUCTION = IElementType("PTX_INSTRUCTION", PtxLanguage)
    val REGISTER = IElementType("PTX_REGISTER", PtxLanguage)
    val PREDICATE = IElementType("PTX_PREDICATE", PtxLanguage)
    val LABEL = IElementType("PTX_LABEL", PtxLanguage)
    val IDENTIFIER = IElementType("PTX_IDENTIFIER", PtxLanguage)
    val NUMBER = IElementType("PTX_NUMBER", PtxLanguage)
    val STRING = IElementType("PTX_STRING", PtxLanguage)
    val COMMENT = IElementType("PTX_COMMENT", PtxLanguage)
    val OPERATOR = IElementType("PTX_OPERATOR", PtxLanguage)
}
