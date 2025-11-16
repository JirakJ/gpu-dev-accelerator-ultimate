package com.gpudev.accelerator.opencl

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class OpenCLSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return OpenCLSyntaxHighlighter()
    }
}

class OpenCLSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = OpenCLLexer()
    
    override fun getTokenHighlights(tokenType: IElementType?) = 
        arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
}
