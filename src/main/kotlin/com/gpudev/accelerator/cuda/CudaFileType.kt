package com.gpudev.accelerator.cuda

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class CudaFileType : LanguageFileType(CudaLanguage) {
    override fun getName(): String = "CUDA"
    override fun getDescription(): String = "CUDA source file"
    override fun getDefaultExtension(): String = "cu"
    override fun getIcon(): Icon? = null

    companion object {
        val INSTANCE = CudaFileType()
    }
}
