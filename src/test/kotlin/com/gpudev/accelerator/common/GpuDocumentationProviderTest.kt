/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.common

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GpuDocumentationProviderTest {

    private val provider = GpuDocumentationProvider()

    private fun element(text: String?, languageId: String = "CUDA"): PsiElement {
        val language = mockk<Language>()
        every { language.id } returns languageId
        val element = mockk<PsiElement>()
        every { element.text } returns text
        every { element.language } returns language
        return element
    }

    @Test
    fun `documents a catalog entry with escaped signature`() {
        val doc = provider.generateDoc(element("cudaMalloc"), null)!!
        assertTrue(doc.startsWith("<b>cudaMalloc</b>"))
        assertTrue(doc.contains("void** devPtr"))
        assertTrue(doc.contains("Pair with cudaFree"))
    }

    @Test
    fun `html in signatures is escaped`() {
        val doc = provider.generateDoc(element("__global__"), null)!!
        assertTrue(doc.contains("&lt;&lt;&lt;grid, block&gt;&gt;&gt;"))
    }

    @Test
    fun `quick navigate shows name and signature`() {
        assertEquals("cudaFree — cudaError_t cudaFree(void* devPtr)", provider.getQuickNavigateInfo(element(" cudaFree "), null))
    }

    @Test
    fun `unknown, blank, oversized or null elements have no documentation`() {
        assertNull(provider.generateDoc(element("notAnApi"), null))
        assertNull(provider.generateDoc(element("   "), null))
        assertNull(provider.generateDoc(element("x".repeat(65)), null))
        assertNull(provider.generateDoc(element(null), null))
        assertNull(provider.generateDoc(null, null))
        assertNull(provider.generateDoc(element("cudaMalloc", languageId = "JAVA"), null))
    }

    @Test
    fun `falls back to the original element`() {
        assertTrue(provider.generateDoc(null, element("threadIdx"))!!.startsWith("<b>threadIdx</b>"))
    }
}
