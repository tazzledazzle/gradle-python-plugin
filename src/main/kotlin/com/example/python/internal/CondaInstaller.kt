package com.example.python.internal

import java.io.File

object CondaInstaller {
    fun detectPlatform(systemArch: String?): PlatformSpec {
        val os = System.getProperty("os.name")
        val normalizedOs = when {
            os.contains("Windows", ignoreCase = true) -> "Windows"
            os.contains("Mac", ignoreCase = true) -> "MacOSX"
            else -> "Linux"
        }
        val arch = systemArch ?: System.getProperty("os.arch")
        val normalizedArch = when (arch) {
            "aarch64", "arm64" -> "arm64"
            else -> "x86_64"
        }
        return PlatformSpec(os = normalizedOs, arch = normalizedArch)
    }

    /**
     * Idempotent bootstrap marker for conda root. Real download logic lands in later tasks.
     */
    fun installIfAbsent(spec: CondaInstallSpec): File {
        val condaRoot = File(spec.installDir, ".gradle/python/conda/${spec.version}")
        val sentinel = File(condaRoot, ".installed")
        if (!sentinel.exists()) {
            condaRoot.mkdirs()
            sentinel.writeText("installed")
        }
        return condaRoot
    }
}
