package com.tazzledazzle.python

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "CI_NETWORK_TESTS", matches = "true")
class BackendParityFunctionalTest {
    @Test
    fun `conda envSetup creates install sentinel without stub`() {
        val projectDir = Files.createTempDirectory("backend-parity-functional").toFile()
        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "backend-parity"
            """.trimIndent(),
        )
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("com.tazzledazzle.python")
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("envSetup")
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":envSetup")?.outcome)
        assertTrue(File(projectDir, ".gradle/python/conda/24.11.3-0/.installed").exists())
    }
}
