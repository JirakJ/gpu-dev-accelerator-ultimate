package com.gpudev.accelerator.lexer

import com.gpudev.accelerator.common.CLikeLexer
import com.gpudev.accelerator.common.LanguageTokenTypes
import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Base test class verifying core C-like lexer functionality shared by all GPU languages.
 * Tests are run using the OpenCL lexer as a representative implementation.
 */
class CLikeLexerBaseTest {

    private fun createLexer(): LexerBase {
        return com.gpudev.accelerator.opencl.OpenCLLexer()
    }

    private val tokens = com.gpudev.accelerator.opencl.OpenCLTokenTypes

    private fun tokenize(text: String): List<Pair<IElementType?, String>> {
        val lexer = createLexer()
        lexer.start(text, 0, text.length, 0)
        val result = mutableListOf<Pair<IElementType?, String>>()
        while (lexer.tokenType != null) {
            result.add(lexer.tokenType to text.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return result
    }

    @Test
    fun `empty input produces no tokens`() {
        val result = tokenize("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `whitespace only`() {
        val result = tokenize("  \t\n  ")
        assertEquals(1, result.size)
        assertEquals(TokenType.WHITE_SPACE, result[0].first)
    }

    @Test
    fun `lexer terminates at buffer end`() {
        val lexer = createLexer()
        lexer.start("abc", 0, 3, 0)
        assertNotNull(lexer.tokenType)
        lexer.advance()
        assertNull(lexer.tokenType)
        assertEquals(3, lexer.tokenStart)
    }

    @Test
    fun `lexer covers entire buffer without gaps`() {
        val text = "int x = 42; // comment\n"
        val lexer = createLexer()
        lexer.start(text, 0, text.length, 0)
        var lastEnd = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart, "Gap detected at position $lastEnd")
            assertTrue(lexer.tokenEnd > lexer.tokenStart, "Zero-length token at ${lexer.tokenStart}")
            lastEnd = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(text.length, lastEnd, "Lexer did not consume entire buffer")
    }

    @Test
    fun `line comment`() {
        val result = tokenize("// hello world")
        assertEquals(1, result.size)
        assertEquals(tokens.LINE_COMMENT, result[0].first)
        assertEquals("// hello world", result[0].second)
    }

    @Test
    fun `line comment stops at newline`() {
        val result = tokenize("// comment\nint x;")
        assertTrue(result.any { it.first == tokens.LINE_COMMENT && it.second == "// comment" })
        assertTrue(result.any { it.first == tokens.TYPE_KEYWORD && it.second == "int" })
    }

    @Test
    fun `block comment single line`() {
        val result = tokenize("/* block */")
        assertEquals(1, result.size)
        assertEquals(tokens.BLOCK_COMMENT, result[0].first)
    }

    @Test
    fun `block comment multiline`() {
        val result = tokenize("/* line1\nline2\n*/")
        assertEquals(1, result.size)
        assertEquals(tokens.BLOCK_COMMENT, result[0].first)
    }

    @Test
    fun `unterminated block comment`() {
        val result = tokenize("/* unterminated")
        assertEquals(1, result.size)
        assertEquals(tokens.BLOCK_COMMENT, result[0].first)
    }

    @Test
    fun `block comment state restoration`() {
        val lexer = createLexer()
        lexer.start("/* start", 0, 8, 0)
        assertNotNull(lexer.tokenType)
        val savedState = lexer.state
        assertEquals(CLikeLexer.STATE_IN_BLOCK_COMMENT, savedState)

        // Resume with saved state
        val text2 = " end */"
        lexer.start(text2, 0, text2.length, savedState)
        assertEquals(tokens.BLOCK_COMMENT, lexer.tokenType)
        lexer.advance()
        assertNull(lexer.tokenType)
    }

    @Test
    fun `preprocessor directive`() {
        val result = tokenize("#include <stdio.h>")
        assertEquals(1, result.size)
        assertEquals(tokens.PREPROCESSOR, result[0].first)
    }

    @Test
    fun `preprocessor with line continuation`() {
        val result = tokenize("#define FOO \\\nBAR")
        assertEquals(1, result.size)
        assertEquals(tokens.PREPROCESSOR, result[0].first)
        assertEquals("#define FOO \\\nBAR", result[0].second)
    }

    @Test
    fun `string literal`() {
        val result = tokenize("\"hello world\"")
        assertEquals(1, result.size)
        assertEquals(tokens.STRING_LITERAL, result[0].first)
    }

    @Test
    fun `string literal with escape`() {
        val result = tokenize("\"hello\\nworld\"")
        assertEquals(1, result.size)
        assertEquals(tokens.STRING_LITERAL, result[0].first)
        assertEquals("\"hello\\nworld\"", result[0].second)
    }

    @Test
    fun `char literal`() {
        val result = tokenize("'a'")
        assertEquals(1, result.size)
        assertEquals(tokens.CHAR_LITERAL, result[0].first)
    }

    @Test
    fun `char literal with escape`() {
        val result = tokenize("'\\n'")
        assertEquals(1, result.size)
        assertEquals(tokens.CHAR_LITERAL, result[0].first)
    }

    @Test
    fun `decimal integer`() {
        val result = tokenize("42")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
        assertEquals("42", result[0].second)
    }

    @Test
    fun `hex number`() {
        val result = tokenize("0xFF")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
        assertEquals("0xFF", result[0].second)
    }

    @Test
    fun `binary number`() {
        val result = tokenize("0b1010")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
    }

    @Test
    fun `float number`() {
        val result = tokenize("3.14")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
        assertEquals("3.14", result[0].second)
    }

    @Test
    fun `float with exponent`() {
        val result = tokenize("1.5e-3")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
    }

    @Test
    fun `float with suffix`() {
        val result = tokenize("3.0f")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
        assertEquals("3.0f", result[0].second)
    }

    @Test
    fun `number suffix not consumed if followed by identifier`() {
        val nonWhitespace = tokenize("3float").filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(2, nonWhitespace.size)
        assertEquals(tokens.NUMBER, nonWhitespace[0].first)
        assertEquals("3", nonWhitespace[0].second)
    }

    @Test
    fun `leading dot number`() {
        val result = tokenize(".5")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
        assertEquals(".5", result[0].second)
    }

    @Test
    fun `dot not followed by digit is punctuation`() {
        val nonWs = tokenize("a.b").filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(3, nonWs.size)
        assertEquals(tokens.IDENTIFIER, nonWs[0].first)
        assertEquals(tokens.DOT, nonWs[1].first)
        assertEquals(tokens.IDENTIFIER, nonWs[2].first)
    }

    @Test
    fun `operators`() {
        val text = "+-*/%"
        val nonWs = tokenize(text).filter { it.first != TokenType.WHITE_SPACE }
        assertTrue(nonWs.all { it.first == tokens.OPERATOR })
        assertEquals(5, nonWs.size)
    }

    @Test
    fun `multi-char operators`() {
        val tests = listOf("==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "->", "<<", ">>")
        for (op in tests) {
            val nonWs = tokenize(op).filter { it.first != TokenType.WHITE_SPACE }
            assertEquals(1, nonWs.size, "Expected single token for operator '$op', got: $nonWs")
            assertEquals(tokens.OPERATOR, nonWs[0].first)
            assertEquals(op, nonWs[0].second)
        }
    }

    @Test
    fun `triple angle brackets for CUDA kernel launch`() {
        val nonWs = tokenize("<<<").filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(1, nonWs.size)
        assertEquals(tokens.OPERATOR, nonWs[0].first)
        assertEquals("<<<", nonWs[0].second)
    }

    @Test
    fun `braces and brackets`() {
        val nonWs = tokenize("{}()[]").filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(6, nonWs.size)
        assertEquals(tokens.LBRACE, nonWs[0].first)
        assertEquals(tokens.RBRACE, nonWs[1].first)
        assertEquals(tokens.LPAREN, nonWs[2].first)
        assertEquals(tokens.RPAREN, nonWs[3].first)
        assertEquals(tokens.LBRACKET, nonWs[4].first)
        assertEquals(tokens.RBRACKET, nonWs[5].first)
    }

    @Test
    fun `semicolons and commas`() {
        val nonWs = tokenize(";,").filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(2, nonWs.size)
        assertEquals(tokens.SEMICOLON, nonWs[0].first)
        assertEquals(tokens.COMMA, nonWs[1].first)
    }

    @Test
    fun `identifier`() {
        val result = tokenize("myVariable")
        assertEquals(1, result.size)
        assertEquals(tokens.IDENTIFIER, result[0].first)
        assertEquals("myVariable", result[0].second)
    }

    @Test
    fun `identifier with underscores and digits`() {
        val result = tokenize("_var_123")
        assertEquals(1, result.size)
        assertEquals(tokens.IDENTIFIER, result[0].first)
    }

    @Test
    fun `keyword`() {
        val result = tokenize("return")
        assertEquals(1, result.size)
        assertEquals(tokens.KEYWORD, result[0].first)
    }

    @Test
    fun `complete statement`() {
        val text = "int x = 42;"
        val nonWs = tokenize(text).filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(5, nonWs.size)
        assertEquals(tokens.TYPE_KEYWORD to "int", nonWs[0])
        assertEquals(tokens.IDENTIFIER to "x", nonWs[1])
        assertEquals(tokens.OPERATOR to "=", nonWs[2])
        assertEquals(tokens.NUMBER to "42", nonWs[3])
        assertEquals(tokens.SEMICOLON to ";", nonWs[4])
    }

    @Test
    fun `partial buffer with offset`() {
        val text = "AAAA int x BBBB"
        val lexer = createLexer()
        lexer.start(text, 5, 10, 0)
        // Should tokenize "int x" portion
        val result = mutableListOf<Pair<IElementType?, String>>()
        while (lexer.tokenType != null) {
            result.add(lexer.tokenType to text.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        val nonWs = result.filter { it.first != TokenType.WHITE_SPACE }
        assertEquals(2, nonWs.size)
        assertEquals(tokens.TYPE_KEYWORD to "int", nonWs[0])
        assertEquals(tokens.IDENTIFIER to "x", nonWs[1])
    }

    @Test
    fun `stress test - no infinite loop on random input`() {
        val text = "@#\$%^&*()_+=-{}[]|\\:;<>?,./~`!@#\$%^&*()"
        val lexer = createLexer()
        lexer.start(text, 0, text.length, 0)
        var count = 0
        while (lexer.tokenType != null && count < 1000) {
            assertTrue(lexer.tokenEnd > lexer.tokenStart, "Zero-length token detected")
            lexer.advance()
            count++
        }
        assertNull(lexer.tokenType, "Lexer did not terminate")
    }
}
