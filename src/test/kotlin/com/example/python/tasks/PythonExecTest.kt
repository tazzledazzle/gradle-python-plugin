package com.example.python.tasks

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PythonExecTest {
    @Test
    fun `captures stdout stderr and exit value for successful process`() {
        val task = newTask()
        val command = successCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)

        task.executeProcess()

        assertEquals("hello", task.stdout.get())
        assertEquals("oops", task.stderr.get())
        assertEquals(0, task.exitValue.get())
    }

    @Test
    fun `fails on non-zero exit and still captures process output`() {
        val task = newTask()
        val command = failureCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)

        assertFailsWith<GradleException> {
            task.executeProcess()
        }

        assertEquals("fail", task.stdout.get())
        assertEquals("bad", task.stderr.get())
        assertEquals(3, task.exitValue.get())
    }

    private fun newTask(): PythonExec {
        val project = ProjectBuilder.builder().build()
        return project.tasks.create("pythonExecTestTask", PythonExec::class.java)
    }

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

    private fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("win")
}
