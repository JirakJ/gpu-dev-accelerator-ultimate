package com.gpudev.accelerator.debugger

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

class CudaDebugRunner : GenericProgramRunner<com.intellij.execution.configurations.RunnerSettings>() {
    
    override fun getRunnerId(): String = "CudaDebugRunner"
    
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return true
    }
    
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        FileDocumentManager.getInstance().saveAllDocuments()
        return super.doExecute(state, environment)
    }
}
