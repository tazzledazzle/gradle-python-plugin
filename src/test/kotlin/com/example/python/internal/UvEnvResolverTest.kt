package com.example.python.internal

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class UvEnvResolverTest {
    @Test
    fun `resolve uses bin directory on unix`() {
        val projectDir = createTempDir(prefix = "uv-resolver")
        val venvBin = File(projectDir, ".venv/bin/pytest")
        venvBin.parentFile.mkdirs()
        venvBin.createNewFile()

        val resolved =
            UvEnvResolver.resolve(
                projectDir = projectDir,
                execName = "pytest",
                platform = PlatformSpec(os = "Linux", arch = "x86_64"),
            )

        assertEquals(venvBin.absolutePath, resolved.absolutePath)
    }

    @Test
    fun `resolve uses Scripts directory on windows`() {
        val projectDir = createTempDir(prefix = "uv-resolver-win")
        val venvBin = File(projectDir, ".venv/Scripts/pytest.exe")
        venvBin.parentFile.mkdirs()
        venvBin.createNewFile()

        val resolved =
            UvEnvResolver.resolve(
                projectDir = projectDir,
                execName = "pytest",
                platform = PlatformSpec(os = "Windows", arch = "x86_64"),
            )

        assertEquals(venvBin.absolutePath, resolved.absolutePath)
    }
}
