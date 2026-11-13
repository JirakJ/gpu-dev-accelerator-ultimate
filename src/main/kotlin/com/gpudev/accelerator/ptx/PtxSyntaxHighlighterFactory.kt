package com.gpudev.accelerator.ptx

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class PtxSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        PtxSyntaxHighlighter()
}

class PtxSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = PtxLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        tokenType?.let { HIGHLIGHTS[it] } ?: EMPTY_KEYS

    private companion object {
        val EMPTY_KEYS = emptyArray<TextAttributesKey>()
        val HIGHLIGHTS = mapOf(
            PtxTokenTypes.DIRECTIVE to arrayOf(DefaultLanguageHighlighterColors.METADATA),
            PtxTokenTypes.INSTRUCTION to arrayOf(DefaultLanguageHighlighterColors.KEYWORD),
            PtxTokenTypes.REGISTER to arrayOf(DefaultLanguageHighlighterColors.INSTANCE_FIELD),
            PtxTokenTypes.PREDICATE to arrayOf(DefaultLanguageHighlighterColors.KEYWORD),
            PtxTokenTypes.LABEL to arrayOf(DefaultLanguageHighlighterColors.LABEL),
            PtxTokenTypes.IDENTIFIER to arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER),
            PtxTokenTypes.NUMBER to arrayOf(DefaultLanguageHighlighterColors.NUMBER),
            PtxTokenTypes.STRING to arrayOf(DefaultLanguageHighlighterColors.STRING),
            PtxTokenTypes.COMMENT to arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT),
            PtxTokenTypes.OPERATOR to arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
        )
    }
}
