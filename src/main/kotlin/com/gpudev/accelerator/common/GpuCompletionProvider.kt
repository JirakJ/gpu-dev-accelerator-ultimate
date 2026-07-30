/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.common

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/** Catalog-backed completion with signatures, shared by all GPU languages. */
class GpuCompletionProvider(private val languageId: String) : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        for (entry in GpuApiCatalog.forLanguage(languageId)) {
            result.addElement(
                LookupElementBuilder.create(entry.name)
                    .withTypeText(languageId)
                    .withTailText("  ${entry.signature}", true)
                    .withBoldness(true)
            )
        }
    }
}
