package com.gpudev.accelerator.opencl

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class OpenCLFileType : LanguageFileType(OpenCLLanguage) {
    override fun getName(): String = "OpenCL"
    override fun getDescription(): String = "OpenCL source file"
    override fun getDefaultExtension(): String = "cl"
    override fun getIcon(): Icon? = null

    companion object {
        val INSTANCE = OpenCLFileType()
    }
}
