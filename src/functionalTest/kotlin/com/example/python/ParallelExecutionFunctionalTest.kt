package com.example.python

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParallelExecutionFunctionalTest {
    @Test
    fun `parallel env setup tasks complete using shared service`() {
        val projectDir = Files.createTempDirectory("python-plugin-functional").toFile()
        writeSettings(projectDir)
        writeBuildScript(projectDir)

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("envSetupA", "envSetupB", "--parallel")
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":envSetupA")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":envSetupB")?.outcome)
        assertTrue(File(projectDir, ".gradle/python/conda/24.11.3-0/.installed").exists())
    }

    private fun writeSettings(projectDir: File) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "functional-test-project"
            """.trimIndent(),
        )
    }

    private fun writeBuildScript(projectDir: File) {
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.example.python")
            }

            val condaSentinel = layout.projectDirectory.file(
                ".gradle/python/conda/24.11.3-0/.installed",
            )
            tasks.register("prepareCondaSentinel") {
                outputs.file(condaSentinel)
                doLast {
                    condaSentinel.asFile.parentFile.mkdirs()
                    condaSentinel.asFile.writeText("installed")
                }
            }

            tasks.register<com.example.python.tasks.EnvSetupTask>("envSetupA") {
                dependsOn("prepareCondaSentinel")
            }
            tasks.register<com.example.python.tasks.EnvSetupTask>("envSetupB") {
                dependsOn("prepareCondaSentinel")
            }
            """.trimIndent(),
        )
    }
}
