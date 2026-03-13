package com.gpudev.accelerator.lexer

import com.gpudev.accelerator.opencl.OpenCLLexer
import com.gpudev.accelerator.opencl.OpenCLTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OpenCLLexerTest {

    private val tokens = OpenCLTokenTypes

    private fun tokenize(text: String): List<Pair<IElementType?, String>> {
        val lexer = OpenCLLexer()
        lexer.start(text, 0, text.length, 0)
        val result = mutableListOf<Pair<IElementType?, String>>()
        while (lexer.tokenType != null) {
            result.add(lexer.tokenType to text.substring(lexer.tokenStart, lexer.tokenEnd))
            lexer.advance()
        }
        return result
    }

    private fun nonWs(text: String) = tokenize(text).filter { it.first != TokenType.WHITE_SPACE }

    @Test
    fun `kernel qualifier`() {
        val result = nonWs("__kernel")
        assertEquals(1, result.size)
        assertEquals(tokens.QUALIFIER, result[0].first)
    }

    @Test
    fun `kernel short form qualifier`() {
        val result = nonWs("kernel")
        assertEquals(1, result.size)
        assertEquals(tokens.QUALIFIER, result[0].first)
    }

    @Test
    fun `address space qualifiers`() {
        val qualifiers = listOf("__global", "global", "__local", "local", "__constant", "constant", "__private", "private")
        for (q in qualifiers) {
            val result = nonWs(q)
            assertEquals(1, result.size, "Expected QUALIFIER for '$q'")
            assertEquals(tokens.QUALIFIER, result[0].first, "Wrong type for '$q'")
        }
    }

    @Test
    fun `OpenCL vector types`() {
        val types = listOf("float4", "int2", "uchar16", "half4", "double2")
        for (t in types) {
            val result = nonWs(t)
            assertEquals(1, result.size, "Expected TYPE_KEYWORD for '$t'")
            assertEquals(tokens.TYPE_KEYWORD, result[0].first, "Wrong type for '$t'")
        }
    }

    @Test
    fun `OpenCL image types`() {
        val types = listOf("image2d_t", "image3d_t", "sampler_t")
        for (t in types) {
            val result = nonWs(t)
            assertEquals(tokens.TYPE_KEYWORD, result[0].first, "Wrong type for '$t'")
        }
    }

    @Test
    fun `builtin work item functions`() {
        val builtins = listOf("get_global_id", "get_local_id", "get_group_id", "get_global_size", "get_local_size")
        for (fn in builtins) {
            val result = nonWs(fn)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$fn'")
        }
    }

    @Test
    fun `sync functions`() {
        assertEquals(tokens.BUILTIN_FUNCTION, nonWs("barrier")[0].first)
        assertEquals(tokens.QUALIFIER, nonWs("CLK_LOCAL_MEM_FENCE")[0].first)
    }

    @Test
    fun `full OpenCL kernel function`() {
        val code = """
            __kernel void vector_add(
                __global const float* a,
                __global const float* b,
                __global float* result)
            {
                int gid = get_global_id(0);
                result[gid] = a[gid] + b[gid];
            }
        """.trimIndent()

        val lexer = OpenCLLexer()
        lexer.start(code, 0, code.length, 0)

        // Verify complete tokenization with no gaps
        var lastEnd = 0
        var tokenCount = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart, "Gap at position $lastEnd")
            assertTrue(lexer.tokenEnd > lexer.tokenStart, "Zero-length token at ${lexer.tokenStart}")
            lastEnd = lexer.tokenEnd
            tokenCount++
            lexer.advance()
        }
        assertEquals(code.length, lastEnd, "Lexer didn't consume all input")
        assertTrue(tokenCount > 20, "Too few tokens for a full kernel function")
    }

    @Test
    fun `user reported crash scenario - file opens without error`() {
        // This is the core regression test: the old lexer returned null from getTokenType()
        // immediately, causing "Unexpected termination offset" crash
        val code = """
            #include <opencl.h>
            
            __kernel void my_kernel(__global float* data, int count) {
                int idx = get_global_id(0);
                if (idx < count) {
                    data[idx] = data[idx] * 2.0f;
                }
            }
        """.trimIndent()

        val lexer = OpenCLLexer()
        lexer.start(code, 0, code.length, 0)

        // The old lexer would return null immediately with tokenStart != bufferEnd
        // causing LexerEditorHighlighter.doSetText to throw IllegalStateException
        assertNotNull(lexer.tokenType, "Lexer must return a token for non-empty input")

        // Verify it fully tokenizes
        var lastEnd = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart)
            lastEnd = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(code.length, lastEnd)
    }

    @Test
    fun `atomic functions recognized`() {
        val fns = listOf("atomic_add", "atomic_cmpxchg", "atom_add")
        for (fn in fns) {
            val result = nonWs(fn)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$fn'")
        }
    }

    @Test
    fun `C keywords recognized`() {
        val kws = listOf("if", "else", "for", "while", "return", "struct", "typedef", "void")
        for (kw in kws) {
            val result = nonWs(kw)
            assertEquals(tokens.KEYWORD, result[0].first, "Wrong type for '$kw'")
        }
    }

    @Test
    fun `access qualifiers`() {
        val qualifiers = listOf("__read_only", "__write_only", "__read_write")
        for (q in qualifiers) {
            val result = nonWs(q)
            assertEquals(tokens.QUALIFIER, result[0].first, "Wrong type for '$q'")
        }
    }
}
