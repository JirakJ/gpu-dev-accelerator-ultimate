/*
 * Copyright (c) 2026 Alex (github.com/lexasub).
 * Licensed under the Apache License, Version 2.0. See LICENSE for details.
 */
package com.gpudev.accelerator.tools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File

private fun Any.readIntProperty(name: String): Int = readProperty("get", name) as Int
private fun Any.readStringProperty(name: String): String = readProperty("get", name) as String
private fun Any.readBooleanProperty(name: String): Boolean = readProperty("is", name.removePrefix("is")) as Boolean

private fun Any.readProperty(prefix: String, name: String): Any =
    javaClass.getMethod("$prefix${name.replaceFirstChar(Char::uppercase)}").invoke(this)

/** Adapter over CLion's internal SSH toolchain and deployment APIs. */
internal class ClionRemoteToolchain private constructor(
    private val project: Project,
    private val hostId: String,
    private val sshConfig: Any,
    private val credentials: Any
) : RemoteToolTransport {

    override fun resolve(configuredPath: String, vararg names: String): String? {
        if (configuredPath.isNotBlank()) return configuredPath
        for (name in names) {
            val resolved = invokeStatic(REMOTE_UTIL, "resolveToExecutable", credentials, name) as? String
            if (!resolved.isNullOrBlank()) return resolved
        }
        LOG.warn("Remote resolve: none of [${names.joinToString()}] found (hostId=$hostId)")
        return null
    }

    override fun run(command: List<String>, workDirectory: String?, timeoutMs: Int): ToolResult {
        val line = GeneralCommandLine(command)
        if (!workDirectory.isNullOrBlank()) line.workDirectory = File(workDirectory)
        line.charset = Charsets.UTF_8
        val output = invokeStatic(REMOTE_UTIL, "runRemoteProcess", line, credentials, timeoutMs)
            ?: error("CLion remote process API returned no result")
        return ToolResult(
            exitCode = output.readIntProperty("exitCode"),
            stdout = output.readStringProperty("stdout"),
            stderr = output.readStringProperty("stderr"),
            timedOut = output.readBooleanProperty("isTimeout")
        )
    }

    override fun toRemotePath(localPath: String): String {
        val mapper = pathMapper(localPath)
        if (mapper == null) {
            LOG.warn(
                "No remote path mapper: hostId=$hostId, project='${project.name}', " +
                    "localPath='$localPath', basePath='${project.basePath}'"
            )
            error("CLion has no remote path mapping for $localPath")
        }
        val remote = convertPath(mapper, localPath, "convertToRemote")
        if (remote == null) {
            LOG.warn(
                "Mapper.convertToRemote returned null: hostId=$hostId, localPath='$localPath', " +
                    "mapper=${mapper.javaClass.name}"
            )
            error("CLion could not map local path $localPath to the remote host")
        }
        return remote
    }

    override fun toLocalPath(remotePath: String): String? {
        val localRoot = project.basePath ?: return null
        val mapper = pathMapper(localRoot) ?: return null
        return convertPath(mapper, remotePath, "convertToLocal")
    }

    private fun pathMapper(localPath: String): Any? {
        val helper = type(REMOTE_DEPLOYMENT_HELPER)
        val direct = helper.getMethod(
            "getPathMapper",
            String::class.java,
            Project::class.java,
            String::class.java
        ).invoke(null, hostId, project, localPath)
        if (direct != null) return direct
        val basePath = project.basePath ?: return null
        if (basePath == localPath) return null
        return helper.getMethod(
            "getPathMapper",
            String::class.java,
            Project::class.java,
            String::class.java
        ).invoke(null, hostId, project, basePath)
    }

    private fun convertPath(mapper: Any, path: String, methodName: String): String? =
        mapper.javaClass.getMethod(methodName, String::class.java).invoke(mapper, path) as? String

    override fun createTemporaryFile(): String {
        val linux = type("com.intellij.util.system.OS").getField("Linux").get(null)
        val remotePath = invokeStatic(REMOTE_UTIL, "mktemp", credentials, linux, false) as? String
        if (remotePath.isNullOrBlank()) error("CLion could not create a remote temporary file")
        return remotePath
    }

    override fun needsTransfer(localPath: String, remotePath: String): Boolean {
        val needed = runCatching {
            invokeStatic(REMOTE_DEPLOYMENT_HELPER, "isSyncNeeded", hostId, project, localPath, remotePath) as? Boolean
        }.getOrNull()
        if (needed == null) {
            LOG.warn("CLion transfer policy unavailable for '$localPath' -> '$remotePath'; aborting transfer")
            error("CLion could not determine whether remote file transfer is safe")
        }
        return needed
    }

    override fun upload(localPath: String, remotePath: String) {
        invokeStatic(REMOTE_DEPLOYMENT_HELPER, "upload", hostId, project, localPath, remotePath)
    }

    override fun download(remotePath: String, localPath: String) {
        invokeStatic(REMOTE_DEPLOYMENT_HELPER, "download", hostId, sshConfig, project, remotePath, localPath)
    }

    override fun delete(remotePath: String, timeoutMs: Int) {
        run(listOf("rm", "-f", remotePath), null, timeoutMs)
    }

    companion object {
        private val LOG = Logger.getInstance(ClionRemoteToolchain::class.java)
        private const val CPP_TOOLCHAINS = "com.jetbrains.cidr.cpp.toolchains.CPPToolchains"
        private const val REMOTE_UTIL = "com.jetbrains.cidr.system.RemoteUtil"
        private const val REMOTE_DEPLOYMENT_HELPER = "com.jetbrains.cidr.system.RemoteDeploymentHelper"
        private const val SSH_TOOLSET = "SSH"

        fun current(project: Project): ToolExecutionAdapter? {
            return try {
                val toolchains = type(CPP_TOOLCHAINS).getMethod("getInstance").invoke(null)
                val toolchain = toolchains.javaClass.getMethod("getDefaultToolchain").invoke(toolchains) ?: return null
                val kind = toolchain.javaClass.getMethod("getToolSetKind").invoke(toolchain)
                if (kind.toString() != SSH_TOOLSET) return null
                val hostData = toolchain.javaClass.getMethod("getHostData").invoke(toolchain)
                    ?: error("The default CLion SSH toolchain has no host configuration")
                val hostId = hostData.javaClass.getMethod("getId").invoke(hostData) as String
                val sshConfig = hostData.javaClass.getMethod("getSshConfig").invoke(hostData)
                    ?: error("The default CLion SSH toolchain has no SSH configuration")
                val credentials = invokeStatic(REMOTE_UTIL, "getCredentials", sshConfig)
                    ?: error("CLion could not resolve SSH credentials")
                RemoteToolExecutionAdapter(ClionRemoteToolchain(project, hostId, sshConfig, credentials))
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: NoSuchMethodException) {
                null
            } catch (_: LinkageError) {
                null
            } catch (e: Exception) {
                LOG.warn("CLion remote toolchain initialization failed; using local execution", e)
                null
            }
        }



        private fun type(name: String): Class<*> = Class.forName(name)

        private fun invokeStatic(className: String, methodName: String, vararg args: Any): Any? {
            val method = type(className).methods.firstOrNull { candidate ->
                candidate.name == methodName && candidate.parameterCount == args.size &&
                    candidate.parameterTypes.indices.all { index ->
                        val parameterType = candidate.parameterTypes[index]
                        val argument = args[index]
                        parameterType.isInstance(argument) ||
                            (parameterType == Int::class.javaPrimitiveType && argument is Int) ||
                            (parameterType == Boolean::class.javaPrimitiveType && argument is Boolean)
                    }
            } ?: error("CLion API $className.$methodName is unavailable")
            return method.invoke(null, *args)
        }
    }
}
