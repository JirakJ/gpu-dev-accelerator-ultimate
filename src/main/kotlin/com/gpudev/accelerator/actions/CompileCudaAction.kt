package com.gpudev.accelerator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.gpudev.accelerator.cuda.CudaFileType
import java.io.BufferedReader
import java.io.InputStreamReader

class CompileCudaAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (file.fileType != CudaFileType.INSTANCE) {
            Messages.showErrorDialog(project, "Not a CUDA file", "Compile Error")
            return
        }
        
        compileCudaFile(project, file.path)
    }
    
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabled = file?.fileType == CudaFileType.INSTANCE
    }
    
    private fun compileCudaFile(project: Project, filePath: String) {
        try {
            val process = ProcessBuilder("nvcc", "-c", filePath).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = reader.readText()
            val errors = errorReader.readText()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Messages.showInfoMessage(project, "Compilation successful!", "CUDA Compiler")
            } else {
                Messages.showErrorDialog(project, "Compilation failed:\n$errors", "CUDA Compiler")
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "nvcc not found. Please install CUDA Toolkit and add nvcc to PATH.",
                "Compiler Error"
            )
        }
    }
}
