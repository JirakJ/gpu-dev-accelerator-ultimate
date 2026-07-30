/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.tools

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SanitizerOutputParserTest {

    @Test
    fun `parses error summary and findings`() {
        val output = """
            ========= COMPUTE-SANITIZER
            ========= Invalid __global__ write of size 4 bytes
            =========     at 0x70 in kernel(float*)
            ========= Leaked 1024 bytes at 0x7f0042000000
            ========= ERROR SUMMARY: 2 errors
        """.trimIndent()
        val summary = SanitizerOutputParser.parse(output)
        assertEquals(2, summary.errorCount)
        assertEquals(2, summary.findings.size)
        assertTrue(summary.findings[0].startsWith("Invalid __global__ write"))
    }

    @Test
    fun `clean run reports zero errors`() {
        val output = """
            ========= COMPUTE-SANITIZER
            ========= ERROR SUMMARY: 0 errors
        """.trimIndent()
        val summary = SanitizerOutputParser.parse(output)
        assertEquals(0, summary.errorCount)
        assertTrue(summary.findings.isEmpty())
    }

    @Test
    fun `missing summary yields minus one`() {
        assertEquals(-1, SanitizerOutputParser.parse("garbage output").errorCount)
    }
}
