/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import java.io.File
import java.lang.reflect.InvocationTargetException

data class ToolResult(val exitCode: Int, val stdout: String, val stderr: String, val timedOut: Boolean) {
    val combined: String get() = buildString {
        if (stdout.isNotBlank()) append(stdout)
        if (stderr.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append(stderr)
        }
    }
    val success: Boolean get() = exitCode == 0 && !timedOut
}

internal enum class ToolArtifactKind { INPUT, OUTPUT, TEMPORARY_OUTPUT, PATH_REFERENCE }

internal data class ToolArtifact(val file: File, val kind: ToolArtifactKind)

/**
 * File paths in [command] must match declared [artifacts] exactly. Remote transfers for project
 * files are opt-in; temporary outputs are always retrieved and removed. The caller owns local files.
 */
internal data class ToolInvocation(
    val command: List<String>,
    val workDir: File? = null,
    val artifacts: List<ToolArtifact> = emptyList()
)

/** Resolves tools and executes invocations for one machine and filesystem. */
internal interface ToolExecutionAdapter {
    fun resolve(configuredPath: String, vararg names: String): String?
    fun execute(invocation: ToolInvocation, timeoutMs: Int, transferFiles: Boolean): ToolResult
    fun localPath(toolchainPath: String): String?
}

internal interface RemoteToolTransport {
    fun resolve(configuredPath: String, vararg names: String): String?
    fun run(command: List<String>, workDirectory: String?, timeoutMs: Int): ToolResult
    fun toRemotePath(localPath: String): String
    fun toLocalPath(remotePath: String): String?
    fun createTemporaryFile(): String
    fun needsTransfer(localPath: String, remotePath: String): Boolean
    fun upload(localPath: String, remotePath: String)
    fun download(remotePath: String, localPath: String)
    fun delete(remotePath: String, timeoutMs: Int)
}

internal class RemoteToolExecutionAdapter(private val transport: RemoteToolTransport) : ToolExecutionAdapter {
    override fun resolve(configuredPath: String, vararg names: String): String? =
        transport.resolve(configuredPath, *names)

    override fun execute(invocation: ToolInvocation, timeoutMs: Int, transferFiles: Boolean): ToolResult {
        val staged = try {
            stage(invocation, timeoutMs)
        } catch (e: Exception) {
            return failure(e)
        }
        return try {
            uploadInputs(invocation, staged.paths, transferFiles)
            val command = invocation.command.map { staged.paths[it] ?: it }
            val workDirectory = invocation.workDir?.let { transport.toRemotePath(it.absolutePath) }
            val result = transport.run(command, workDirectory, timeoutMs)
            if (result.success) retrieveOutputs(invocation, staged.paths, transferFiles)
            result
        } catch (e: Exception) {
            failure(e)
        } finally {
            cleanup(staged.temporaryPaths, timeoutMs)
        }
    }

    override fun localPath(toolchainPath: String): String? = transport.toLocalPath(toolchainPath)

    private fun stage(invocation: ToolInvocation, timeoutMs: Int): StagedArtifacts {
        val paths = mutableMapOf<String, String>()
        val temporaryPaths = mutableListOf<String>()
        try {
            invocation.artifacts.forEach { artifact ->
                val localPath = artifact.file.absolutePath
                val remotePath = if (artifact.kind == ToolArtifactKind.TEMPORARY_OUTPUT) {
                    transport.createTemporaryFile()
                } else {
                    transport.toRemotePath(localPath)
                }
                paths[artifact.file.path] = remotePath
                paths[localPath] = remotePath
                if (artifact.kind == ToolArtifactKind.TEMPORARY_OUTPUT) temporaryPaths.add(remotePath)
            }
            return StagedArtifacts(paths, temporaryPaths)
        } catch (e: Exception) {
            cleanup(temporaryPaths, timeoutMs)
            throw e
        }
    }

    private fun uploadInputs(invocation: ToolInvocation, paths: Map<String, String>, transferFiles: Boolean) {
        invocation.artifacts.filter { it.kind == ToolArtifactKind.INPUT }.forEach { artifact ->
            val remotePath = paths.getValue(artifact.file.path)
            if (transferFiles && transport.needsTransfer(artifact.file.absolutePath, remotePath)) {
                transport.upload(artifact.file.absolutePath, remotePath)
            }
        }
    }

    private fun retrieveOutputs(invocation: ToolInvocation, paths: Map<String, String>, transferFiles: Boolean) {
        invocation.artifacts
            .filter { it.kind == ToolArtifactKind.OUTPUT || it.kind == ToolArtifactKind.TEMPORARY_OUTPUT }
            .forEach { artifact ->
                val remotePath = paths.getValue(artifact.file.path)
                if (artifact.kind == ToolArtifactKind.TEMPORARY_OUTPUT ||
                    (transferFiles && transport.needsTransfer(artifact.file.absolutePath, remotePath))
                ) {
                    transport.download(remotePath, artifact.file.absolutePath)
                }
            }
    }

    private fun cleanup(paths: List<String>, timeoutMs: Int) {
        paths.forEach { path -> runCatching { transport.delete(path, timeoutMs) } }
    }

    private fun failure(error: Exception): ToolResult =
        ToolResult(-1, "", error.rootCause().message ?: "Remote command failed", false)

    private fun Throwable.rootCause(): Throwable {
        if (this !is InvocationTargetException) return this
        return targetException?.rootCause() ?: this
    }

    private data class StagedArtifacts(val paths: Map<String, String>, val temporaryPaths: List<String>)
}

internal class ToolExecutionRouter(
    internal val localAdapter: ToolExecutionAdapter = LocalToolExecutionAdapter(),
    private val remoteForProject: (Project) -> ToolExecutionAdapter? = { ClionRemoteToolchain.current(it) }
) {
    fun adapter(project: Project): ToolExecutionAdapter = remoteForProject(project) ?: localAdapter
}

internal class LocalToolExecutionAdapter : ToolExecutionAdapter {
    override fun resolve(configuredPath: String, vararg names: String): String? {
        if (configuredPath.isNotBlank()) {
            val file = File(configuredPath)
            if (file.canExecute()) return file.absolutePath
        }
        for (name in names) {
            val found = PathEnvironmentVariableUtil.findInPath(name)
            if (found != null) return found.absolutePath
        }
        return null
    }

    override fun execute(invocation: ToolInvocation, timeoutMs: Int, transferFiles: Boolean): ToolResult {
        val commandLine = GeneralCommandLine(invocation.command)
        invocation.workDir?.let { commandLine.workDirectory = it }
        commandLine.charset = Charsets.UTF_8
        return try {
            val output = CapturingProcessHandler(commandLine).runProcess(timeoutMs)
            ToolResult(output.exitCode, output.stdout, output.stderr, output.isTimeout)
        } catch (e: Exception) {
            ToolResult(-1, "", e.message ?: "Failed to start ${invocation.command.firstOrNull()}", false)
        }
    }

    override fun localPath(toolchainPath: String): String = toolchainPath
}
