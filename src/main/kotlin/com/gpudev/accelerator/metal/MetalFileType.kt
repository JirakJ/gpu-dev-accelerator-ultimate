package com.gpudev.accelerator.metal

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class MetalFileType : LanguageFileType(MetalLanguage) {
    override fun getName(): String = "Metal"
    override fun getDescription(): String = "Metal Shading Language file"
    override fun getDefaultExtension(): String = "metal"
    override fun getIcon(): Icon? = null

    companion object {
        val INSTANCE = MetalFileType()
    }
}
