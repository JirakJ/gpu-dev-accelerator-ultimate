package com.gpudev.accelerator.lexer

import com.gpudev.accelerator.metal.MetalLexer
import com.gpudev.accelerator.metal.MetalTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MetalLexerTest {

    private val tokens = MetalTokenTypes

    private fun tokenize(text: String): List<Pair<IElementType?, String>> {
        val lexer = MetalLexer()
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
    fun `Metal function qualifiers`() {
        val qualifiers = listOf("kernel", "vertex", "fragment", "compute")
        for (q in qualifiers) {
            val result = nonWs(q)
            assertEquals(1, result.size, "Expected single token for '$q'")
            assertEquals(tokens.QUALIFIER, result[0].first, "Wrong type for '$q'")
        }
    }

    @Test
    fun `Metal address space qualifiers`() {
        val qualifiers = listOf("device", "constant", "threadgroup", "thread")
        for (q in qualifiers) {
            val result = nonWs(q)
            assertEquals(tokens.QUALIFIER, result[0].first, "Wrong type for '$q'")
        }
    }

    @Test
    fun `Metal attribute qualifiers`() {
        val qualifiers = listOf("thread_position_in_grid", "stage_in", "position")
        for (q in qualifiers) {
            val result = nonWs(q)
            assertEquals(tokens.QUALIFIER, result[0].first, "Wrong type for '$q'")
        }
    }

    @Test
    fun `Metal vector types`() {
        val types = listOf("float2", "float3", "float4", "half4", "int4", "uint2")
        for (t in types) {
            val result = nonWs(t)
            assertEquals(tokens.TYPE_KEYWORD, result[0].first, "Wrong type for '$t'")
        }
    }

    @Test
    fun `Metal matrix types`() {
        val types = listOf("float4x4", "float3x3", "half4x4")
        for (t in types) {
            val result = nonWs(t)
            assertEquals(tokens.TYPE_KEYWORD, result[0].first, "Wrong type for '$t'")
        }
    }

    @Test
    fun `Metal texture types`() {
        val types = listOf("texture2d", "texture3d", "depth2d", "sampler")
        for (t in types) {
            val result = nonWs(t)
            assertEquals(tokens.TYPE_KEYWORD, result[0].first, "Wrong type for '$t'")
        }
    }

    @Test
    fun `Metal builtin functions`() {
        val fns = listOf("normalize", "dot", "cross", "length", "saturate", "smoothstep")
        for (fn in fns) {
            val result = nonWs(fn)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$fn'")
        }
    }

    @Test
    fun `Metal sync functions`() {
        assertEquals(tokens.BUILTIN_FUNCTION, nonWs("threadgroup_barrier")[0].first)
        assertEquals(tokens.BUILTIN_FUNCTION, nonWs("simdgroup_barrier")[0].first)
    }

    @Test
    fun `full Metal kernel function`() {
        val code = """
            #include <metal_stdlib>
            using namespace metal;
            
            kernel void my_kernel(
                device const float* input [[buffer(0)]],
                device float* output [[buffer(1)]],
                uint id [[thread_position_in_grid]]
            ) {
                output[id] = input[id] * 2.0;
            }
        """.trimIndent()

        val lexer = MetalLexer()
        lexer.start(code, 0, code.length, 0)

        var lastEnd = 0
        var tokenCount = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart, "Gap at position $lastEnd")
            assertTrue(lexer.tokenEnd > lexer.tokenStart)
            lastEnd = lexer.tokenEnd
            tokenCount++
            lexer.advance()
        }
        assertEquals(code.length, lastEnd, "Lexer didn't consume all input")
        assertTrue(tokenCount > 30)
    }

    @Test
    fun `Metal binding index reuse across functions - no crash`() {
        // This is the exact user-reported Metal Shader Support bug scenario.
        // Different functions reusing buffer/texture indices is VALID Metal code.
        val code = """
            kernel void kernel_a(
                device const float* input [[buffer(0)]],
                device float* output [[buffer(1)]],
                uint id [[thread_position_in_grid]]
            ) {
                output[id] = input[id] * 2.0;
            }
            
            kernel void kernel_b(
                device const float* input [[buffer(0)]],
                device float* output [[buffer(1)]],
                uint id [[thread_position_in_grid]]
            ) {
                output[id] = input[id] + 1.0;
            }
        """.trimIndent()

        val lexer = MetalLexer()
        lexer.start(code, 0, code.length, 0)

        var lastEnd = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart)
            lastEnd = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(code.length, lastEnd)
    }

    @Test
    fun `Metal attribute bracket syntax`() {
        // [[buffer(0)]] should tokenize as [ [ identifier ( number ) ] ]
        val nonWs = nonWs("[[buffer(0)]]")
        assertTrue(nonWs.size >= 7, "Expected at least 7 tokens, got: $nonWs")
        assertEquals(tokens.LBRACKET, nonWs[0].first)
        assertEquals(tokens.LBRACKET, nonWs[1].first)
    }

    @Test
    fun `half suffix on numbers`() {
        val result = nonWs("0.5h")
        assertEquals(1, result.size)
        assertEquals(tokens.NUMBER, result[0].first)
        assertEquals("0.5h", result[0].second)
    }

    @Test
    fun `C++ keywords recognized`() {
        val kws = listOf("class", "struct", "namespace", "using", "template")
        for (kw in kws) {
            val result = nonWs(kw)
            assertEquals(tokens.KEYWORD, result[0].first, "Wrong type for '$kw'")
        }
    }

    @Test
    fun `crash regression - non-empty input always produces tokens`() {
        val code = "#include <metal_stdlib>\nusing namespace metal;"
        val lexer = MetalLexer()
        lexer.start(code, 0, code.length, 0)
        assertNotNull(lexer.tokenType, "Lexer must produce tokens for non-empty input")
        var lastEnd = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart)
            lastEnd = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(code.length, lastEnd)
    }
}
