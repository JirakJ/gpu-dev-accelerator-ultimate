/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.gpudev.accelerator.settings.GpuDevSettings
import java.io.File

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

/**
 * Locates and runs external GPU toolchain binaries off the EDT.
 */
object GpuToolchain {

    fun resolve(configuredPath: String, vararg names: String): String? {
        if (configuredPath.isNotBlank()) {
            val f = File(configuredPath)
            if (f.canExecute()) return f.absolutePath
        }
        for (name in names) {
            val found = PathEnvironmentVariableUtil.findInPath(name)
            if (found != null) return found.absolutePath
        }
        return null
    }

    fun nvcc(): String? = resolve(GpuDevSettings.getInstance().state.nvccPath, "nvcc")
    fun sanitizer(): String? =
        resolve(GpuDevSettings.getInstance().state.sanitizerPath, "compute-sanitizer", "cuda-memcheck")
    fun ncu(): String? = resolve(GpuDevSettings.getInstance().state.ncuPath, "ncu", "nvprof")
    fun nvidiaSmi(): String? = resolve("", "nvidia-smi")

    /** Blocking — call only from a background thread. */
    fun run(command: List<String>, workDir: File? = null): ToolResult {
        val timeoutMs = GpuDevSettings.getInstance().state.toolTimeoutSeconds * 1000
        val cmd = GeneralCommandLine(command)
        if (workDir != null) cmd.workDirectory = workDir
        cmd.charset = Charsets.UTF_8
        return try {
            val output = CapturingProcessHandler(cmd).runProcess(timeoutMs)
            ToolResult(output.exitCode, output.stdout, output.stderr, output.isTimeout)
        } catch (e: Exception) {
            ToolResult(-1, "", e.message ?: "Failed to start ${command.firstOrNull()}", false)
        }
    }

    /** Runs [work] under a background progress task, then [onDone] on the EDT. */
    fun <T> inBackground(project: Project, title: String, work: (ProgressIndicator) -> T, onDone: (T) -> Unit) {
        object : Task.Backgroundable(project, title, true) {
            private var result: T? = null
            override fun run(indicator: ProgressIndicator) {
                result = work(indicator)
            }

            override fun onSuccess() {
                @Suppress("UNCHECKED_CAST")
                ApplicationManager.getApplication().invokeLater { onDone(result as T) }
            }
        }.queue()
    }
}
