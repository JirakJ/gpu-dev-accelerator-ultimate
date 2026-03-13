package com.gpudev.accelerator.lexer

import com.gpudev.accelerator.cuda.CudaLexer
import com.gpudev.accelerator.cuda.CudaTokenTypes
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CudaLexerTest {

    private val tokens = CudaTokenTypes

    private fun tokenize(text: String): List<Pair<IElementType?, String>> {
        val lexer = CudaLexer()
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
    fun `CUDA execution space qualifiers`() {
        val qualifiers = listOf("__global__", "__device__", "__host__", "__shared__", "__constant__")
        for (q in qualifiers) {
            val result = nonWs(q)
            assertEquals(1, result.size, "Expected single token for '$q'")
            assertEquals(tokens.QUALIFIER, result[0].first, "Wrong type for '$q'")
        }
    }

    @Test
    fun `CUDA builtin variables`() {
        val vars = listOf("threadIdx", "blockIdx", "blockDim", "gridDim", "warpSize")
        for (v in vars) {
            val result = nonWs(v)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$v'")
        }
    }

    @Test
    fun `CUDA API functions`() {
        val fns = listOf("cudaMalloc", "cudaFree", "cudaMemcpy", "cudaDeviceSynchronize")
        for (fn in fns) {
            val result = nonWs(fn)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$fn'")
        }
    }

    @Test
    fun `CUDA sync functions`() {
        val fns = listOf("__syncthreads", "__syncwarp", "__threadfence")
        for (fn in fns) {
            val result = nonWs(fn)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$fn'")
        }
    }

    @Test
    fun `CUDA types`() {
        val types = listOf("dim3", "cudaStream_t", "cudaError_t", "float4", "int2")
        for (t in types) {
            val result = nonWs(t)
            assertEquals(tokens.TYPE_KEYWORD, result[0].first, "Wrong type for '$t'")
        }
    }

    @Test
    fun `kernel launch syntax triple brackets`() {
        val nonWs = nonWs("<<<")
        assertEquals(1, nonWs.size)
        assertEquals(tokens.OPERATOR, nonWs[0].first)
        assertEquals("<<<", nonWs[0].second)

        val nonWs2 = nonWs(">>>")
        assertEquals(1, nonWs2.size)
        assertEquals(tokens.OPERATOR, nonWs2[0].first)
        assertEquals(">>>", nonWs2[0].second)
    }

    @Test
    fun `full CUDA kernel`() {
        val code = """
            __global__ void vectorAdd(const float *A, const float *B, float *C, int N) {
                int i = blockDim.x * blockIdx.x + threadIdx.x;
                if (i < N) {
                    C[i] = A[i] + B[i];
                }
            }
        """.trimIndent()

        val lexer = CudaLexer()
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
        assertEquals(code.length, lastEnd)
        assertTrue(tokenCount > 20)
    }

    @Test
    fun `CUDA kernel launch expression`() {
        val code = "myKernel<<<grid, block>>>(args)"
        val nonWs = nonWs(code)
        assertTrue(nonWs.any { it.second == "<<<" && it.first == tokens.OPERATOR })
        assertTrue(nonWs.any { it.second == ">>>" && it.first == tokens.OPERATOR })
    }

    @Test
    fun `C++ keywords recognized`() {
        val kws = listOf("class", "template", "namespace", "virtual", "nullptr", "constexpr")
        for (kw in kws) {
            val result = nonWs(kw)
            assertEquals(tokens.KEYWORD, result[0].first, "Wrong type for '$kw'")
        }
    }

    @Test
    fun `atomic functions`() {
        val fns = listOf("atomicAdd", "atomicSub", "atomicCAS", "atomicExch")
        for (fn in fns) {
            val result = nonWs(fn)
            assertEquals(tokens.BUILTIN_FUNCTION, result[0].first, "Wrong type for '$fn'")
        }
    }

    @Test
    fun `crash regression - non-empty input always produces tokens`() {
        val code = "#include <cuda_runtime.h>\nint main() { return 0; }"
        val lexer = CudaLexer()
        lexer.start(code, 0, code.length, 0)
        assertNotNull(lexer.tokenType)
        var lastEnd = 0
        while (lexer.tokenType != null) {
            assertEquals(lastEnd, lexer.tokenStart)
            lastEnd = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals(code.length, lastEnd)
    }
}
