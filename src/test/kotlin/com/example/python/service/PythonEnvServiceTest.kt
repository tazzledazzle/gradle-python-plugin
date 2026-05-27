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
    fun `resolveExecutable supports uv backend`() {
        val installDir = createTempDir(prefix = "python-env-uv")
        val service = createService(installDir, envManager = "uv")
        val venvExec = File(installDir, ".venv/bin/pytest")
        venvExec.parentFile.mkdirs()
        venvExec.createNewFile()

        val resolved = service.resolveExecutable("pytest")

        assertEquals(venvExec.canonicalFile, resolved.canonicalFile)
    }

    @Test
    fun `resolveExecutable rejects unknown env manager`() {
        val installDir = createTempDir(prefix = "python-env-invalid")
        val service = createService(installDir, envManager = "pipenv")

        val error = kotlin.runCatching { service.resolveExecutable("python") }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `plugin registers shared python environment service`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(PythonPlugin::class.java)

        val registry = project.gradle.sharedServices.registrations
        assertTrue(registry.names.contains("pythonEnvService"))
    }

    private fun createService(installDir: File, envManager: String): PythonEnvService {
        val project = ProjectBuilder.builder().withProjectDir(installDir).build()
        val service = project.gradle.sharedServices.registerIfAbsent(
            "pythonEnvServiceTest",
            PythonEnvService::class.java
        ) { spec ->
            spec.parameters.pythonVersion.set("3.12.0")
            spec.parameters.condaVersion.set("test")
            spec.parameters.condaInstaller.set("miniforge")
            spec.parameters.condaRepoUrl.set("https://example.invalid")
            spec.parameters.installDir.set(project.layout.projectDirectory)
            spec.parameters.envManagerType.set(envManager)
            spec.parameters.uvVersion.set("0.4.0")
            spec.parameters.uvRepoUrl.set("https://example.invalid/")
        }
        return service.get()
    }
}
