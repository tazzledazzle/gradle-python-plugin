package com.tazzledazzle.python.tasks

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PythonExecIgnoreExitValueTest {
    @Test
    fun `default ignoreExitValue false throws on non-zero exit`() {
        val task = newTask()
        val command = failureCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)

        assertFailsWith<GradleException> {
            task.executeProcess()
        }

        assertEquals(5, task.exitValue.get())
    }

    @Test
    fun `ignoreExitValue true does not throw on non-zero exit`() {
        val task = newTask()
        val command = failureCommand()

        task.executable.set(command.first)
        task.arguments.set(command.second)
        task.ignoreExitValue.set(true)

        task.executeProcess()

        assertEquals(5, task.exitValue.get())
    }

    private fun newTask(): PythonExec {
        val project = ProjectBuilder.builder().build()
        return project.tasks.create("pythonExecIgnoreExitTask", PythonExec::class.java)
    }

    private fun failureCommand(): Pair<String, List<String>> =
        if (isWindows()) {
            "cmd" to listOf("/c", "(set /p=warn<nul) & (set /p=oops<nul 1>&2) & exit /b 5")
        } else {
            "sh" to listOf("-c", "printf 'warn'; printf 'oops' 1>&2; exit 5")
        }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
