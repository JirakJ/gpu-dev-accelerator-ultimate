package com.gpudev.accelerator.cuda.sass

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class SassFileType : LanguageFileType(SassLanguage) {
    override fun getName(): String = "CUDA SASS"
    override fun getDescription(): String = "NVIDIA CUDA machine assembly"
    override fun getDefaultExtension(): String = "sassasm"
    override fun getIcon(): Icon? = null

    companion object {
        val INSTANCE = SassFileType()
    }
}
