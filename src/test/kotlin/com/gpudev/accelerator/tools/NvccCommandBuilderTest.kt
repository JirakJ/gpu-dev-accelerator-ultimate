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
    fun `cubin command keeps configured options and requests source line metadata`() {
        assertEquals(
            listOf(
                "nvcc", "-cubin", "kernel.cu", "-o", "kernel.cubin", "-I", "/repo", "-arch=sm_86",
                "--use_fast_math", "-lineinfo"
            ),
            NvccCommandBuilder.cubin(
                "nvcc",
                "kernel.cu",
                "kernel.cubin",
                NvccCommandOptions("sm_86", "--use_fast_math", listOf("/repo"))
            )
        )
    }

    @Test
    fun `ptx command keeps configured options and requests source line metadata`() {
        assertEquals(
            listOf(
                "nvcc", "-ptx", "kernel.cu", "-o", "kernel.ptx", "-I", "/repo", "-arch=sm_90",
                "--use_fast_math", "-lineinfo"
            ),
            NvccCommandBuilder.ptx(
                "nvcc",
                "kernel.cu",
                "kernel.ptx",
                NvccCommandOptions("sm_90", "--use_fast_math", listOf("/repo"))
            )
        )
    }

    @Test
    fun `ptx command omits empty options and keeps remote output path`() {
        assertEquals(
            listOf("nvcc", "-ptx", "/remote/project/kernel.cu", "-o", "/remote/tmp/kernel.ptx", "-lineinfo"),
            NvccCommandBuilder.ptx(
                "nvcc",
                "/remote/project/kernel.cu",
                "/remote/tmp/kernel.ptx",
                NvccCommandOptions()
            )
        )
    }
}
