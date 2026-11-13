package com.gpudev.accelerator.cuda.ptx

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/** A tolerant lexer for PTX text; unknown opcodes and directives remain valid identifiers. */
@Suppress("TooManyFunctions")
class PtxLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var currentTokenType: IElementType? = null
    private var lineMayContainInstruction = true
    private var predicatePrefix = false
    private var tokenState = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        lineMayContainInstruction = initialState == 0 || initialState and STATE_INSTRUCTION_POSITION != 0
        predicatePrefix = initialState and STATE_PREDICATE_PREFIX != 0
        tokenEnd = startOffset
        advance()
    }

    override fun getState(): Int = tokenState
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

        tokenState = STATE_MARKER or
            (if (lineMayContainInstruction) STATE_INSTRUCTION_POSITION else 0) or
            (if (predicatePrefix) STATE_PREDICATE_PREFIX else 0)
        classifyToken(buffer[tokenStart])
    }

    private fun classifyToken(c: Char) {
        when {
            c.isWhitespace() -> scanWhitespace()
            isLineCommentStart() -> scanWhile(tokenStart + 2) { it != '\n' }
                .also { currentTokenType = PtxTokenTypes.COMMENT }
            c == '"' -> scanString()
            isRegisterStart(c) -> scanRegister()
            c == '@' -> scanPredicate()
            isDirectiveStart(c) -> scanDirective()
            isWordStart(c) -> scanWord()
            c.isDigit() -> scanNumber()
            else -> scanOperator()
        }
    }

    private fun isLineCommentStart(): Boolean = buffer[tokenStart] == '/' && peek(1) == '/'

    private fun isRegisterStart(c: Char): Boolean =
        c == '%' && peek(1)?.let { it == '_' || it.isLetter() } == true

    private fun isDirectiveStart(c: Char): Boolean =
        c == '.' && peek(1)?.let { it == '_' || it.isLetter() } == true

    private fun isWordStart(c: Char): Boolean = c == '_' || c.isLetter() || c == '$'

    private fun scanWhitespace() {
        val end = scanWhile(tokenStart) { it.isWhitespace() }
        if (buffer.subSequence(tokenStart, end).contains('\n')) {
            lineMayContainInstruction = true
            predicatePrefix = false
        }
        currentTokenType = TokenType.WHITE_SPACE
    }

    private fun peek(offset: Int): Char? {
        val position = tokenStart + offset
        return if (position < bufferEnd) buffer.getOrNull(position) else null
    }

    private fun scanWhile(start: Int, predicate: (Char) -> Boolean): Int {
        var end = start
        while (end < bufferEnd && predicate(buffer[end])) end++
        tokenEnd = end
        return end
    }

    private fun scanString() {
        var end = tokenStart + 1
        while (end < bufferEnd && buffer[end] != '"' && buffer[end] != '\n') {
            end = if (buffer[end] == '\\') (end + 2).coerceAtMost(bufferEnd) else end + 1
        }
        if (end < bufferEnd && buffer[end] == '"') end++
        tokenEnd = end
        currentTokenType = PtxTokenTypes.STRING
    }

    private fun scanRegister() {
        val end = scanWhile(tokenStart + 1) { it == '_' || it == '$' || it == '.' || it.isLetterOrDigit() }
        currentTokenType = if (buffer[tokenStart + 1] == 'p') PtxTokenTypes.PREDICATE else PtxTokenTypes.REGISTER
        if (!predicatePrefix) lineMayContainInstruction = false
        predicatePrefix = false
        tokenEnd = end
    }

    private fun scanPredicate() {
        tokenEnd = if (peek(1) == '!') tokenStart + 2 else tokenStart + 1
        predicatePrefix = true
        currentTokenType = PtxTokenTypes.PREDICATE
    }

    private fun scanDirective() {
        val end = scanWhile(tokenStart + 1) { it == '_' || it.isLetterOrDigit() }
        tokenEnd = end
        lineMayContainInstruction = false
        predicatePrefix = false
        currentTokenType = PtxTokenTypes.DIRECTIVE
    }

    private fun scanWord() {
        val end = scanWhile(tokenStart) { it == '_' || it == '$' || it.isLetterOrDigit() }
        tokenEnd = end
        val label = end < bufferEnd && buffer[end] == ':'
        currentTokenType = when {
            label -> PtxTokenTypes.LABEL
            lineMayContainInstruction -> PtxTokenTypes.INSTRUCTION
            else -> PtxTokenTypes.IDENTIFIER
        }
        lineMayContainInstruction = false
        predicatePrefix = false
    }

    private fun scanNumber() {
        var end = tokenStart
        while (end < bufferEnd) {
            if (isNumberContinuation(buffer[end], end)) {
                end++
            } else {
                break
            }
        }
        tokenEnd = end.coerceAtLeast(tokenStart + 1)
        lineMayContainInstruction = false
        currentTokenType = PtxTokenTypes.NUMBER
    }

    private fun isNumberContinuation(c: Char, position: Int): Boolean {
        if (c.isLetterOrDigit() || c == '_' || c == '.') return true
        return (c == '+' || c == '-') && position > tokenStart && buffer[position - 1] in "eEpP"
    }

    private fun scanOperator() {
        tokenEnd = tokenStart + 1
        if (buffer[tokenStart] == ';') lineMayContainInstruction = true
        currentTokenType = PtxTokenTypes.OPERATOR
    }

    private companion object {
        const val STATE_MARKER = 4
        const val STATE_INSTRUCTION_POSITION = 1
        const val STATE_PREDICATE_PREFIX = 2
    }
}
