/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.gpudev.accelerator.tools

/**
 * Parses compute-sanitizer / cuda-memcheck output.
 * Both tools end with `========= ERROR SUMMARY: N error(s)`.
 */
object SanitizerOutputParser {

    data class Summary(val errorCount: Int, val findings: List<String>)

    private val summaryLine = Regex("""=+\s*ERROR SUMMARY:\s*(\d+)\s*errors?""")
    private val findingLine = Regex("""^=+\s+((?:Invalid|Uninitialized|Leaked|Race|Barrier|Program hit).*)$""")

    fun parse(output: String): Summary {
        var errors = -1
        val findings = mutableListOf<String>()
        for (rawLine in output.lineSequence()) {
            val line = rawLine.trim()
            summaryLine.find(line)?.let { errors = it.groupValues[1].toInt() }
            findingLine.find(line)?.let { findings.add(it.groupValues[1].trim()) }
        }
        // ponytail: -1 = no summary line found (tool crashed or wrong binary), caller reports raw output
        return Summary(errors, findings)
    }
}
