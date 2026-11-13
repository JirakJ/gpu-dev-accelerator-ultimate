/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.Key
import com.gpudev.accelerator.settings.GpuDevSettings
import java.io.File

/**
 * Locates and runs external GPU toolchain binaries off the EDT.
 */
object GpuToolchain {

    private val LOG = Logger.getInstance(GpuToolchain::class.java)
    private val executionRouter = ToolExecutionRouter()

    fun nvcc(project: Project): String? = resolve(project, GpuDevSettings.getInstance().state.nvccPath, "nvcc")
    fun nvdisasm(project: Project): String? {
        resolve(project, "", "nvdisasm")?.let { return it }
        val nvccPath = nvcc(project) ?: return null
        val separator = maxOf(nvccPath.lastIndexOf('/'), nvccPath.lastIndexOf('\\'))
        if (separator < 0) return null
        val executable = if (nvccPath.endsWith(".exe", ignoreCase = true)) "nvdisasm.exe" else "nvdisasm"
        return nvccPath.substring(0, separator + 1) + executable
    }
    fun sanitizer(project: Project): String? =
        resolve(project, GpuDevSettings.getInstance().state.sanitizerPath, "compute-sanitizer", "cuda-memcheck")
    fun ncu(project: Project): String? = resolve(project, GpuDevSettings.getInstance().state.ncuPath, "ncu", "nvprof")
    fun nvidiaSmi(project: Project): String? = resolve(project, "", "nvidia-smi")

    private fun resolve(project: Project, configuredPath: String, vararg names: String): String? =
        executor(project).resolve(configuredPath, *names)

    internal fun execute(project: Project, invocation: ToolInvocation): ToolResult {
        val transferFiles = GpuDevSettings.getInstance().state.transferRemoteFiles
        return executor(project).execute(invocation, timeoutMs(), transferFiles)
    }

    fun run(project: Project, command: List<String>, workDir: File? = null): ToolResult =
        execute(project, ToolInvocation(command, workDir))

    fun localPath(project: Project, toolchainPath: String): String? =
        executor(project).localPath(toolchainPath)

    private fun executor(project: Project): ToolExecutionAdapter =
        CachedToolExecutionAdapter(project) { executionRouter.adapter(project) }.get()

    private fun timeoutMs(): Int =
        (GpuDevSettings.getInstance().state.toolTimeoutSeconds * 1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    /** Always runs on the local machine; bypasses the CLion remote toolchain. */
    fun runLocal(command: List<String>, workDir: File? = null): ToolResult =
        executionRouter.localAdapter.execute(ToolInvocation(command, workDir), timeoutMs(), transferFiles = false)

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

            override fun onThrowable(error: Throwable) {
                val message = error.localizedMessage ?: error.javaClass.simpleName
                LOG.warn("Background task failed: $title", error)
                ApplicationManager.getApplication().invokeLater {
                    GpuConsoleService.getInstance(project).print(title, message, error = true)
                    NotificationGroupManager.getInstance().getNotificationGroup("GPU Dev Accelerator")
                        .createNotification("GPU tool operation failed", message, NotificationType.ERROR)
                        .notify(project)
                }
            }
        }.queue()
    }
}

/**
 * Caches the CLion-or-local [ToolExecutionAdapter] for the lifetime of one project.
 *
 * Picking the adapter is expensive on CLion: it reflects through `CPPToolchains`,
 * `RemoteUtil.getCredentials` and friends. Console filters call [GpuToolchain.localPath] for
 * every diagnostic line (synchronously on the EDT), so the choice must not be recomputed per line.
 * The result lives in project user data instead of a static map, so the project — and the adapter
 * holding it — become collectable together; nothing keeps a closed project alive.
 *
 * Invalidation: the adapter is cached until the project is closed. Changing the default CLion
 * toolchain (e.g. switching to a remote SSH toolchain) requires a project reload.
 */
internal class CachedToolExecutionAdapter(
    private val project: Project,
    private val compute: () -> ToolExecutionAdapter
) {
    fun get(): ToolExecutionAdapter = project.getUserData(ADAPTER_KEY) ?: synchronized(LOCK) {
        project.getUserData(ADAPTER_KEY) ?: compute().also { project.putUserData(ADAPTER_KEY, it) }
    }

    private companion object {
        val ADAPTER_KEY: Key<ToolExecutionAdapter> = Key.create("gpudev.execution.adapter")

        /** Stable lock: this wrapper may be allocated per call, so `this` would never contend. */
        val LOCK = Any()
    }
}
