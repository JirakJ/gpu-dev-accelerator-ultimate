package com.gpudev.accelerator.cuda.ptx

import com.intellij.psi.TokenType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PtxLexerTest {
    @Test
    fun `highlights common PTX syntax categories and tolerates unknown opcodes`() {
        val text = """
            .version 8.0
            // generated PTX
            L_entry:
            @%p1 add.u32 %r1, %r2, 42;
            mystery_opcode %r3;
        """.trimIndent()
        val tokens = tokenize(text)

        assertTrue(tokens.any { it.first == PtxTokenTypes.DIRECTIVE && it.second == ".version" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.COMMENT && it.second.startsWith("//") })
        assertTrue(tokens.any { it.first == PtxTokenTypes.LABEL && it.second == "L_entry" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.PREDICATE && it.second == "@" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.PREDICATE && it.second == "%p1" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.INSTRUCTION && it.second == "add" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.REGISTER && it.second == "%r1" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.NUMBER && it.second == "42" })
        assertTrue(tokens.any { it.first == PtxTokenTypes.INSTRUCTION && it.second == "mystery_opcode" })
        assertTrue(PtxSyntaxHighlighter().getTokenHighlights(PtxTokenTypes.INSTRUCTION).isNotEmpty())
    }

    @Test
    fun `lexer accounts for all characters`() {
        val text = ".file 1 \"kernel.cu\"\n.loc 1 4 0\nld.global.u32 %r1, [%rd2];\n"
        val lexer = PtxLexer()
        lexer.start(text, 0, text.length, 0)
        var offset = 0
        while (lexer.tokenType != null) {
            assertEquals(offset, lexer.tokenStart)
            assertTrue(lexer.tokenEnd > lexer.tokenStart)
            offset = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(text.length, offset)
    }

    private fun tokenize(text: String): List<Pair<com.intellij.psi.tree.IElementType, String>> {
        val lexer = PtxLexer()
        lexer.start(text, 0, text.length, 0)
        val tokens = mutableListOf<Pair<com.intellij.psi.tree.IElementType, String>>()
        while (lexer.tokenType != null) {
            if (lexer.tokenType != TokenType.WHITE_SPACE) {
                tokens.add(lexer.tokenType!! to text.substring(lexer.tokenStart, lexer.tokenEnd))
            }
            lexer.advance()
        }
        return tokens
    }
}
