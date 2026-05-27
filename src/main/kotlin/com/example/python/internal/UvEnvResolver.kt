package com.example.python.internal

import java.io.File

object UvEnvResolver {
    fun resolve(projectDir: File, execName: String, platform: PlatformSpec): File {
        val venvDir = File(projectDir, ".venv")
        return if (platform.os == "Windows") {
            val scripts = File(venvDir, "Scripts/$execName.exe")
            val direct = File(venvDir, "$execName.exe")
            if (scripts.exists()) scripts else direct
        } else {
            File(venvDir, "bin/$execName")
        }
    }
}
