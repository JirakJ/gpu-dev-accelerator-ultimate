package com.gpudev.accelerator.metal

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class MetalSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return MetalSyntaxHighlighter()
    }
}

class MetalSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = MetalLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            MetalTokenTypes.KEYWORD -> KEYWORD_KEYS
            MetalTokenTypes.TYPE_KEYWORD -> TYPE_KEYS
            MetalTokenTypes.BUILTIN_FUNCTION -> BUILTIN_KEYS
            MetalTokenTypes.QUALIFIER -> QUALIFIER_KEYS
            MetalTokenTypes.LINE_COMMENT, MetalTokenTypes.BLOCK_COMMENT -> COMMENT_KEYS
            MetalTokenTypes.STRING_LITERAL -> STRING_KEYS
            MetalTokenTypes.CHAR_LITERAL -> STRING_KEYS
            MetalTokenTypes.NUMBER -> NUMBER_KEYS
            MetalTokenTypes.PREPROCESSOR -> PREPROCESSOR_KEYS
            MetalTokenTypes.OPERATOR -> OPERATOR_KEYS
            MetalTokenTypes.SEMICOLON, MetalTokenTypes.COMMA -> COMMA_KEYS
            MetalTokenTypes.DOT -> DOT_KEYS
            MetalTokenTypes.LBRACE, MetalTokenTypes.RBRACE -> BRACE_KEYS
            MetalTokenTypes.LPAREN, MetalTokenTypes.RPAREN -> PAREN_KEYS
            MetalTokenTypes.LBRACKET, MetalTokenTypes.RBRACKET -> BRACKET_KEYS
            MetalTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS
            else -> EMPTY_KEYS
        }
    }

    companion object {
        private val KEYWORD_KEYS = arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
        private val TYPE_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("METAL_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)
        )
        private val BUILTIN_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("METAL_BUILTIN", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        )
        private val QUALIFIER_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("METAL_QUALIFIER", DefaultLanguageHighlighterColors.KEYWORD)
        )
        private val COMMENT_KEYS = arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
        private val STRING_KEYS = arrayOf(DefaultLanguageHighlighterColors.STRING)
        private val NUMBER_KEYS = arrayOf(DefaultLanguageHighlighterColors.NUMBER)
        private val PREPROCESSOR_KEYS = arrayOf(DefaultLanguageHighlighterColors.METADATA)
        private val OPERATOR_KEYS = arrayOf(DefaultLanguageHighlighterColors.OPERATION_SIGN)
        private val COMMA_KEYS = arrayOf(DefaultLanguageHighlighterColors.COMMA)
        private val DOT_KEYS = arrayOf(DefaultLanguageHighlighterColors.DOT)
        private val BRACE_KEYS = arrayOf(DefaultLanguageHighlighterColors.BRACES)
        private val PAREN_KEYS = arrayOf(DefaultLanguageHighlighterColors.PARENTHESES)
        private val BRACKET_KEYS = arrayOf(DefaultLanguageHighlighterColors.BRACKETS)
        private val IDENTIFIER_KEYS = arrayOf(DefaultLanguageHighlighterColors.IDENTIFIER)
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
