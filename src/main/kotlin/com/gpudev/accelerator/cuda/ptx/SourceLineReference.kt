package com.gpudev.accelerator.cuda.ptx

/** One generated assembly line correlated with an original source file and line. */
data class SourceLineReference(val outputLine: Int, val sourceFile: String, val sourceLine: Int)

typealias PtxLineReference = SourceLineReference

/** Resolves tool-emitted file paths to one unambiguous source file. */
internal object SourcePathMatcher {
    fun matchingSourceFiles(
        sourcePaths: Collection<String>,
        sourcePath: String,
        remoteToLocalPath: (String) -> String? = { null }
    ): Set<String> {
        val target = normalizePath(sourcePath)
        val localized = sourcePaths.distinct().associateWith { path ->
            normalizePath(remoteToLocalPath(path) ?: path)
        }
        val exact = localized.filterValues { it == target }.keys
        if (exact.isNotEmpty()) return exact

        val scores = localized.values.distinct().associateWith { commonSuffixSegments(target, it) }
        val bestScore = scores.values.maxOrNull() ?: return emptySet()
        if (bestScore == 0) return emptySet()
        val bestPaths = scores.filterValues { it == bestScore }.keys
        if (bestPaths.size != 1) return emptySet()
        val best = bestPaths.single()
        return localized.filterValues { it == best }.keys
    }

    private fun commonSuffixSegments(first: String, second: String): Int {
        val left = first.split('/').filter(String::isNotBlank).asReversed()
        val right = second.split('/').filter(String::isNotBlank).asReversed()
        return left.zip(right).takeWhile { (firstSegment, secondSegment) -> firstSegment == secondSegment }.size
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/').trimEnd('/')
}
