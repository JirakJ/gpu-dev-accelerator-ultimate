package com.gpudev.accelerator.cuda

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class CudaParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = CudaLexer()
    override fun createParser(project: Project?): PsiParser = CudaParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = CudaTokenTypes.COMMENTS
    override fun getStringLiteralElements(): TokenSet = CudaTokenTypes.STRINGS
    override fun createElement(node: ASTNode?): PsiElement = CudaPsiElement(node!!)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = CudaFile(viewProvider)
    
    companion object {
        val FILE = IFileElementType(CudaLanguage)
    }
}

class CudaParser : PsiParser {
    override fun parse(root: com.intellij.psi.tree.IElementType, builder: com.intellij.lang.PsiBuilder): ASTNode {
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        return builder.treeBuilt
    }
}

class CudaPsiElement(node: ASTNode) : com.intellij.extapi.psi.ASTWrapperPsiElement(node)

class CudaFile(viewProvider: FileViewProvider) : com.intellij.extapi.psi.PsiFileBase(viewProvider, CudaLanguage) {
    override fun getFileType() = CudaFileType.INSTANCE
}
