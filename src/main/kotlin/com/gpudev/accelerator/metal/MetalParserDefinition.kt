package com.gpudev.accelerator.metal

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

class MetalParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = MetalLexer()
    override fun createParser(project: Project?): PsiParser = MetalParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = MetalTokenTypes.COMMENTS
    override fun getStringLiteralElements(): TokenSet = MetalTokenTypes.STRINGS
    override fun createElement(node: ASTNode?): PsiElement = MetalPsiElement(node!!)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = MetalFile(viewProvider)
    
    companion object {
        val FILE = IFileElementType(MetalLanguage)
    }
}

class MetalParser : PsiParser {
    override fun parse(root: com.intellij.psi.tree.IElementType, builder: com.intellij.lang.PsiBuilder): ASTNode {
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        return builder.treeBuilt
    }
}

class MetalPsiElement(node: ASTNode) : com.intellij.extapi.psi.ASTWrapperPsiElement(node)

class MetalFile(viewProvider: FileViewProvider) : com.intellij.extapi.psi.PsiFileBase(viewProvider, MetalLanguage) {
    override fun getFileType() = MetalFileType.INSTANCE
}
