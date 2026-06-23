package com.tazzledazzle.python.internal

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CondaInstallerTest {
    @Test
    fun `installIfAbsent is idempotent`() {
        val installDir = kotlin.io.path.createTempDirectory(prefix = "conda-installer").toFile()
        val spec =
            CondaInstallSpec(
                version = "24.11.3-0",
                installer = "miniforge",
                repoUrl = "https://example.invalid/",
                installDir = installDir,
                platform = PlatformSpec(os = "Linux", arch = "x86_64"),
                repoUsername = null,
                repoPassword = null,
                repoHeaders = emptyMap(),
            )
        val sentinel = File(installDir, ".gradle/python/conda/${spec.version}/.installed")
        sentinel.parentFile.mkdirs()
        sentinel.writeText("installed")

        val first = CondaInstaller.installIfAbsent(spec)
        val second = CondaInstaller.installIfAbsent(spec)
        assertEquals(first.absolutePath, second.absolutePath)
    }

    @Test
    fun `buildInstallerName maps linux x86_64`() {
        val spec =
            CondaInstallSpec(
                version = "24.11.3-0",
                installer = "miniforge",
                repoUrl = "https://example.invalid/",
                installDir = File("."),
                platform = PlatformSpec(os = "Linux", arch = "x86_64"),
                repoUsername = null,
                repoPassword = null,
                repoHeaders = emptyMap(),
            )
        assertEquals("Miniforge3-24.11.3-0-Linux-x86_64.sh", CondaInstaller.buildInstallerName(spec))
    }
}
