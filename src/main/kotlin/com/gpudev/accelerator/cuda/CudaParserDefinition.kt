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
    
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    
    override fun createElement(node: ASTNode?): PsiElement = CudaPsiElement(node!!)
    
    override fun createFile(viewProvider: FileViewProvider): PsiFile = CudaFile(viewProvider)
    
    companion object {
        val FILE = IFileElementType(CudaLanguage)
    }
}

class CudaLexer : com.intellij.lexer.LexerBase() {
    private var myBuffer: CharSequence = ""
    private var myStartOffset = 0
    private var myEndOffset = 0
    private var myPosition = 0
    
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        myBuffer = buffer
        myStartOffset = startOffset
        myEndOffset = endOffset
        myPosition = startOffset
    }
    
    override fun getState(): Int = 0
    override fun getTokenType() = null
    override fun getTokenStart(): Int = myPosition
    override fun getTokenEnd(): Int = myEndOffset
    override fun advance() { myPosition = myEndOffset }
    override fun getBufferSequence(): CharSequence = myBuffer
    override fun getBufferEnd(): Int = myEndOffset
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
