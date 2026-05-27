package com.example.python.tasks

import com.example.python.service.PythonEnvService
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class PythonExec : DefaultTask() {
    @get:ServiceReference("pythonEnvService")
    abstract val envService: Property<PythonEnvService>

    /**
     * Explicit executable path or command name on PATH (used when [venvExec] is not set).
     */
    @get:Input
    @get:Optional
    abstract val executable: Property<String>

    /**
     * Named executable resolved from the managed environment (conda or uv) via [PythonEnvService].
     */
    @get:Input
    @get:Optional
    abstract val venvExec: Property<String>

    @get:Input
    abstract val arguments: ListProperty<String>

    @get:Input
    abstract val ignoreExitValue: Property<Boolean>

    @get:Internal
    abstract val stdout: Property<String>

    @get:Internal
    abstract val stderr: Property<String>

    @get:Internal
    abstract val exitValue: Property<Int>

    init {
        arguments.convention(emptyList())
        ignoreExitValue.convention(false)
        stdout.convention("")
        stderr.convention("")
        exitValue.convention(-1)
    }

    @TaskAction
    fun executeProcess() {
        val command = buildCommand()
        val process =
            ProcessBuilder(command)
                .directory(project.projectDir)
                .start()

        val out = process.inputStream.bufferedReader().use { it.readText() }
        val err = process.errorStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()

        stdout.set(out)
        stderr.set(err)
        exitValue.set(code)

        if (code != 0 && !ignoreExitValue.get()) {
            throw GradleException(
                "Python process '${command.first()}' exited with code $code.\n" +
                    "stdout:\n$out\nstderr:\n$err",
            )
        }
    }

    internal fun buildCommand(): List<String> = listOf(resolveExecutableCommand()) + arguments.get()

    private fun resolveExecutableCommand(): String =
        when {
            venvExec.isPresent ->
                envService.get().resolveExecutable(venvExec.get()).absolutePath
            executable.isPresent -> {
                val exe = executable.get()
                val candidate = File(exe)
                when {
                    candidate.isAbsolute -> candidate.absolutePath
                    candidate.exists() -> candidate.absolutePath
                    else -> exe
                }
            }
            else ->
                throw GradleException(
                    "PythonExec requires either 'venvExec' (managed environment) or 'executable' (explicit path).",
                )
        }
}
