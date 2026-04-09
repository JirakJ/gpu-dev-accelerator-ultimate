package com.gpudev.accelerator.debugger

import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.gpudev.accelerator.run.CudaRunConfiguration
import com.gpudev.accelerator.run.MetalRunConfiguration

/**
 * Debug runner for CUDA and Metal run configurations.
 * Only activates for GPU-specific configs — never intercepts other languages' debug sessions.
 */
class CudaDebugRunner : ProgramRunner<RunnerSettings> {

    private val log = Logger.getInstance(CudaDebugRunner::class.java)

    override fun getRunnerId(): String = "CudaDebugRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
        return profile is CudaRunConfiguration || profile is MetalRunConfiguration
    }

    override fun execute(environment: ExecutionEnvironment) {
        FileDocumentManager.getInstance().saveAllDocuments()
        val state = environment.state ?: return
        val result: ExecutionResult = state.execute(environment.executor, this) ?: return
        val descriptor: RunContentDescriptor = RunContentBuilder(result, environment).showRunContent(environment.contentToReuse)
        log.info("GPU debug session started: ${environment.runProfile.name}")
    }
}
