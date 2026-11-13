/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class RemoteToolExecutionAdapterTest {

    @Test
    fun `maps paths transfers artifacts and removes temporary remote output`() {
        val source = Files.createTempFile("gpudev-source-", ".cu").toFile()
        val output = Files.createTempFile("gpudev-output-", ".ptx").toFile()
        val transport = RecordingTransport()
        try {
            val command = listOf("nvcc", "-ptx", source.path, "-o", output.path)
            val invocation = ToolInvocation(
                command,
                source.parentFile,
                listOf(
                    ToolArtifact(source, ToolArtifactKind.INPUT),
                    ToolArtifact(output, ToolArtifactKind.TEMPORARY_OUTPUT)
                )
            )

            val result = RemoteToolExecutionAdapter(transport).execute(invocation, 2_000, transferFiles = true)

            assertTrue(result.success)
            assertEquals(
                listOf("nvcc", "-ptx", transport.remote(source), "-o", transport.remoteTemporaryFile),
                transport.command
            )
            assertEquals(transport.remote(source.parentFile), transport.workDirectory)
            assertEquals(listOf(transport.remote(source)), transport.uploaded)
            assertEquals(listOf(transport.remoteTemporaryFile), transport.downloaded)
            assertEquals(listOf(transport.remoteTemporaryFile), transport.deleted)
            assertEquals("downloaded artifact", output.readText())
        } finally {
            source.delete()
            output.delete()
        }
    }

    @Test
    fun `temporary output is isolated from a shared project filesystem`() {
        val source = Files.createTempFile("gpudev-source-", ".cu").toFile()
        val output = Files.createTempFile("gpudev-output-", ".ptx").toFile()
        val transport = RecordingTransport().apply { transferNeeded = false }
        try {
            val invocation = ToolInvocation(
                listOf("nvcc", "-ptx", source.path, "-o", output.path),
                artifacts = listOf(
                    ToolArtifact(source, ToolArtifactKind.INPUT),
                    ToolArtifact(output, ToolArtifactKind.TEMPORARY_OUTPUT)
                )
            )

            val result = RemoteToolExecutionAdapter(transport).execute(invocation, 2_000, transferFiles = true)

            assertTrue(result.success)
            assertTrue(transport.uploaded.isEmpty())
            assertEquals(listOf(transport.remoteTemporaryFile), transport.downloaded)
            assertEquals(listOf(transport.remoteTemporaryFile), transport.deleted)
        } finally {
            source.delete()
            output.delete()
        }
    }

    @Test
    fun `does not transfer project artifacts unless explicitly enabled`() {
        val source = Files.createTempFile("gpudev-source-", ".cu").toFile()
        val output = File("${source.path}.o")
        val transport = RecordingTransport()
        try {
            val invocation = ToolInvocation(
                listOf("nvcc", "-c", source.path, "-o", output.path),
                artifacts = listOf(
                    ToolArtifact(source, ToolArtifactKind.INPUT),
                    ToolArtifact(output, ToolArtifactKind.OUTPUT)
                )
            )

            val result = RemoteToolExecutionAdapter(transport).execute(invocation, 2_000, transferFiles = false)

            assertTrue(result.success)
            assertTrue(transport.uploaded.isEmpty())
            assertTrue(transport.downloaded.isEmpty())
        } finally {
            source.delete()
            output.delete()
        }
    }

    @Test
    fun `cleans remote temporary output when command fails`() {
        val output = Files.createTempFile("gpudev-output-", ".ptx").toFile()
        val transport = RecordingTransport().apply { result = ToolResult(1, "", "nvcc failed", false) }
        try {
            val invocation = ToolInvocation(
                listOf("nvcc", "-o", output.path),
                artifacts = listOf(ToolArtifact(output, ToolArtifactKind.TEMPORARY_OUTPUT))
            )

            val result = RemoteToolExecutionAdapter(transport).execute(invocation, 2_000, transferFiles = true)

            assertFalse(result.success)
            assertTrue(transport.downloaded.isEmpty())
            assertEquals(listOf(transport.remoteTemporaryFile), transport.deleted)
        } finally {
            output.delete()
        }
    }

    @Test
    fun `does not upload or download mapped paths when CLion says storage is shared`() {
        val source = Files.createTempFile("gpudev-source-", ".cu").toFile()
        val output = File("${source.path}.o")
        val transport = RecordingTransport().apply { transferNeeded = false }
        try {
            val invocation = ToolInvocation(
                listOf("nvcc", "-c", source.path, "-I", source.parent, "-o", output.path),
                artifacts = listOf(
                    ToolArtifact(source, ToolArtifactKind.INPUT),
                    ToolArtifact(output, ToolArtifactKind.OUTPUT),
                    ToolArtifact(source.parentFile, ToolArtifactKind.PATH_REFERENCE)
                )
            )

            val result = RemoteToolExecutionAdapter(transport).execute(invocation, 2_000, transferFiles = true)

            assertTrue(result.success)
            assertTrue(transport.uploaded.isEmpty())
            assertTrue(transport.downloaded.isEmpty())
            assertEquals(
                listOf(
                    "nvcc",
                    "-c",
                    transport.remote(source),
                    "-I",
                    transport.remote(source.parentFile),
                    "-o",
                    transport.remote(output)
                ),
                transport.command
            )
        } finally {
            source.delete()
            output.delete()
        }
    }

    private class RecordingTransport : RemoteToolTransport {
        var command: List<String> = emptyList()
        var workDirectory: String? = null
        var result = ToolResult(0, "", "", false)
        var transferNeeded = true
        val remoteTemporaryFile = "/remote/tmp/gpudev-test.ptx"
        val uploaded = mutableListOf<String>()
        val downloaded = mutableListOf<String>()
        val deleted = mutableListOf<String>()

        fun remote(file: File): String = remote(file.absolutePath)
        fun remote(path: String): String = "/remote$path"

        override fun resolve(configuredPath: String, vararg names: String): String? = null

        override fun run(command: List<String>, workDirectory: String?, timeoutMs: Int): ToolResult {
            this.command = command
            this.workDirectory = workDirectory
            return result
        }

        override fun toRemotePath(localPath: String): String = remote(localPath)
        override fun toLocalPath(remotePath: String): String? = remotePath.removePrefix("/remote")
        override fun createTemporaryFile(): String = remoteTemporaryFile
        override fun needsTransfer(localPath: String, remotePath: String): Boolean = transferNeeded
        override fun upload(localPath: String, remotePath: String) { uploaded.add(remotePath) }
        override fun download(remotePath: String, localPath: String) {
            downloaded.add(remotePath)
            File(localPath).writeText("downloaded artifact")
        }
        override fun delete(remotePath: String, timeoutMs: Int) { deleted.add(remotePath) }
    }
}
