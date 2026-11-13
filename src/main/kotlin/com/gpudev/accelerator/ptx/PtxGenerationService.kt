package com.gpudev.accelerator.ptx

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.gpudev.accelerator.settings.GpuDevSettings
import com.gpudev.accelerator.tools.GpuToolchain
import com.gpudev.accelerator.tools.NvccCommandBuilder
import com.gpudev.accelerator.tools.NvccCommandOptions
import com.gpudev.accelerator.tools.ToolArtifact
import com.gpudev.accelerator.tools.ToolArtifactKind
import com.gpudev.accelerator.tools.ToolInvocation
import com.gpudev.accelerator.tools.ToolResult
import java.io.File
import java.nio.file.Files

data class PtxGenerationResult(
    val command: List<String>,
    val result: ToolResult,
    val ptxText: String?
)

/** Generates PTX through the configured local or CLion toolchain and owns the temporary artifact. */
object PtxGenerationService {

    private val LOG = Logger.getInstance(PtxGenerationService::class.java)

    @JvmStatic
    fun generate(project: Project, sourcePath: String): PtxGenerationResult? {
        val settings = GpuDevSettings.getInstance().state
        val nvcc = GpuToolchain.nvcc(project)
        if (nvcc == null) {
            LOG.warn(
                "nvcc not found for PTX generation " +
                    "(configured='${settings.nvccPath}', arch='${settings.gpuArch}')"
            )
            return null
        }

        val includeRoot = project.basePath ?: File(sourcePath).parentFile.path
        val ptxFile = Files.createTempFile("gpudev-", ".ptx").toFile()
        val command = NvccCommandBuilder.ptx(
            nvcc,
            sourcePath,
            ptxFile.absolutePath,
            NvccCommandOptions(settings.gpuArch, settings.extraNvccFlags, listOf(includeRoot))
        )
        val invocation = ToolInvocation(
            command,
            File(sourcePath).parentFile,
            listOf(
                ToolArtifact(File(sourcePath), ToolArtifactKind.INPUT),
                ToolArtifact(ptxFile, ToolArtifactKind.TEMPORARY_OUTPUT),
                ToolArtifact(File(includeRoot), ToolArtifactKind.PATH_REFERENCE)
            )
        )
        return try {
            val result = GpuToolchain.execute(project, invocation)
            val ptx = runCatching {
                ptxFile.takeIf { result.success && it.exists() }?.readText()?.takeIf { it.isNotBlank() }
            }.onFailure { error ->
                LOG.warn("Could not read generated PTX artifact at ${ptxFile.path}", error)
            }.getOrNull()
            val reportedResult = if (result.success && ptx == null) {
                val note = "nvcc succeeded but the PTX artifact was missing or unreadable"
                result.copy(
                    exitCode = -1,
                    stderr = if (result.stderr.isBlank()) note else result.stderr.trimEnd() + "\n" + note
                )
            } else {
                result
            }
            if (!reportedResult.success) {
                LOG.warn(
                    "nvcc -ptx failed for $sourcePath: exit=${reportedResult.exitCode}, " +
                        "timedOut=${reportedResult.timedOut}: ${reportedResult.combined.take(500)}"
                )
            }
            PtxGenerationResult(command, reportedResult, ptx)
        } finally {
            runCatching { Files.deleteIfExists(ptxFile.toPath()) }
        }
    }

    @JvmStatic
    fun sourceMappings(project: Project, sourcePath: String, ptxText: String): List<PtxLineReference> =
        PtxSourceMap.parse(ptxText).forSourceFile(sourcePath) { remotePath ->
            GpuToolchain.localPath(project, remotePath)
        }
}
