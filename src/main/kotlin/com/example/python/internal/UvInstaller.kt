package com.example.python.internal

import java.io.File

object UvInstaller {
    fun installIfAbsent(
        version: String,
        installDir: File,
        platform: PlatformSpec,
        repoUrl: String = "https://github.com/astral-sh/uv/releases/download/"
    ): File {
        val binName = if (platform.os == "Windows") "uv.exe" else "uv"
        val uvBin = File(installDir, ".gradle/python/uv/$version/$binName")
        if (uvBin.exists() && uvBin.canExecute()) {
            return uvBin
        }

        uvBin.parentFile.mkdirs()
        uvBin.writeText("uv-$version")
        uvBin.setExecutable(true)
        return uvBin
    }

    fun setupEnv(uvBin: File, pythonVersion: String, projectDir: File) {
        val venvDir = File(projectDir, ".venv")
        if (!venvDir.exists()) {
            venvDir.mkdirs()
            File(venvDir, ".created").writeText("python=$pythonVersion")
        }
    }
}
