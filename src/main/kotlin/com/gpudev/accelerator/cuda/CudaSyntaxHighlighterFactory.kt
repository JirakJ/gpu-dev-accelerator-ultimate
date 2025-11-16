package com.gpudev.accelerator.cuda

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

class CudaSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
        return CudaSyntaxHighlighter()
    }
}

class CudaSyntaxHighlighter : SyntaxHighlighter {
    override fun getHighlightingLexer() = CudaLexer()
    
    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return arrayOf(DefaultLanguageHighlighterColors.KEYWORD)
    }
    
    companion object {
        val KEYWORD = TextAttributesKey.createTextAttributesKey(
            "CUDA_KEYWORD",
            DefaultLanguageHighlighterColors.KEYWORD
        )
        val FUNCTION = TextAttributesKey.createTextAttributesKey(
            "CUDA_FUNCTION",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )
        val KERNEL = TextAttributesKey.createTextAttributesKey(
            "CUDA_KERNEL",
            DefaultLanguageHighlighterColors.STATIC_METHOD
        )
    }
}
