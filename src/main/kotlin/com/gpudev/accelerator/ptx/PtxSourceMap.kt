package com.gpudev.accelerator.ptx

/** One PTX instruction line correlated with a source file and line via PTX .file/.loc metadata. */
data class PtxLineReference(val ptxLine: Int, val sourceFile: String, val sourceLine: Int)

class PtxSourceMap private constructor(val references: List<PtxLineReference>) {
    fun forSourceFile(
        sourcePath: String,
        remoteToLocalPath: (String) -> String? = { null }
    ): List<PtxLineReference> {
        val localizedPaths = references.map(PtxLineReference::sourceFile).distinct().associateWith { remotePath ->
            normalizePath(remoteToLocalPath(remotePath) ?: remotePath)
        }
        val normalizedSource = normalizePath(sourcePath)
        val exact = references.filter { localizedPaths[it.sourceFile] == normalizedSource }
        if (exact.isNotEmpty()) return exact

        val candidateScores = localizedPaths.values.distinct().associateWith { path ->
            commonSuffixSegments(normalizedSource, path)
        }
        val bestScore = candidateScores.values.maxOrNull() ?: return emptyList()
        if (bestScore == 0) return emptyList()
        val bestPaths = candidateScores.filterValues { it == bestScore }.keys
        if (bestPaths.size != 1) return emptyList()
        val resolved = bestPaths.single()
        return references.filter { localizedPaths[it.sourceFile] == resolved }
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
            val references = mutableListOf<PtxLineReference>()
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
                references.add(PtxLineReference(index + 1, sourceFile, sourceLine))
            }
            return PtxSourceMap(references)
        }

        private fun unescape(path: String): String = path.replace("\\\\", "\\").replace("\\\"", "\"")

        private fun commonSuffixSegments(first: String, second: String): Int {
            val firstSegments = first.split('/').filter(String::isNotBlank).asReversed()
            val secondSegments = second.split('/').filter(String::isNotBlank).asReversed()
            return firstSegments.zip(secondSegments).takeWhile { (left, right) -> left == right }.size
        }

        private fun normalizePath(path: String): String = path.replace('\\', '/').trimEnd('/')
    }
}
