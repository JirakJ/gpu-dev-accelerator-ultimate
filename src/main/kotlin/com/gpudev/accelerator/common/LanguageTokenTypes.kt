package com.gpudev.accelerator.common

import com.intellij.lang.Language
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * Shared token type definitions for C-like GPU shading languages.
 * Each language instantiates this with its own [Language] to scope token types.
 */
open class LanguageTokenTypes(language: Language) {
    @JvmField val LINE_COMMENT = IElementType("LINE_COMMENT", language)
    @JvmField val BLOCK_COMMENT = IElementType("BLOCK_COMMENT", language)
    @JvmField val KEYWORD = IElementType("KEYWORD", language)
    @JvmField val TYPE_KEYWORD = IElementType("TYPE_KEYWORD", language)
    @JvmField val BUILTIN_FUNCTION = IElementType("BUILTIN_FUNCTION", language)
    @JvmField val QUALIFIER = IElementType("QUALIFIER", language)
    @JvmField val IDENTIFIER = IElementType("IDENTIFIER", language)
    @JvmField val NUMBER = IElementType("NUMBER", language)
    @JvmField val STRING_LITERAL = IElementType("STRING_LITERAL", language)
    @JvmField val CHAR_LITERAL = IElementType("CHAR_LITERAL", language)
    @JvmField val PREPROCESSOR = IElementType("PREPROCESSOR", language)
    @JvmField val OPERATOR = IElementType("OPERATOR", language)
    @JvmField val SEMICOLON = IElementType("SEMICOLON", language)
    @JvmField val COMMA = IElementType("COMMA", language)
    @JvmField val DOT = IElementType("DOT", language)
    @JvmField val LBRACE = IElementType("LBRACE", language)
    @JvmField val RBRACE = IElementType("RBRACE", language)
    @JvmField val LPAREN = IElementType("LPAREN", language)
    @JvmField val RPAREN = IElementType("RPAREN", language)
    @JvmField val LBRACKET = IElementType("LBRACKET", language)
    @JvmField val RBRACKET = IElementType("RBRACKET", language)

    @JvmField val COMMENTS: TokenSet = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
    @JvmField val STRINGS: TokenSet = TokenSet.create(STRING_LITERAL, CHAR_LITERAL)
}
