/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.openapi.project.Project
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.File

class ToolExecutionRouterTest {

    @Test
    fun `selects CLion adapter when present and keeps local fallback`() {
        val local = FakeAdapter()
        val remote = FakeAdapter()
        val project = mockk<Project>()

        assertSame(remote, ToolExecutionRouter(local) { remote }.adapter(project))
        assertSame(local, ToolExecutionRouter(local) { null }.adapter(project))
    }

    private class FakeAdapter : ToolExecutionAdapter {
        override fun resolve(configuredPath: String, vararg names: String): String? = null
        override fun execute(invocation: ToolInvocation, timeoutMs: Int, transferFiles: Boolean): ToolResult =
            ToolResult(0, "", "", false)
        override fun localPath(toolchainPath: String): String = toolchainPath
    }
}
