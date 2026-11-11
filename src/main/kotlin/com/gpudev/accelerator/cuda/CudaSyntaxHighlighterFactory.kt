package com.gpudev.accelerator.cuda

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class CudaSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return CudaSyntaxHighlighter()
    }
}

class CudaSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = CudaLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        tokenType?.let { HIGHLIGHTS[it] } ?: EMPTY_KEYS

    companion object {
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "CUDA_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD
        )
        val FUNCTION = TextAttributesKey.createTextAttributesKey(
            "CUDA_FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )
        val KERNEL = TextAttributesKey.createTextAttributesKey(
            "CUDA_KERNEL", DefaultLanguageHighlighterColors.STATIC_METHOD
        )

        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val TYPE_KEYS = arrayOf(
            TextAttributesKey.createTextAttributesKey("CUDA_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)
        )
        private val BUILTIN_KEYS = arrayOf(FUNCTION)
        private val QUALIFIER_KEYS = arrayOf(KERNEL)
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
                CudaTokenTypes.KEYWORD to KEYWORD_KEYS,
                CudaTokenTypes.TYPE_KEYWORD to TYPE_KEYS,
                CudaTokenTypes.BUILTIN_FUNCTION to BUILTIN_KEYS,
                CudaTokenTypes.QUALIFIER to QUALIFIER_KEYS,
                CudaTokenTypes.LINE_COMMENT to COMMENT_KEYS,
                CudaTokenTypes.BLOCK_COMMENT to COMMENT_KEYS,
                CudaTokenTypes.STRING_LITERAL to STRING_KEYS,
                CudaTokenTypes.CHAR_LITERAL to STRING_KEYS,
                CudaTokenTypes.NUMBER to NUMBER_KEYS,
                CudaTokenTypes.PREPROCESSOR to PREPROCESSOR_KEYS,
                CudaTokenTypes.OPERATOR to OPERATOR_KEYS,
                CudaTokenTypes.SEMICOLON to COMMA_KEYS,
                CudaTokenTypes.COMMA to COMMA_KEYS,
                CudaTokenTypes.DOT to DOT_KEYS,
                CudaTokenTypes.LBRACE to BRACE_KEYS,
                CudaTokenTypes.RBRACE to BRACE_KEYS,
                CudaTokenTypes.LPAREN to PAREN_KEYS,
                CudaTokenTypes.RPAREN to PAREN_KEYS,
                CudaTokenTypes.LBRACKET to BRACKET_KEYS,
                CudaTokenTypes.RBRACKET to BRACKET_KEYS,
                CudaTokenTypes.IDENTIFIER to IDENTIFIER_KEYS
            )
        }
    }
}
