/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.common

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement

/**
 * Quick documentation (Ctrl+Q / F1) for GPU builtins, backed by [GpuApiCatalog].
 */
class GpuDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val entry = lookup(element ?: originalElement) ?: lookup(originalElement) ?: return null
        return "<b>${entry.name}</b><br><code>${entry.signature.escapeHtml()}</code><br><br>${entry.doc.escapeHtml()}"
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? =
        lookup(element ?: originalElement)?.let { "${it.name} — ${it.signature}" }

    private fun lookup(element: PsiElement?): ApiEntry? {
        if (element == null) return null
        val word = element.text?.trim() ?: return null
        if (word.isEmpty() || word.length > 64) return null
        return GpuApiCatalog.lookup(element.language.id, word)
    }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
