/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NvccCommandBuilderTest {

    @Test
    fun `compile command preserves toolchain-mapped source and output paths`() {
        assertEquals(
            listOf(
                "/remote/cuda/bin/nvcc",
                "-c",
                "/remote/project/kernel.cu",
                "-o",
                "/remote/project/kernel.o",
                "-arch=sm_90",
                "-O3",
                "--use_fast_math"
            ),
            NvccCommandBuilder.compile(
                "/remote/cuda/bin/nvcc",
                "/remote/project/kernel.cu",
                "/remote/project/kernel.o",
                NvccCommandOptions("sm_90", "-O3 --use_fast_math")
            )
        )
    }

    @Test
    fun `compile command includes project root for repository-relative headers`() {
        assertEquals(
            listOf("nvcc", "-c", "/repo/kernels/common/kernel.cu", "-o", "/repo/kernel.o", "-I", "/repo"),
            NvccCommandBuilder.compile(
                "nvcc",
                "/repo/kernels/common/kernel.cu",
                "/repo/kernel.o",
                NvccCommandOptions(includeDirectories = listOf("/repo"))
            )
        )
    }

    @Test
    fun `ptx command omits empty options and keeps remote output path`() {
        assertEquals(
            listOf("nvcc", "-ptx", "/remote/project/kernel.cu", "-o", "/remote/tmp/kernel.ptx"),
            NvccCommandBuilder.ptx(
                "nvcc",
                "/remote/project/kernel.cu",
                "/remote/tmp/kernel.ptx",
                NvccCommandOptions()
            )
        )
    }
}
