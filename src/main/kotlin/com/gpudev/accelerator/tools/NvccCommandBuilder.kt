/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

internal data class NvccCommandOptions(
    val gpuArch: String = "",
    val extraFlags: String = "",
    val includeDirectories: List<String> = emptyList()
)

/** Builds nvcc commands independently of whether paths are local or toolchain-mapped. */
internal object NvccCommandBuilder {

    fun compile(nvcc: String, source: String, output: String, options: NvccCommandOptions): List<String> =
        build(nvcc, "-c", source, output, options)

    fun ptx(nvcc: String, source: String, output: String, options: NvccCommandOptions): List<String> =
        build(nvcc, "-ptx", source, output, options) + "-lineinfo"

    private fun build(
        nvcc: String,
        mode: String,
        source: String,
        output: String,
        options: NvccCommandOptions
    ): List<String> =
        buildList {
            add(nvcc)
            add(mode)
            add(source)
            add("-o")
            add(output)
            addOptions(options)
        }

    private fun MutableList<String>.addOptions(options: NvccCommandOptions) {
        options.includeDirectories.forEach { directory ->
            add("-I")
            add(directory)
        }
        if (options.gpuArch.isNotBlank()) add("-arch=${options.gpuArch}")
        if (options.extraFlags.isNotBlank()) addAll(options.extraFlags.split(Regex("\\s+")))
    }

}
