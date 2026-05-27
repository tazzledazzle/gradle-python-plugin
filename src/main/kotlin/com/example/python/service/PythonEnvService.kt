package com.example.python.service

import com.example.python.internal.CondaInstallSpec
import com.example.python.internal.CondaInstaller
import com.example.python.internal.PlatformSpec
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
    }

    val platform: PlatformSpec by lazy {
        CondaInstaller.detectPlatform(parameters.systemArch.orNull)
    }

    val condaRoot: File by lazy {
        CondaInstaller.installIfAbsent(buildCondaSpec())
    }

    fun resolveExecutable(execName: String): File {
        val venvBin = if (platform.os == "Windows") "Scripts" else "bin"
        return File(condaRoot, "envs/${parameters.pythonVersion.get()}/$venvBin/$execName")
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
