package com.tazzledazzle.python

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PythonExecScriptFunctionalTest {
    @Test
    fun `pythonExec script mode succeeds with offline managed python stub`() {
        val projectDir = Files.createTempDirectory("python-exec-script-functional").toFile()
        writeSettings(projectDir)
        writeBuildScript(projectDir)
        File(projectDir, "scripts/hello.py").apply {
            parentFile.mkdirs()
            writeText("print('ignored')")
        }

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("runScript")
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":runScript")?.outcome)
        assertTrue(File(projectDir, "build/script-out.txt").exists())
        assertEquals("script-ok", File(projectDir, "build/script-out.txt").readText())
    }

    private fun writeSettings(projectDir: File) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "python-exec-script-functional"
            """.trimIndent(),
        )
    }

    private fun writeBuildScript(projectDir: File) {
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.tazzledazzle.python")
            }

            val condaSentinel = layout.projectDirectory.file(
                ".gradle/python/conda/24.11.3-0/.installed",
            )
            val managedPython = layout.projectDirectory.file(
                ".gradle/python/conda/24.11.3-0/envs/3.12.0/bin/python",
            )

            tasks.register("prepareScriptEnv") {
                outputs.files(condaSentinel, managedPython)
                doLast {
                    condaSentinel.asFile.parentFile.mkdirs()
                    condaSentinel.asFile.writeText("installed")
                    managedPython.asFile.parentFile.mkdirs()
                    managedPython.asFile.writeText("#!/bin/sh\nprintf 'script-ok'\n")
                    managedPython.asFile.setExecutable(true)
                }
            }

            tasks.register<com.tazzledazzle.python.tasks.PythonExec>("runScript") {
                dependsOn("prepareScriptEnv")
                script.set(layout.projectDirectory.file("scripts/hello.py"))
                outputFile.set(layout.buildDirectory.file("script-out.txt"))
            }
            """.trimIndent(),
        )
    }
}
