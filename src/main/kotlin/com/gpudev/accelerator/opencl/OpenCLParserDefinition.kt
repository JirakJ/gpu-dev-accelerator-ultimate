package com.gpudev.accelerator.opencl

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

class OpenCLParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = OpenCLLexer()
    override fun createParser(project: Project?): PsiParser = OpenCLParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = OpenCLTokenTypes.COMMENTS
    override fun getStringLiteralElements(): TokenSet = OpenCLTokenTypes.STRINGS
    override fun createElement(node: ASTNode?): PsiElement = OpenCLPsiElement(node!!)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = OpenCLFile(viewProvider)
    
    companion object {
        val FILE = IFileElementType(OpenCLLanguage)
    }
}

class OpenCLParser : PsiParser {
    override fun parse(root: com.intellij.psi.tree.IElementType, builder: com.intellij.lang.PsiBuilder): ASTNode {
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        return builder.treeBuilt
    }
}

class OpenCLPsiElement(node: ASTNode) : com.intellij.extapi.psi.ASTWrapperPsiElement(node)

class OpenCLFile(viewProvider: FileViewProvider) : com.intellij.extapi.psi.PsiFileBase(viewProvider, OpenCLLanguage) {
    override fun getFileType() = OpenCLFileType.INSTANCE
}
