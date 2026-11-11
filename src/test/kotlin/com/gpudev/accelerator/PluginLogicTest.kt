/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator

import com.gpudev.accelerator.analysis.CudaInspectionLogic
import com.gpudev.accelerator.cuda.CudaSyntaxHighlighter
import com.gpudev.accelerator.cuda.CudaTokenTypes
import com.gpudev.accelerator.feedback.FeedbackNotifier
import com.gpudev.accelerator.feedback.FeedbackState
import com.gpudev.accelerator.inspections.CudaKernelOptimizationInspection
import com.gpudev.accelerator.metal.MetalSyntaxHighlighter
import com.gpudev.accelerator.metal.MetalTokenTypes
import com.gpudev.accelerator.opencl.OpenCLSyntaxHighlighter
import com.gpudev.accelerator.opencl.OpenCLTokenTypes
import com.gpudev.accelerator.settings.GpuDevSettings
import com.gpudev.accelerator.tools.ToolResult
import com.intellij.openapi.util.TextRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class PluginLogicTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val start = 1_700_000_000_000L

    @Test
    fun `feedback prompt waits a week and is shown at most once`() {
        val fresh = FeedbackState.State()
        assertFalse(FeedbackNotifier.shouldShowFeedback(fresh, start))
        assertEquals(start, fresh.firstActivation)
        assertFalse(FeedbackNotifier.shouldShowFeedback(FeedbackState.State(start), start + 7 * day - 1))
        assertTrue(FeedbackNotifier.shouldShowFeedback(FeedbackState.State(start), start + 7 * day))
        assertFalse(FeedbackNotifier.shouldShowFeedback(FeedbackState.State(start, feedbackShown = true), start + 30 * day))
        assertFalse(FeedbackNotifier.shouldShowFeedback(FeedbackState.State(start, dismissed = true), start + 30 * day))
    }

    @Test
    fun `notification groups used in code are registered in plugin xml`() {
        val xml = javaClass.getResource("/META-INF/plugin.xml")!!.readText()
        assertTrue(xml.contains("""<notificationGroup id="${FeedbackNotifier.NOTIFICATION_GROUP_ID}""""))
        assertTrue(xml.contains("""<notificationGroup id="GPU Dev Accelerator""""))
        assertFalse(xml.contains("debugger.CudaDebugRunner"))
    }

    @Test
    fun `settings defaults and loadState`() {
        val settings = GpuDevSettings()
        val state = settings.state
        assertEquals("", state.nvccPath)
        assertEquals("macosx", state.metalSdk)
        assertEquals(300, state.toolTimeoutSeconds)
        val loaded = GpuDevSettings.State(nvccPath = "/usr/local/cuda/bin/nvcc", toolTimeoutSeconds = 10)
        settings.loadState(loaded)
        assertSame(loaded, settings.state)
    }

    @Test
    fun `tool result combines streams and reports success`() {
        assertEquals("out\nerr", ToolResult(0, "out", "err", false).combined)
        assertEquals("err", ToolResult(1, "  ", "err", false).combined)
        assertTrue(ToolResult(0, "", "", false).success)
        assertFalse(ToolResult(0, "", "", true).success)
        assertFalse(ToolResult(2, "", "", false).success)
    }

    @Test
    fun `every token type of every language is highlighted`() {
        val cuda = CudaSyntaxHighlighter()
        assertTrue(cuda.getTokenHighlights(CudaTokenTypes.KEYWORD).isNotEmpty())
        assertTrue(cuda.getTokenHighlights(CudaTokenTypes.BLOCK_COMMENT).isNotEmpty())
        assertTrue(cuda.getTokenHighlights(CudaTokenTypes.RBRACKET).isNotEmpty())
        assertTrue(cuda.getTokenHighlights(null).isEmpty())
        assertTrue(MetalSyntaxHighlighter().getTokenHighlights(MetalTokenTypes.IDENTIFIER).isNotEmpty())
        assertTrue(MetalSyntaxHighlighter().getTokenHighlights(MetalTokenTypes.LINE_COMMENT).isNotEmpty())
        assertTrue(OpenCLSyntaxHighlighter().getTokenHighlights(OpenCLTokenTypes.NUMBER).isNotEmpty())
        assertTrue(OpenCLSyntaxHighlighter().getTokenHighlights(OpenCLTokenTypes.PREPROCESSOR).isNotEmpty())
    }

    @Test
    fun `inspection line ranges skip missing and empty lines`() {
        val inspection = CudaKernelOptimizationInspection()
        val text = "a\n\nccc"
        val offsets = inspection.buildLineOffsets(text)
        assertEquals(listOf(0, 2, 3), offsets)
        assertEquals(TextRange(0, 1), inspection.lineRange(text, offsets, 0))
        assertNull(inspection.lineRange(text, offsets, 1))
        assertEquals(TextRange(3, 6), inspection.lineRange(text, offsets, 2))
        assertNull(inspection.lineRange(text, offsets, 3))
        assertNull(inspection.lineRange(text, offsets, -1))
    }

    @Test
    fun `unchecked call detection only treats whole if and return keywords as handled`() {
        // "verify" and "returned" contain "if"/"return" but do not handle the call result.
        val code = "// verify the copy was returned\ncudaMemcpy(dst, src, n, cudaMemcpyHostToDevice);\n"
        assertEquals(1, CudaInspectionLogic.uncheckedCalls(code).size)
        val handled = "if (ok)\ncudaMemcpy(dst, src, n, cudaMemcpyHostToDevice);\n"
        assertTrue(CudaInspectionLogic.uncheckedCalls(handled).isEmpty())
    }
}
