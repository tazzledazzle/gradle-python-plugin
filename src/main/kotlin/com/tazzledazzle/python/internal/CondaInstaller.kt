package com.tazzledazzle.python.internal

import java.io.File
import java.net.HttpURLConnection
import java.net.URI

object CondaInstaller {
    fun detectPlatform(systemArch: String?): PlatformSpec {
        val os = System.getProperty("os.name")
        val normalizedOs =
            when {
                os.contains("Windows", ignoreCase = true) -> "Windows"
                os.contains("Mac", ignoreCase = true) -> "MacOSX"
                else -> "Linux"
            }
        val arch = systemArch ?: System.getProperty("os.arch")
        val normalizedArch =
            when (arch) {
                "aarch64", "arm64" -> "arm64"
                else -> "x86_64"
            }
        return PlatformSpec(os = normalizedOs, arch = normalizedArch)
    }

    fun installIfAbsent(spec: CondaInstallSpec): File {
        val condaRoot = File(spec.installDir, ".gradle/python/conda/${spec.version}")
        val sentinel = File(condaRoot, ".installed")
        if (sentinel.exists()) {
            return condaRoot
        }

        when (spec.platform.os) {
            "Windows" -> installWindows(spec, condaRoot)
            else -> installUnix(spec, condaRoot)
        }
        sentinel.writeText("installed")
        return condaRoot
    }

    internal fun buildInstallerName(spec: CondaInstallSpec): String {
        val platformToken =
            when (spec.platform.os to spec.platform.arch) {
                "MacOSX" to "arm64" -> "MacOSX-arm64"
                "MacOSX" to "x86_64" -> "MacOSX-x86_64"
                "Linux" to "arm64" -> "Linux-aarch64"
                "Linux" to "x86_64" -> "Linux-x86_64"
                "Windows" to "x86_64" -> "Windows-x86_64"
                else -> error("Unsupported platform: ${spec.platform.os}/${spec.platform.arch}")
            }
        return "Miniforge3-${spec.version}-$platformToken${if (spec.platform.os == "Windows") ".exe" else ".sh"}"
    }

    private fun installUnix(
        spec: CondaInstallSpec,
        condaRoot: File,
    ) {
        val installerName = buildInstallerName(spec)
        val downloadUrl = "${spec.repoUrl.trimEnd('/')}/${spec.version}/$installerName"
        val installerFile = File(spec.installDir, ".gradle/python/conda/cache/$installerName")
        installerFile.parentFile.mkdirs()
        downloadFile(downloadUrl, installerFile, spec.repoUsername, spec.repoPassword)
        installerFile.setExecutable(true)

        condaRoot.parentFile.mkdirs()
        val command =
            listOf(
                "bash",
                installerFile.absolutePath,
                "-b",
                "-p",
                condaRoot.absolutePath,
            )
        runCommand(command, spec.installDir)
    }

    private fun installWindows(
        spec: CondaInstallSpec,
        condaRoot: File,
    ) {
        val installerName = buildInstallerName(spec)
        val downloadUrl = "${spec.repoUrl.trimEnd('/')}/${spec.version}/$installerName"
        val installerFile = File(spec.installDir, ".gradle/python/conda/cache/$installerName")
        installerFile.parentFile.mkdirs()
        downloadFile(downloadUrl, installerFile, spec.repoUsername, spec.repoPassword)

        condaRoot.parentFile.mkdirs()
        val command =
            listOf(
                "cmd",
                "/c",
                "${installerFile.absolutePath} /S /D=${condaRoot.absolutePath}",
            )
        runCommand(command, spec.installDir)
    }

    private fun downloadFile(
        url: String,
        dest: File,
        repoUsername: String?,
        repoPassword: String?,
    ) {
        val connection =
            (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 300_000
                if (!repoUsername.isNullOrBlank()) {
                    val token = "$repoUsername:${repoPassword.orEmpty()}"
                    val encoded = java.util.Base64.getEncoder().encodeToString(token.toByteArray())
                    setRequestProperty("Authorization", "Basic $encoded")
                }
            }
        connection.inputStream.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun runCommand(
        command: List<String>,
        workingDir: File,
    ) {
        val exit =
            ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        if (exit != 0) {
            error("Command failed (exit $exit): ${command.joinToString(" ")}")
        }
    }
}
