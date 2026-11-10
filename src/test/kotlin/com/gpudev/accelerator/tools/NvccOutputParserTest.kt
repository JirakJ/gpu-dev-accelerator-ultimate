/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NvccOutputParserTest {

    @Test
    fun `parses nvcc style diagnostics`() {
        val output = """
            kernel.cu(12): error: identifier "foo" is undefined
            kernel.cu(20): warning: variable "x" was declared but never referenced
            1 error detected in the compilation of "kernel.cu".
        """.trimIndent()
        val diags = NvccOutputParser.parse(output)
        assertEquals(2, diags.size)
        assertEquals(NvccOutputParser.Severity.ERROR, diags[0].severity)
        assertEquals("kernel.cu", diags[0].file)
        assertEquals(12, diags[0].line)
        assertEquals(NvccOutputParser.Severity.WARNING, diags[1].severity)
        assertEquals(1, NvccOutputParser.errorCount(output))
        assertEquals(1, NvccOutputParser.warningCount(output))
    }

    @Test
    fun `parses gcc style diagnostics from host pass`() {
        val output = "/src/main.cu:33:5: error: expected ';' before 'return'"
        val diags = NvccOutputParser.parse(output)
        assertEquals(1, diags.size)
        assertEquals("/src/main.cu", diags[0].file)
        assertEquals(33, diags[0].line)
    }

    @Test
    fun `clean output has no diagnostics`() {
        assertTrue(NvccOutputParser.parse("").isEmpty())
        assertTrue(NvccOutputParser.parse("ptxas info: 8 registers used").isEmpty())
    }
}
