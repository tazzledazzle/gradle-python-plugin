package com.example.python.tasks

import com.example.python.service.PythonEnvService
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PythonExecVenvExecTest {
    @Test
    fun `buildCommand resolves venvExec through env service`() {
        val projectDir = createTempDir(prefix = "python-exec-venv")
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val service =
            project.gradle.sharedServices.registerIfAbsent(
                "pythonEnvServiceTest",
                PythonEnvService::class.java,
            ) { spec ->
                spec.parameters.pythonVersion.set("3.12.0")
                spec.parameters.condaVersion.set("test")
                spec.parameters.condaInstaller.set("miniforge")
                spec.parameters.condaRepoUrl.set("https://example.invalid/")
                spec.parameters.installDir.set(project.layout.projectDirectory)
                spec.parameters.envManagerType.set("uv")
                spec.parameters.uvVersion.set("0.4.0")
                spec.parameters.uvRepoUrl.set("https://example.invalid/")
            }

        File(projectDir, ".gradle/python/uv/0.4.0/uv").apply {
            parentFile.mkdirs()
            writeText("#!/bin/sh\necho uv\n")
            setExecutable(true)
        }
        File(projectDir, ".venv/pyvenv.cfg").apply {
            parentFile.mkdirs()
            writeText("home = .\n")
        }
        val pytest = File(projectDir, ".venv/bin/pytest")
        pytest.parentFile.mkdirs()
        pytest.writeText("#!/bin/sh\necho ok\n")
        pytest.setExecutable(true)

        val task = project.tasks.create("pythonExecVenv", PythonExec::class.java)
        task.envService.set(service)
        task.venvExec.set("pytest")
        task.arguments.set(listOf("-V"))

        val command = task.buildCommand()
        assertEquals(pytest.canonicalFile.absolutePath, command.first())
        assertEquals("-V", command[1])
    }

    @Test
    fun `buildCommand requires executable or venvExec`() {
        val task =
            ProjectBuilder.builder().build().tasks.create("pythonExecMissing", PythonExec::class.java)

        assertFailsWith<org.gradle.api.GradleException> {
            task.buildCommand()
        }
    }
}
