package com.gpudev.accelerator.debugger

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpointTypeBase
import com.gpudev.accelerator.cuda.CudaFileType

class CudaLineBreakpointType : XLineBreakpointTypeBase(
    "cuda-line",
    "CUDA Line Breakpoints",
    CudaDebuggerEditorsProvider()
) {
    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        return file.fileType == CudaFileType.INSTANCE
    }
}

class CudaDebuggerEditorsProvider : com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider() {
    override fun getFileType() = CudaFileType.INSTANCE
    
    override fun createDocument(
        project: Project,
        expression: com.intellij.xdebugger.XExpression,
        sourcePosition: com.intellij.xdebugger.XSourcePosition?,
        mode: com.intellij.xdebugger.evaluation.EvaluationMode
    ) = com.intellij.openapi.editor.EditorFactory.getInstance().createDocument("")
}
