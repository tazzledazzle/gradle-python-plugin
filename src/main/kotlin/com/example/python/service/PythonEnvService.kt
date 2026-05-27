package com.example.python.service

import com.example.python.internal.CondaInstallSpec
import com.example.python.internal.CondaInstaller
import com.example.python.internal.PlatformSpec
import com.example.python.internal.UvEnvResolver
import com.example.python.internal.UvInstaller
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File

abstract class PythonEnvService : BuildService<PythonEnvService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val pythonVersion: Property<String>
        val condaVersion: Property<String>
        val condaInstaller: Property<String>
        val condaRepoUrl: Property<String>
        val condaRepoUsername: Property<String>
        val condaRepoPassword: Property<String>
        val installDir: DirectoryProperty
        val systemArch: Property<String>
        val envManagerType: Property<String>
        val uvVersion: Property<String>
        val uvRepoUrl: Property<String>
    }

    val platform: PlatformSpec by lazy {
        CondaInstaller.detectPlatform(parameters.systemArch.orNull)
    }

    val condaRoot: File by lazy {
        CondaInstaller.installIfAbsent(buildCondaSpec())
    }

    private val uvBinary: File by lazy {
        UvInstaller.installIfAbsent(
            version = parameters.uvVersion.get(),
            installDir = parameters.installDir.asFile.get(),
            platform = platform,
            repoUrl = parameters.uvRepoUrl.get()
        )
    }

    fun resolveExecutable(execName: String): File {
        val projectDir = parameters.installDir.asFile.get()
        return when (val envManager = parameters.envManagerType.get()) {
            "uv" -> {
                UvInstaller.setupEnv(uvBinary, parameters.pythonVersion.get(), projectDir)
                UvEnvResolver.resolve(projectDir, execName, platform)
            }
            "conda" -> {
                val venvBin = if (platform.os == "Windows") "Scripts" else "bin"
                File(condaRoot, "envs/${parameters.pythonVersion.get()}/$venvBin/$execName")
            }
            else -> throw IllegalStateException(
                "Unsupported envManager '$envManager'. Expected 'conda' or 'uv'."
            )
        }
    }

    fun bootstrapEnvironment() {
        when (parameters.envManagerType.get()) {
            "uv" -> {
                uvBinary
                UvInstaller.setupEnv(uvBinary, parameters.pythonVersion.get(), parameters.installDir.asFile.get())
            }
            "conda" -> {
                condaRoot
            }
            else -> throw IllegalStateException(
                "Unsupported envManager '${parameters.envManagerType.get()}'. Expected 'conda' or 'uv'."
            )
        }
    }

    private fun buildCondaSpec(): CondaInstallSpec =
        CondaInstallSpec(
            version = parameters.condaVersion.get(),
            installer = parameters.condaInstaller.get(),
            repoUrl = parameters.condaRepoUrl.get(),
            installDir = parameters.installDir.asFile.get(),
            platform = platform,
            repoUsername = parameters.condaRepoUsername.orNull,
            repoPassword = parameters.condaRepoPassword.orNull,
            repoHeaders = emptyMap()
        )

    override fun close() {
        // Reserved for future resource cleanup.
    }
}
