/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.templates

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType

class GpuTemplateContext : TemplateContextType("GPUDEV", "GPU (CUDA/Metal/OpenCL)") {
    override fun isInContext(context: TemplateActionContext): Boolean {
        val name = context.file.name
        return name.endsWith(".cu") || name.endsWith(".cuh") ||
            name.endsWith(".metal") || name.endsWith(".cl")
    }
}
