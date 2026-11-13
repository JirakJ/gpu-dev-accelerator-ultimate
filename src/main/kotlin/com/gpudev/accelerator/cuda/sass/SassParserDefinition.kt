package com.gpudev.accelerator.cuda.sass

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

class SassParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = SassLexer()
    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(root)
        builder.treeBuilt
    }
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = TokenSet.create(SassTokenTypes.COMMENT)
    override fun getStringLiteralElements(): TokenSet = TokenSet.create(SassTokenTypes.STRING)
    override fun createElement(node: ASTNode?): PsiElement = SassPsiElement(node!!)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = SassFile(viewProvider)

    private companion object {
        val FILE = IFileElementType(SassLanguage)
    }
}

private class SassPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)

private class SassFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, SassLanguage) {
    override fun getFileType() = SassFileType.INSTANCE
}
