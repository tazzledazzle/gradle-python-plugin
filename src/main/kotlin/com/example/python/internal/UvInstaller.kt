package com.example.python.internal

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

object UvInstaller {
    fun installIfAbsent(
        version: String,
        installDir: File,
        platform: PlatformSpec,
        repoUrl: String = "https://github.com/astral-sh/uv/releases/download/",
    ): File {
        val binName = if (platform.os == "Windows") "uv.exe" else "uv"
        val uvBin = File(installDir, ".gradle/python/uv/$version/$binName")
        if (uvBin.exists() && uvBin.canExecute()) {
            return uvBin
        }

        uvBin.parentFile.mkdirs()
        val archiveName = buildArchiveName(platform)
        val downloadUrl = "${repoUrl.trimEnd('/')}/$version/$archiveName"
        val tempArchive = File(System.getProperty("java.io.tmpdir"), "uv-$version-$archiveName")
        downloadFile(downloadUrl, tempArchive)
        extractBinary(tempArchive, uvBin, platform)
        uvBin.setExecutable(true)
        tempArchive.delete()
        return uvBin
    }

    fun setupEnv(
        uvBin: File,
        pythonVersion: String,
        projectDir: File,
    ) {
        if (File(projectDir, ".venv/pyvenv.cfg").exists()) {
            return
        }
        runCommand(listOf(uvBin.absolutePath, "python", "install", pythonVersion), projectDir)
        runCommand(
            listOf(uvBin.absolutePath, "venv", "--python", pythonVersion, ".venv"),
            projectDir,
        )
    }

    internal fun buildArchiveName(platform: PlatformSpec): String {
        val target =
            when {
                platform.os == "Windows" && platform.arch == "x86_64" -> "x86_64-pc-windows-msvc"
                platform.os == "Linux" && platform.arch == "x86_64" -> "x86_64-unknown-linux-musl"
                platform.os == "Linux" && platform.arch == "arm64" -> "aarch64-unknown-linux-musl"
                platform.os == "MacOSX" && platform.arch == "arm64" -> "aarch64-apple-darwin"
                platform.os == "MacOSX" && platform.arch == "x86_64" -> "x86_64-apple-darwin"
                else -> error("Unsupported platform: ${platform.os}/${platform.arch}")
            }
        val ext = if (platform.os == "Windows") "zip" else "tar.gz"
        return "uv-$target.$ext"
    }

    private fun downloadFile(
        url: String,
        dest: File,
        repoUsername: String? = null,
        repoPassword: String? = null,
    ) {
        val connection =
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
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

    private fun extractBinary(
        archive: File,
        dest: File,
        platform: PlatformSpec,
    ) {
        if (platform.os == "Windows") {
            extractWindowsBinary(archive, dest)
        } else {
            extractUnixBinary(archive, dest)
        }
    }

    private fun extractWindowsBinary(
        archive: File,
        dest: File,
    ) {
        ZipFile(archive).use { zip ->
            val entry =
                zip.entries().asSequence().firstOrNull { it.name.endsWith("uv.exe") }
                    ?: error("uv.exe not found in archive ${archive.name}")
            zip.getInputStream(entry).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun extractUnixBinary(
        archive: File,
        dest: File,
    ) {
        val command =
            listOf(
                "tar",
                "-xzf",
                archive.absolutePath,
                "-C",
                dest.parent,
                "--strip-components=1",
                "uv",
            )
        runCommand(command, dest.parentFile)
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
