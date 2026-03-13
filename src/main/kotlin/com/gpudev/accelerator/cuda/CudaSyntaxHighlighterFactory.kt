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

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            CudaTokenTypes.KEYWORD -> KEYWORD_KEYS
            CudaTokenTypes.TYPE_KEYWORD -> TYPE_KEYS
            CudaTokenTypes.BUILTIN_FUNCTION -> BUILTIN_KEYS
            CudaTokenTypes.QUALIFIER -> QUALIFIER_KEYS
            CudaTokenTypes.LINE_COMMENT, CudaTokenTypes.BLOCK_COMMENT -> COMMENT_KEYS
            CudaTokenTypes.STRING_LITERAL -> STRING_KEYS
            CudaTokenTypes.CHAR_LITERAL -> STRING_KEYS
            CudaTokenTypes.NUMBER -> NUMBER_KEYS
            CudaTokenTypes.PREPROCESSOR -> PREPROCESSOR_KEYS
            CudaTokenTypes.OPERATOR -> OPERATOR_KEYS
            CudaTokenTypes.SEMICOLON, CudaTokenTypes.COMMA -> COMMA_KEYS
            CudaTokenTypes.DOT -> DOT_KEYS
            CudaTokenTypes.LBRACE, CudaTokenTypes.RBRACE -> BRACE_KEYS
            CudaTokenTypes.LPAREN, CudaTokenTypes.RPAREN -> PAREN_KEYS
            CudaTokenTypes.LBRACKET, CudaTokenTypes.RBRACKET -> BRACKET_KEYS
            CudaTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS
            else -> EMPTY_KEYS
        }
    }

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
    }
}
