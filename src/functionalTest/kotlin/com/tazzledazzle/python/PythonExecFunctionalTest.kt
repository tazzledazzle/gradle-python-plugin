package com.tazzledazzle.python

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class PythonExecFunctionalTest {
    @Test
    fun `pythonExec with explicit executable succeeds`() {
        val projectDir = Files.createTempDirectory("python-exec-functional").toFile()
        writeSettings(projectDir)
        writeBuildScript(projectDir)

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("runEcho")
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":runEcho")?.outcome)
    }

    private fun writeSettings(projectDir: File) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "python-exec-functional"
            """.trimIndent(),
        )
    }

    private fun writeBuildScript(projectDir: File) {
        val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
        val executable = if (isWindows) "cmd" else "sh"
        val arguments =
            if (isWindows) {
                """listOf("/c", "echo hello")"""
            } else {
                """listOf("-c", "printf hello")"""
            }

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.tazzledazzle.python")
            }

            tasks.register<com.tazzledazzle.python.tasks.PythonExec>("runEcho") {
                executable.set("$executable")
                arguments.set($arguments)
            }
            """.trimIndent(),
        )
    }
}
