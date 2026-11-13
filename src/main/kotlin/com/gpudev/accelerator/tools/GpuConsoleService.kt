/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

/**
 * Owns the "GPU Dev" tool window console. All output from GPU tool
 * invocations lands here instead of modal dialogs.
 */
@Service(Service.Level.PROJECT)
class GpuConsoleService(private val project: Project) : Disposable {

    private var console: ConsoleView? = null

    fun print(header: String, body: String, error: Boolean = false) {
        val c = obtainConsole()
        if (c == null) {
            LOG.warn("GPU Dev console unavailable (tool window '$TOOL_WINDOW_ID' not found). Dropped: $header")
            return
        }
        c.print("=== $header ===\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        if (body.isNotBlank()) {
            c.print(body.trimEnd() + "\n\n", if (error) ConsoleViewContentType.ERROR_OUTPUT else ConsoleViewContentType.NORMAL_OUTPUT)
        }
        activate()
    }

    private fun obtainConsole(): ConsoleView? {
        console?.let { return it }
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null
        val c = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        c.addMessageFilter(GpuSourceLineFilter(project))
        Disposer.register(this, c)
        val content = ContentFactory.getInstance().createContent(c.component, "Output", false)
        toolWindow.contentManager.addContent(content)
        console = c
        return c
    }

    private fun activate() {
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)?.show()
    }

    override fun dispose() {
        console = null
    }

    companion object {
        private val LOG = Logger.getInstance(GpuConsoleService::class.java)
        const val TOOL_WINDOW_ID = "GPU Dev"
        fun getInstance(project: Project): GpuConsoleService = project.getService(GpuConsoleService::class.java)
    }
}

/**
 * Makes nvcc/compute-sanitizer source references clickable.
 * Matches `path.cu(42): error ...` (nvcc) and `path.cu:42` (gcc-style/sanitizer).
 *
 * [GpuToolchain.localPath] runs once per matching line on the EDT; the remote/local adapter it
 * needs is cached per project (see [CachedToolExecutionAdapter]), so only the path mapping itself
 * is repeated here.
 */
class GpuSourceLineFilter(private val project: Project) : Filter {

    private val patterns = listOf(
        Regex("""(/[^\s():]+\.(?:cu|cuh|metal|cl|c|cpp|cc|h|hpp))\((\d+)\)"""),
        Regex("""(/[^\s():]+\.(?:cu|cuh|metal|cl|c|cpp|cc|h|hpp)):(\d+)""")
    )

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        for (pattern in patterns) {
            val m = pattern.find(line)
            val file = m?.let {
                val localPath = runCatching { GpuToolchain.localPath(project, it.groupValues[1]) }.getOrNull()
                    ?: it.groupValues[1]
                LocalFileSystem.getInstance().findFileByPath(localPath)
            } ?: continue
            val lineNumber = (m.groupValues[2].toIntOrNull() ?: 1) - 1
            val startOffset = entireLength - line.length + m.range.first
            return Filter.Result(
                startOffset,
                startOffset + (m.range.last - m.range.first + 1),
                OpenFileHyperlinkInfo(project, file, lineNumber.coerceAtLeast(0))
            )
        }
        return null
    }
}
