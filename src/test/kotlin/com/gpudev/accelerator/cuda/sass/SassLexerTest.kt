package com.gpudev.accelerator.cuda.sass

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SassLexerTest {
    @Test
    fun `highlights SASS addresses instructions registers predicates and labels`() {
        val text = """
            .text.kernel:
            //## File "/remote/workspace/kernel.cu", line 4
            /*0000*/ @P0 LDG.E R1, [R2.64] ;
            .L_exit:
            /*0010*/ EXIT ;
        """.trimIndent()
        val tokens = tokenize(text)

        assertTrue(tokens.any { it.first == SassTokenTypes.LABEL && it.second == ".text.kernel" })
        assertTrue(tokens.any { it.first == SassTokenTypes.COMMENT && it.second.startsWith("//## File") })
        assertTrue(tokens.any { it.first == SassTokenTypes.ADDRESS && it.second == "/*0000*/" })
        assertTrue(tokens.any { it.first == SassTokenTypes.PREDICATE && it.second == "@P0" })
        assertTrue(tokens.any { it.first == SassTokenTypes.INSTRUCTION && it.second == "LDG.E" })
        assertTrue(tokens.any { it.first == SassTokenTypes.REGISTER && it.second == "R1" })
        assertTrue(tokens.any { it.first == SassTokenTypes.LABEL && it.second == ".L_exit" })
        assertTrue(SassSyntaxHighlighter().getTokenHighlights(SassTokenTypes.INSTRUCTION).isNotEmpty())
    }

    @Test
    fun `lexer accounts for all characters`() {
        val text = "/*0000*/ MOV R1, c[0x0][0x28] ;\n"
        val lexer = SassLexer()
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

    private fun tokenize(text: String): List<Pair<IElementType, String>> {
        val lexer = SassLexer()
        lexer.start(text, 0, text.length, 0)
        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            if (lexer.tokenType != TokenType.WHITE_SPACE) {
                tokens.add(lexer.tokenType!! to text.substring(lexer.tokenStart, lexer.tokenEnd))
            }
            lexer.advance()
        }
        return tokens
    }
}
