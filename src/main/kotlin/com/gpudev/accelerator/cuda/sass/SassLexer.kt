package com.gpudev.accelerator.cuda.sass

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/** Tolerant lexer for nvdisasm textual output. */
@Suppress("CyclomaticComplexMethod", "TooManyFunctions")
class SassLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var currentTokenType: IElementType? = null
    private var instructionPosition = false
    private var tokenState = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        bufferEnd = endOffset
        tokenEnd = startOffset
        instructionPosition = initialState == 0 || initialState and STATE_INSTRUCTION_POSITION != 0
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
        tokenState = STATE_MARKER or (if (instructionPosition) STATE_INSTRUCTION_POSITION else 0)
        classify(buffer[tokenStart])
    }

    private fun classify(char: Char) {
        when {
            char.isWhitespace() -> scanWhitespace()
            isComment() -> scanLineComment()
            char == '"' -> scanString()
            isAddressStart(char) -> scanAddress()
            char == '@' -> scanPredicate()
            char == '%' && peek(1)?.let { it == 'r' || it == 'f' || it == 'p' || it == 'R' } == true -> scanRegister()
            isDirectiveStart(char) -> scanDirective()
            char == '_' || char.isLetter() -> scanWord()
            char.isDigit() -> scanNumber()
            else -> scanOperator()
        }
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

    private fun scanWhitespace() {
        val end = scanWhile(tokenStart) { it.isWhitespace() }
        if (buffer.subSequence(tokenStart, end).contains('\n')) instructionPosition = true
        currentTokenType = TokenType.WHITE_SPACE
    }

    private fun isComment(): Boolean = buffer[tokenStart] == '/' && peek(1) == '/'

    private fun scanLineComment() {
        val end = scanWhile(tokenStart) { it != '\n' }
        currentTokenType = if (buffer.subSequence(tokenStart, end).toString().startsWith("//-")) {
            SassTokenTypes.DIRECTIVE
        } else {
            SassTokenTypes.COMMENT
        }
    }

    private fun isAddressStart(char: Char): Boolean =
        char == '/' && peek(1) == '*' && peek(2)?.isDigit() == true

    private fun scanAddress() {
        val close = (tokenStart + 2 until bufferEnd).firstOrNull { buffer[it] == '*' && peekAt(it + 1) == '/' }
        tokenEnd = if (close == null) tokenStart + 1 else close + 2
        currentTokenType = SassTokenTypes.ADDRESS
    }

    private fun peekAt(position: Int): Char? = buffer.getOrNull(position)?.takeIf { position < bufferEnd }

    private fun scanPredicate() {
        tokenEnd = tokenStart + 1
        while (tokenEnd < bufferEnd && (buffer[tokenEnd].isLetterOrDigit() || buffer[tokenEnd] == '!')) tokenEnd++
        currentTokenType = SassTokenTypes.PREDICATE
    }

    private fun scanRegister() {
        tokenEnd = scanWhile(tokenStart) { it == '%' || it == '_' || it == '.' || it.isLetterOrDigit() }
        currentTokenType = SassTokenTypes.REGISTER
        instructionPosition = false
    }

    private fun isDirectiveStart(char: Char): Boolean =
        char == '.' && peek(1)?.let { it == '_' || it.isLetter() } == true

    private fun scanDirective() {
        val end = scanWhile(tokenStart) { it == '_' || it == '.' || it.isLetterOrDigit() }
        val label = end < bufferEnd && buffer[end] == ':'
        tokenEnd = end
        currentTokenType = if (label) SassTokenTypes.LABEL else SassTokenTypes.DIRECTIVE
        instructionPosition = false
    }

    private fun scanWord() {
        val end = scanWhile(tokenStart) { it == '_' || it == '$' || it == '.' || it.isLetterOrDigit() }
        val text = buffer.subSequence(tokenStart, end).toString()
        val isLabel = end < bufferEnd && buffer[end] == ':'
        currentTokenType = when {
            isLabel -> SassTokenTypes.LABEL
            isPredicateRegister(text) -> SassTokenTypes.PREDICATE
            isRegister(text) -> SassTokenTypes.REGISTER
            instructionPosition -> SassTokenTypes.INSTRUCTION
            else -> SassTokenTypes.IDENTIFIER
        }
        instructionPosition = false
    }

    private fun isPredicateRegister(word: String): Boolean =
        word == "PT" || word == "UPT" || word.matches(Regex("(?:P|UP)\\d+"))

    private fun isRegister(word: String): Boolean =
        word == "RZ" || word == "URZ" || word == "c" || word.startsWith("SR_") ||
            word.matches(Regex("(?:R|UR)\\d+(?:\\.\\d+)?"))

    private fun scanNumber() {
        tokenEnd = scanWhile(tokenStart) { it.isLetterOrDigit() || it in "xX._" }
        currentTokenType = SassTokenTypes.NUMBER
        instructionPosition = false
    }

    private fun scanString() {
        var end = tokenStart + 1
        while (end < bufferEnd && buffer[end] != '"' && buffer[end] != '\n') {
            end = if (buffer[end] == '\\') (end + 2).coerceAtMost(bufferEnd) else end + 1
        }
        if (end < bufferEnd && buffer[end] == '"') end++
        tokenEnd = end
        currentTokenType = SassTokenTypes.STRING
    }

    private fun scanOperator() {
        tokenEnd = tokenStart + 1
        currentTokenType = SassTokenTypes.OPERATOR
    }

    private companion object {
        const val STATE_MARKER = 2
        const val STATE_INSTRUCTION_POSITION = 1
    }
}
