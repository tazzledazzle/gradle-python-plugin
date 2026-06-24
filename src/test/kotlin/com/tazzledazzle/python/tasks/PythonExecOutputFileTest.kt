package com.tazzledazzle.python.tasks

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PythonExecOutputFileTest {
    @Test
    fun `writes captured stdout to outputFile on success`() {
        val project = ProjectBuilder.builder().build()
        val output = File(project.buildDir, "reports/out.txt")
        val task = newTask(project)
        val command = successCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)
        task.outputFile.set(output)

        task.executeProcess()

        assertEquals("hello", output.readText())
    }

    @Test
    fun `writes stdout to outputFile before failing on non-zero exit`() {
        val project = ProjectBuilder.builder().build()
        val output = File(project.buildDir, "reports/fail-out.txt")
        val task = newTask(project)
        val command = failureCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)
        task.outputFile.set(output)

        assertFailsWith<GradleException> {
            task.executeProcess()
        }

        assertEquals("fail", output.readText())
    }

    @Test
    fun `creates parent directories for nested outputFile`() {
        val project = ProjectBuilder.builder().build()
        val output = File(project.buildDir, "nested/deep/out.txt")
        val task = newTask(project)
        val command = successCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)
        task.outputFile.set(output)

        task.executeProcess()

        assertTrue(output.parentFile.isDirectory)
        assertEquals("hello", output.readText())
    }

    private fun newTask(project: org.gradle.api.Project): PythonExec =
        project.tasks.create("pythonExecOutputFileTask", PythonExec::class.java)

    private fun successCommand(): Pair<String, List<String>> =
        if (isWindows()) {
            "cmd" to listOf("/c", "(set /p=hello<nul) & (set /p=oops<nul 1>&2) & exit /b 0")
        } else {
            "sh" to listOf("-c", "printf 'hello'; printf 'oops' 1>&2; exit 0")
        }

    private fun failureCommand(): Pair<String, List<String>> =
        if (isWindows()) {
            "cmd" to listOf("/c", "(set /p=fail<nul) & (set /p=bad<nul 1>&2) & exit /b 3")
        } else {
            "sh" to listOf("-c", "printf 'fail'; printf 'bad' 1>&2; exit 3")
        }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
