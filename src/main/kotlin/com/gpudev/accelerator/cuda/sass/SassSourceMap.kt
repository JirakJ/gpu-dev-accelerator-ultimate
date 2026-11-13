package com.gpudev.accelerator.cuda.sass

import com.gpudev.accelerator.cuda.ptx.SourceLineReference
import com.gpudev.accelerator.cuda.ptx.SourcePathMatcher

object SassSourceMap {
    private val SOURCE_LOCATION = Regex("""^\s*//## File \"((?:\\.|[^"])*)\", line (\d+)\s*$""")
    private val SECTION_MARKER = Regex("^\\s*//-+\\s+\\.([^\\s]+)")
    private val SECTION_DIRECTIVE = Regex("^\\s*\\.section\\s+\\.(\\S+)")
    private val INSTRUCTION_ADDRESS = Regex("^\\s*/\\*([0-9a-fA-F]+)\\*/\\s+(.+)")

    fun parse(disassembly: String): List<SourceLineReference> {
        val references = mutableListOf<SourceLineReference>()
        var inTextSection = false
        var sourceFile: String? = null
        var sourceLine: Int? = null
        disassembly.lineSequence().forEachIndexed { index, row ->
            val sectionName = SECTION_MARKER.find(row)?.groupValues?.get(1)
                ?: SECTION_DIRECTIVE.find(row)?.groupValues?.get(1)
            if (sectionName != null) {
                inTextSection = sectionName.startsWith("text.")
                sourceFile = null
                sourceLine = null
                return@forEachIndexed
            }
            if (row.trimStart().startsWith(".text.") && row.trimEnd().endsWith(':')) {
                inTextSection = true
                sourceFile = null
                sourceLine = null
                return@forEachIndexed
            }

            val sourceMatch = SOURCE_LOCATION.matchEntire(row)
            if (sourceMatch != null) {
                if (inTextSection) {
                    sourceFile = unescape(sourceMatch.groupValues[1])
                    sourceLine = sourceMatch.groupValues[2].toIntOrNull()
                }
                return@forEachIndexed
            }
            if (!inTextSection) return@forEachIndexed

            val instruction = INSTRUCTION_ADDRESS.find(row) ?: return@forEachIndexed
            val opcode = instruction.groupValues[2].trimStart()
            if (opcode.startsWith('.')) return@forEachIndexed
            val file = sourceFile ?: return@forEachIndexed
            val line = sourceLine ?: return@forEachIndexed
            references.add(SourceLineReference(index + 1, file, line))
        }
        return references
    }

    fun forSourceFile(
        references: List<SourceLineReference>,
        sourcePath: String,
        remoteToLocalPath: (String) -> String? = { null }
    ): List<SourceLineReference> {
        val matchingFiles = SourcePathMatcher.matchingSourceFiles(
            references.map(SourceLineReference::sourceFile),
            sourcePath,
            remoteToLocalPath
        )
        return references.filter { it.sourceFile in matchingFiles }
    }

    private fun unescape(path: String): String = path.replace("\\\\", "\\").replace("\\\"", "\"")
}
