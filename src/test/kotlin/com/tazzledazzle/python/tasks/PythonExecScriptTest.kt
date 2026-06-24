package com.tazzledazzle.python.tasks

import com.tazzledazzle.python.service.PythonEnvService
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PythonExecScriptTest {
    @Test
    fun `buildCommand resolves script via managed python`() {
        val projectDir = createTempDir(prefix = "python-exec-script")
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        val python = File(projectDir, ".gradle/python/conda/test/envs/3.12.0/bin/python")
        python.parentFile.mkdirs()
        python.writeText("#!/bin/sh\necho ok\n")
        python.setExecutable(true)

        File(projectDir, ".gradle/python/conda/test/.installed").apply {
            parentFile.mkdirs()
            writeText("installed")
        }

        val service = registerService(project)
        val scriptFile =
            File(projectDir, "scripts/hello.py").apply {
                parentFile.mkdirs()
                writeText("print('hi')")
            }

        val task = project.tasks.create("pythonExecScript", PythonExec::class.java)
        task.envService.set(service)
        task.script.set(scriptFile)
        task.arguments.set(listOf("--verbose"))

        val command = task.buildCommand()
        assertEquals(python.canonicalFile.absolutePath, command[0])
        assertEquals(scriptFile.canonicalFile.absolutePath, command[1])
        assertEquals("--verbose", command[2])
    }

    @Test
    fun `buildCommand rejects script with venvExec`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("pythonExecScriptConflict", PythonExec::class.java)
        task.script.set(File("scripts/a.py"))
        task.venvExec.set("pytest")

        val error = kotlin.runCatching { task.buildCommand() }.exceptionOrNull()
        assertTrue(error is GradleException)
        assertTrue(error!!.message!!.contains("script"))
        assertTrue(error.message!!.contains("venvExec"))
    }

    @Test
    fun `buildCommand rejects script with executable`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("pythonExecScriptExecConflict", PythonExec::class.java)
        task.script.set(File("scripts/a.py"))
        task.executable.set("/usr/bin/python3")

        assertFailsWith<GradleException> {
            task.buildCommand()
        }
    }

    private fun registerService(project: org.gradle.api.Project) =
        project.gradle.sharedServices.registerIfAbsent(
            "pythonEnvServiceScriptTest",
            PythonEnvService::class.java,
        ) { spec ->
            spec.parameters.pythonVersion.set("3.12.0")
            spec.parameters.condaVersion.set("test")
            spec.parameters.condaInstaller.set("miniforge")
            spec.parameters.condaRepoUrl.set("https://example.invalid/")
            spec.parameters.installDir.set(project.layout.projectDirectory)
            spec.parameters.envManagerType.set("conda")
            spec.parameters.uvVersion.set("0.4.0")
            spec.parameters.uvRepoUrl.set("https://example.invalid/")
        }

    private fun createTempDir(prefix: String): File {
        val dir = File.createTempFile(prefix, "")
        check(dir.delete()) { "Failed to delete temp file marker" }
        check(dir.mkdir()) { "Failed to create temp dir" }
        return dir
    }
}
