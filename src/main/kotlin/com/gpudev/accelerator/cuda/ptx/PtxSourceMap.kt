package com.gpudev.accelerator.cuda.ptx

class PtxSourceMap private constructor(val references: List<SourceLineReference>) {
    fun forSourceFile(
        sourcePath: String,
        remoteToLocalPath: (String) -> String? = { null }
    ): List<SourceLineReference> {
        val matchedFiles = SourcePathMatcher.matchingSourceFiles(
            references.map(SourceLineReference::sourceFile),
            sourcePath,
            remoteToLocalPath
        )
        return references.filter { it.sourceFile in matchedFiles }
    }

    companion object {
        private val FILE_DIRECTIVE = Regex("^\\s*\\.file\\s+(\\d+)\\s+\"((?:\\\\.|[^\"])*)\"")
        private val LOCATION_DIRECTIVE = Regex("^\\s*\\.loc\\s+(\\d+)\\s+(\\d+)(?:\\s+\\d+)?(?:\\s|$)")

        fun parse(ptx: String): PtxSourceMap {
            val lines = ptx.lines()
            val files = lines.mapNotNull { line ->
                FILE_DIRECTIVE.find(line)?.let { match ->
                    val index = match.groupValues[1].toIntOrNull() ?: return@let null
                    index to unescape(match.groupValues[2])
                }
            }.toMap()
            val references = mutableListOf<SourceLineReference>()
            var currentFile: Int? = null
            var currentSourceLine: Int? = null

            lines.forEachIndexed { index, line ->
                if (line.trimStart().startsWith(".loc")) {
                    val locationMatch = LOCATION_DIRECTIVE.find(line)
                    currentFile = locationMatch?.groupValues?.get(1)?.toIntOrNull()
                    currentSourceLine = locationMatch?.groupValues?.get(2)?.toIntOrNull()
                    return@forEachIndexed
                }
                val instruction = line.substringBefore("//").trim()
                if (!instruction.endsWith(';') || instruction.startsWith('.')) return@forEachIndexed

                val sourceFile = currentFile?.let(files::get) ?: return@forEachIndexed
                val sourceLine = currentSourceLine ?: return@forEachIndexed
                references.add(SourceLineReference(index + 1, sourceFile, sourceLine))
            }
            return PtxSourceMap(references)
        }

        private fun unescape(path: String): String = path.replace("\\\\", "\\").replace("\\\"", "\"")
    }
}
