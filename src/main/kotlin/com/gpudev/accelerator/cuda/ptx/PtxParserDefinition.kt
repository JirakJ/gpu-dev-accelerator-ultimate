package com.gpudev.accelerator.cuda.ptx

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
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

class PtxParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = PtxLexer()
    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(root)
        builder.treeBuilt
    }
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = TokenSet.create(PtxTokenTypes.COMMENT)
    override fun getStringLiteralElements(): TokenSet = TokenSet.create(PtxTokenTypes.STRING)
    override fun createElement(node: ASTNode?): PsiElement = PtxPsiElement(node!!)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = PtxFile(viewProvider)

    private companion object {
        val FILE = IFileElementType(PtxLanguage)
    }
}

private class PtxPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)

private class PtxFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, PtxLanguage) {
    override fun getFileType() = PtxFileType.INSTANCE
}
