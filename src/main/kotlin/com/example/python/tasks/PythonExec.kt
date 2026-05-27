package com.example.python.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class PythonExec : DefaultTask() {
    @get:Input
    abstract val executable: Property<String>

    @get:Input
    abstract val arguments: ListProperty<String>

    @get:Internal
    abstract val stdout: Property<String>

    @get:Internal
    abstract val stderr: Property<String>

    @get:Internal
    abstract val exitValue: Property<Int>

    init {
        arguments.convention(emptyList())
        stdout.convention("")
        stderr.convention("")
        exitValue.convention(-1)
    }

    @TaskAction
    fun executeProcess() {
        val process = ProcessBuilder(listOf(executable.get()) + arguments.get())
            .directory(project.projectDir)
            .start()

        val out = process.inputStream.bufferedReader().use { it.readText() }
        val err = process.errorStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()

        stdout.set(out)
        stderr.set(err)
        exitValue.set(code)

        if (code != 0) {
            throw GradleException("Python process failed with exit code $code")
        }
    }
}
