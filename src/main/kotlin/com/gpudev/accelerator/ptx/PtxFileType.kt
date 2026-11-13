package com.gpudev.accelerator.ptx

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class PtxFileType : LanguageFileType(PtxLanguage) {
    override fun getName(): String = "PTX"
    override fun getDescription(): String = "NVIDIA PTX assembly"
    override fun getDefaultExtension(): String = "ptx"
    override fun getIcon(): Icon? = null

    companion object {
        val INSTANCE = PtxFileType()
    }
}
