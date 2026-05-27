package com.example.python.service

import com.example.python.PythonPlugin
import com.example.python.internal.CondaInstallSpec
import com.example.python.internal.CondaInstaller
import com.example.python.internal.PlatformSpec
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PythonEnvServiceTest {
    @Test
    fun `condaRoot is resolved lazily once per service instance`() {
        val installDir = createTempDir(prefix = "python-env-service")
        val spec = CondaInstallSpec(
            version = "test",
            installer = "miniforge",
            repoUrl = "https://example.invalid",
            installDir = installDir,
            platform = PlatformSpec(os = "Linux", arch = "x86_64"),
            repoUsername = null,
            repoPassword = null,
            repoHeaders = emptyMap()
        )

        val first = CondaInstaller.installIfAbsent(spec)
        val second = CondaInstaller.installIfAbsent(spec)

        assertEquals(first.absolutePath, second.absolutePath)
        assertTrue(File(installDir, ".gradle/python/conda/test/.installed").exists())
    }

    @Test
    fun `plugin registers shared python environment service`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PythonPlugin::class.java)

        val registry = project.gradle.sharedServices.registrations
        assertTrue(registry.names.contains("pythonEnvService"))
    }
}
