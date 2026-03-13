package com.gpudev.accelerator.common

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Base lexer for C-like GPU shading languages (CUDA, Metal, OpenCL).
 * Handles common tokenization: comments, preprocessor directives, strings,
 * numbers, identifiers/keywords, and operators.
 *
 * Subclasses provide language-specific keyword sets and token types.
 */
abstract class CLikeLexer(
    protected val tokens: LanguageTokenTypes,
    private val keywords: Set<String>,
    private val typeKeywords: Set<String>,
    private val builtinFunctions: Set<String>,
    private val qualifiers: Set<String>
) : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var currentTokenType: IElementType? = null
    private var state = STATE_INITIAL

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenEnd = startOffset
        this.state = initialState
        advance()
    }

    override fun getState(): Int = state
    override fun getTokenType(): IElementType? = currentTokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            currentTokenType = null
            return
        }

        if (state == STATE_IN_BLOCK_COMMENT) {
            lexBlockCommentContent()
            return
        }

        val c = buffer[tokenStart]
        when {
            c.isWhitespace() -> lexWhitespace()
            c == '/' && peek(1) == '/' -> lexLineComment()
            c == '/' && peek(1) == '*' -> lexBlockComment()
            c == '#' -> lexPreprocessor()
            c == '"' -> lexString()
            c == '\'' -> lexChar()
            c.isDigit() -> lexNumber()
            c == '.' && peek(1)?.isDigit() == true -> lexNumber()
            c == '_' || c.isLetter() -> lexWord()
            else -> lexOperatorOrPunctuation()
        }
    }

    private fun peek(offset: Int): Char? {
        val pos = tokenStart + offset
        return if (pos < bufferEnd) buffer[pos] else null
    }

    private fun lexWhitespace() {
        var pos = tokenStart
        while (pos < bufferEnd && buffer[pos].isWhitespace()) pos++
        tokenEnd = pos
        currentTokenType = TokenType.WHITE_SPACE
    }

    private fun lexLineComment() {
        var pos = tokenStart + 2
        while (pos < bufferEnd && buffer[pos] != '\n') pos++
        tokenEnd = pos
        currentTokenType = tokens.LINE_COMMENT
    }

    private fun lexBlockComment() {
        state = STATE_IN_BLOCK_COMMENT
        var pos = tokenStart + 2
        while (pos + 1 < bufferEnd) {
            if (buffer[pos] == '*' && buffer[pos + 1] == '/') {
                pos += 2
                state = STATE_INITIAL
                break
            }
            pos++
        }
        if (state == STATE_IN_BLOCK_COMMENT) pos = bufferEnd
        tokenEnd = pos
        currentTokenType = tokens.BLOCK_COMMENT
    }

    private fun lexBlockCommentContent() {
        var pos = tokenStart
        while (pos + 1 < bufferEnd) {
            if (buffer[pos] == '*' && buffer[pos + 1] == '/') {
                pos += 2
                state = STATE_INITIAL
                break
            }
            pos++
        }
        if (state == STATE_IN_BLOCK_COMMENT) pos = bufferEnd
        tokenEnd = pos
        currentTokenType = tokens.BLOCK_COMMENT
    }

    private fun lexPreprocessor() {
        var pos = tokenStart + 1
        while (pos < bufferEnd && (buffer[pos] == ' ' || buffer[pos] == '\t')) pos++
        while (pos < bufferEnd) {
            val c = buffer[pos]
            if (c == '\\' && pos + 1 < bufferEnd) {
                val next = buffer[pos + 1]
                if (next == '\n') { pos += 2; continue }
                if (next == '\r') {
                    pos += 2
                    if (pos < bufferEnd && buffer[pos] == '\n') pos++
                    continue
                }
            }
            if (c == '\n') break
            pos++
        }
        tokenEnd = pos
        currentTokenType = tokens.PREPROCESSOR
    }

    private fun lexString() {
        var pos = tokenStart + 1
        while (pos < bufferEnd) {
            when (buffer[pos]) {
                '\\' -> { pos++; if (pos < bufferEnd) pos++ }
                '"' -> { pos++; break }
                '\n' -> break
                else -> pos++
            }
        }
        tokenEnd = pos
        currentTokenType = tokens.STRING_LITERAL
    }

    private fun lexChar() {
        var pos = tokenStart + 1
        while (pos < bufferEnd) {
            when (buffer[pos]) {
                '\\' -> { pos++; if (pos < bufferEnd) pos++ }
                '\'' -> { pos++; break }
                '\n' -> break
                else -> pos++
            }
        }
        tokenEnd = pos
        currentTokenType = tokens.CHAR_LITERAL
    }

    private fun lexNumber() {
        var pos = tokenStart

        // Leading dot: .5, .123e4
        if (buffer[pos] == '.') {
            pos++
            while (pos < bufferEnd && buffer[pos].isDigit()) pos++
            pos = readExponent(pos)
            pos = readNumberSuffix(pos)
            tokenEnd = pos
            currentTokenType = tokens.NUMBER
            return
        }

        // Hex: 0x1F
        if (buffer[pos] == '0' && pos + 1 < bufferEnd) {
            val next = buffer[pos + 1]
            if (next == 'x' || next == 'X') {
                pos += 2
                while (pos < bufferEnd && isHexDigit(buffer[pos])) pos++
                pos = readNumberSuffix(pos)
                tokenEnd = pos
                currentTokenType = tokens.NUMBER
                return
            }
            // Binary: 0b101
            if (next == 'b' || next == 'B') {
                pos += 2
                while (pos < bufferEnd && (buffer[pos] == '0' || buffer[pos] == '1')) pos++
                pos = readNumberSuffix(pos)
                tokenEnd = pos
                currentTokenType = tokens.NUMBER
                return
            }
        }

        // Integer part
        while (pos < bufferEnd && buffer[pos].isDigit()) pos++

        // Decimal point (only if followed by digit, exponent, or end-of-input)
        if (pos < bufferEnd && buffer[pos] == '.') {
            val afterDot = pos + 1
            if (afterDot >= bufferEnd || buffer[afterDot].isDigit() ||
                buffer[afterDot] == 'e' || buffer[afterDot] == 'E'
            ) {
                pos++
                while (pos < bufferEnd && buffer[pos].isDigit()) pos++
            }
        }

        pos = readExponent(pos)
        pos = readNumberSuffix(pos)
        tokenEnd = pos
        currentTokenType = tokens.NUMBER
    }

    private fun readExponent(startPos: Int): Int {
        var pos = startPos
        if (pos < bufferEnd && (buffer[pos] == 'e' || buffer[pos] == 'E')) {
            pos++
            if (pos < bufferEnd && (buffer[pos] == '+' || buffer[pos] == '-')) pos++
            while (pos < bufferEnd && buffer[pos].isDigit()) pos++
        }
        return pos
    }

    private fun readNumberSuffix(startPos: Int): Int {
        var pos = startPos
        val suffixStart = pos
        while (pos < bufferEnd && buffer[pos] in "fFlLuUhH") pos++
        // If suffix bleeds into an identifier, roll back
        if (pos > suffixStart && pos < bufferEnd && (buffer[pos].isLetterOrDigit() || buffer[pos] == '_')) {
            return suffixStart
        }
        return pos
    }

    private fun isHexDigit(c: Char): Boolean = c.isDigit() || c in 'a'..'f' || c in 'A'..'F'

    private fun lexWord() {
        var pos = tokenStart
        while (pos < bufferEnd && (buffer[pos] == '_' || buffer[pos].isLetterOrDigit())) pos++
        tokenEnd = pos
        val word = buffer.subSequence(tokenStart, tokenEnd).toString()
        currentTokenType = classifyWord(word)
    }

    protected open fun classifyWord(word: String): IElementType {
        return when {
            word in qualifiers -> tokens.QUALIFIER
            word in keywords -> tokens.KEYWORD
            word in typeKeywords -> tokens.TYPE_KEYWORD
            word in builtinFunctions -> tokens.BUILTIN_FUNCTION
            else -> tokens.IDENTIFIER
        }
    }

    private fun lexOperatorOrPunctuation() {
        val c = buffer[tokenStart]
        var pos = tokenStart + 1

        currentTokenType = when (c) {
            '{' -> tokens.LBRACE
            '}' -> tokens.RBRACE
            '(' -> tokens.LPAREN
            ')' -> tokens.RPAREN
            '[' -> tokens.LBRACKET
            ']' -> tokens.RBRACKET
            ';' -> tokens.SEMICOLON
            ',' -> tokens.COMMA
            '.' -> tokens.DOT
            else -> {
                if (pos < bufferEnd) {
                    val next = buffer[pos]
                    when {
                        c == '<' && next == '<' -> {
                            pos++
                            if (pos < bufferEnd && (buffer[pos] == '<' || buffer[pos] == '=')) pos++
                        }
                        c == '>' && next == '>' -> {
                            pos++
                            if (pos < bufferEnd && (buffer[pos] == '>' || buffer[pos] == '=')) pos++
                        }
                        next == '=' && c in "=!<>+-*/%&|^" -> pos++
                        (c == '&' && next == '&') || (c == '|' && next == '|') -> pos++
                        (c == '+' && next == '+') || (c == '-' && next == '-') -> pos++
                        c == '-' && next == '>' -> pos++
                    }
                }
                tokens.OPERATOR
            }
        }

        tokenEnd = pos
    }

    companion object {
        const val STATE_INITIAL = 0
        const val STATE_IN_BLOCK_COMMENT = 1
    }
}
