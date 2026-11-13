package com.gpudev.accelerator.cuda.sass

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class SassSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        SassSyntaxHighlighter()
}

class SassSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = SassLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        tokenType?.let { HIGHLIGHTS[it] } ?: EMPTY_KEYS

    private companion object {
        val EMPTY_KEYS = emptyArray<TextAttributesKey>()
        val HIGHLIGHTS = mapOf(
            SassTokenTypes.DIRECTIVE to arrayOf(DefaultLanguageHighlighterColors.METADATA),
            SassTokenTypes.INSTRUCTION to arrayOf(DefaultLanguageHighlighterColors.KEYWORD),
            SassTokenTypes.REGISTER to arrayOf(DefaultLanguageHighlighterColors.INSTANCE_FIELD),
            SassTokenTypes.PREDICATE to arrayOf(DefaultLanguageHighlighterColors.KEYWORD),
            SassTokenTypes.ADDRESS to arrayOf(DefaultLanguageHighlighterColors.NUMBER),
            SassTokenTypes.LABEL to arrayOf(DefaultLanguageHighlighterColors.LABEL),
            SassTokenTypes.IDENTIFIER to arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER),
            SassTokenTypes.NUMBER to arrayOf(DefaultLanguageHighlighterColors.NUMBER),
            SassTokenTypes.STRING to arrayOf(DefaultLanguageHighlighterColors.STRING),
            SassTokenTypes.COMMENT to arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT),
            SassTokenTypes.OPERATOR to arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
        )
    }
}
