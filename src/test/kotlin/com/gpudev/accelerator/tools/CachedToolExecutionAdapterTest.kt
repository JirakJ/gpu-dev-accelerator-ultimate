/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CachedToolExecutionAdapterTest {

    @Test
    fun `builds the project adapter once and reuses it across caches`() {
        val project = projectWithUserData()
        var builds = 0
        val remote = FakeAdapter()

        assertSame(remote, CachedToolExecutionAdapter(project) { builds++; remote }.get())
        assertSame(remote, CachedToolExecutionAdapter(project) { builds++; FakeAdapter() }.get())
        assertEquals(1, builds)
    }

    @Test
    fun `keeps distinct projects independent`() {
        val first = FakeAdapter()
        val second = FakeAdapter()

        assertSame(first, CachedToolExecutionAdapter(projectWithUserData()) { first }.get())
        assertSame(second, CachedToolExecutionAdapter(projectWithUserData()) { second }.get())
    }

    private fun projectWithUserData(): Project {
        val store = mutableMapOf<Key<*>, Any?>()
        val project = mockk<Project>(relaxed = true)
        every { project.getUserData(any<Key<ToolExecutionAdapter>>()) } answers {
            store[firstArg<Key<*>>()] as ToolExecutionAdapter?
        }
        every { project.putUserData(any<Key<ToolExecutionAdapter>>(), any()) } answers {
            store[firstArg<Key<*>>()] = secondArg<ToolExecutionAdapter>()
        }
        return project
    }

    private class FakeAdapter : ToolExecutionAdapter {
        override fun resolve(configuredPath: String, vararg names: String): String? = null
        override fun execute(invocation: ToolInvocation, timeoutMs: Int, transferFiles: Boolean): ToolResult =
            ToolResult(0, "", "", false)
        override fun localPath(toolchainPath: String): String = toolchainPath
    }
}
