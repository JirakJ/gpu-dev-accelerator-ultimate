/*
 * Copyright (c) 2026 Jakub Jirák.
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

/**
 * Parses nvcc diagnostic output.
 * nvcc emits `path(line): error: message` / `path(line): warning: message`,
 * and gcc-style `path:line:col: error: message` from the host compiler pass.
 */
object NvccOutputParser {

    data class Diagnostic(val file: String, val line: Int, val severity: Severity, val message: String)

    enum class Severity { ERROR, WARNING }

    private val nvccStyle = Regex("""^(.+?)\((\d+)\):\s*(error|warning)\s*(?:#\d+-D)?:?\s*(.*)$""")
    private val gccStyle = Regex("""^(.+?):(\d+)(?::\d+)?:\s*(error|warning):\s*(.*)$""")

    fun parse(output: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        for (rawLine in output.lineSequence()) {
            val line = rawLine.trim()
            val m = nvccStyle.find(line) ?: gccStyle.find(line) ?: continue
            val (file, lineNo, severity, message) = m.destructured
            diagnostics.add(
                Diagnostic(
                    file = file,
                    line = lineNo.toIntOrNull() ?: 0,
                    severity = if (severity == "error") Severity.ERROR else Severity.WARNING,
                    message = message
                )
            )
        }
        return diagnostics
    }

    fun errorCount(output: String): Int = parse(output).count { it.severity == Severity.ERROR }
    fun warningCount(output: String): Int = parse(output).count { it.severity == Severity.WARNING }
}
