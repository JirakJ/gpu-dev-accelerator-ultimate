package com.gpudev.accelerator.opencl

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class OpenCLSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return OpenCLSyntaxHighlighter()
    }
}

class OpenCLSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = OpenCLLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        tokenType?.let { HIGHLIGHTS[it] } ?: EMPTY_KEYS

    companion object {
        private val KEYWORD_KEYS = arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
        private val TYPE_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("OPENCL_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)
        )
        private val BUILTIN_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("OPENCL_BUILTIN", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        )
        private val QUALIFIER_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("OPENCL_QUALIFIER", DefaultLanguageHighlighterColors.KEYWORD)
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

        private val HIGHLIGHTS: Map<IElementType, Array<TextAttributesKey>> by lazy {
            mapOf(
                OpenCLTokenTypes.KEYWORD to KEYWORD_KEYS,
                OpenCLTokenTypes.TYPE_KEYWORD to TYPE_KEYS,
                OpenCLTokenTypes.BUILTIN_FUNCTION to BUILTIN_KEYS,
                OpenCLTokenTypes.QUALIFIER to QUALIFIER_KEYS,
                OpenCLTokenTypes.LINE_COMMENT to COMMENT_KEYS,
                OpenCLTokenTypes.BLOCK_COMMENT to COMMENT_KEYS,
                OpenCLTokenTypes.STRING_LITERAL to STRING_KEYS,
                OpenCLTokenTypes.CHAR_LITERAL to STRING_KEYS,
                OpenCLTokenTypes.NUMBER to NUMBER_KEYS,
                OpenCLTokenTypes.PREPROCESSOR to PREPROCESSOR_KEYS,
                OpenCLTokenTypes.OPERATOR to OPERATOR_KEYS,
                OpenCLTokenTypes.SEMICOLON to COMMA_KEYS,
                OpenCLTokenTypes.COMMA to COMMA_KEYS,
                OpenCLTokenTypes.DOT to DOT_KEYS,
                OpenCLTokenTypes.LBRACE to BRACE_KEYS,
                OpenCLTokenTypes.RBRACE to BRACE_KEYS,
                OpenCLTokenTypes.LPAREN to PAREN_KEYS,
                OpenCLTokenTypes.RPAREN to PAREN_KEYS,
                OpenCLTokenTypes.LBRACKET to BRACKET_KEYS,
                OpenCLTokenTypes.RBRACKET to BRACKET_KEYS,
                OpenCLTokenTypes.IDENTIFIER to IDENTIFIER_KEYS
            )
        }
    }
}
