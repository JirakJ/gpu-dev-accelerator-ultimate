package com.gpudev.accelerator.cuda.sass

import com.gpudev.accelerator.cuda.ptx.SourceLineReference
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

data class SassGenerationResult(
    val compileCommand: List<String>,
    val disassembleCommand: List<String>,
    val result: ToolResult,
    val sassText: String?,
    val references: List<SourceLineReference>,
    val architecture: String
) {
    val success: Boolean get() = result.success && sassText != null
}

/** Compiles one cubin and disassembles it through the same local or remote toolchain. */
object SassGenerationService {
    private val LOG = Logger.getInstance(SassGenerationService::class.java)

    fun generate(project: Project, sourcePath: String): SassGenerationResult {
        val settings = GpuDevSettings.getInstance().state
        val nvcc = GpuToolchain.nvcc(project) ?: return failure("nvcc", settings.gpuArch)
        val nvdisasm = GpuToolchain.nvdisasm(project) ?: return failure("nvdisasm", settings.gpuArch)
        val tools = SassTools(
            nvcc,
            nvdisasm,
            NvccCommandOptions(settings.gpuArch, settings.extraNvccFlags),
            settings.gpuArch.ifBlank { "toolchain default" }
        )
        val cubinFile = Files.createTempFile("gpudev-", ".cubin").toFile()
        return try {
            generateWithTools(project, sourcePath, cubinFile, tools)
        } finally {
            runCatching { Files.deleteIfExists(cubinFile.toPath()) }
        }
    }

    private fun generateWithTools(
        project: Project,
        sourcePath: String,
        cubinFile: File,
        tools: SassTools
    ): SassGenerationResult {
        val includeRoot = project.basePath ?: File(sourcePath).parentFile.path
        val options = tools.options.copy(includeDirectories = listOf(includeRoot))
        val compileCommand = NvccCommandBuilder.cubin(tools.nvcc, sourcePath, cubinFile.absolutePath, options)
        val disassembleCommand = listOf(tools.nvdisasm, "--print-line-info", cubinFile.absolutePath)
        val invocation = ToolInvocation(
            command = compileCommand,
            workDir = File(sourcePath).parentFile,
            artifacts = listOf(
                ToolArtifact(File(sourcePath), ToolArtifactKind.INPUT),
                ToolArtifact(cubinFile, ToolArtifactKind.INTERMEDIATE_OUTPUT),
                ToolArtifact(File(includeRoot), ToolArtifactKind.PATH_REFERENCE)
            ),
            followUpCommands = listOf(disassembleCommand)
        )
        val result = GpuToolchain.execute(project, invocation)
        val sass = result.stdout.takeIf { result.success && it.isNotBlank() }
        val reportedResult = reportEmptyOutput(result, sass)
        logFailure(sourcePath, reportedResult)
        val references = sass?.let { disassembly ->
            SassSourceMap.forSourceFile(
                SassSourceMap.parse(disassembly),
                sourcePath
            ) { remotePath -> GpuToolchain.localPath(project, remotePath) }
        }.orEmpty()
        return SassGenerationResult(
            compileCommand,
            disassembleCommand,
            reportedResult,
            sass,
            references,
            tools.architecture
        )
    }

    private fun reportEmptyOutput(result: ToolResult, sass: String?): ToolResult =
        if (result.success && sass == null) {
            val note = "nvdisasm succeeded but returned no SASS output"
            result.copy(
                exitCode = -1,
                stderr = if (result.stderr.isBlank()) note else result.stderr.trimEnd() + "\n" + note
            )
        } else {
            result
        }

    private fun logFailure(sourcePath: String, result: ToolResult) {
        if (!result.success) {
            LOG.warn(
                "SASS generation failed for $sourcePath: exit=${result.exitCode}, " +
                    "timedOut=${result.timedOut}: ${result.combined.take(500)}"
            )
        }
    }

    private fun failure(tool: String, architecture: String) = SassGenerationResult(
        emptyList(),
        emptyList(),
        ToolResult(-1, "", "$tool was not found for the active toolchain", false),
        null,
        emptyList(),
        architecture.ifBlank { "toolchain default" }
    )

    private data class SassTools(
        val nvcc: String,
        val nvdisasm: String,
        val options: NvccCommandOptions,
        val architecture: String
    )
}
