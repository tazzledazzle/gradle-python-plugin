package com.tazzledazzle.python.tasks

import com.tazzledazzle.python.service.PythonEnvService
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class PythonExec : DefaultTask() {
    @get:ServiceReference("pythonEnvService")
    abstract val envService: Property<PythonEnvService>

    /**
     * Python script file executed via managed `python` (mutually exclusive with [venvExec] and [executable]).
     */
    @get:InputFile
    @get:Optional
    abstract val script: RegularFileProperty

    /**
     * Explicit executable path or command name on PATH (used when [venvExec] and [script] are not set).
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

    /**
     * Optional file to persist captured stdout after execution completes.
     */
    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

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
        logger.info("Executing command: $command")
        val process =
            ProcessBuilder(command)
                .directory(project.projectDir)
                .start()

        val out = process.inputStream.bufferedReader().use { it.readText() }
        val err = process.errorStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        logger.info("Output is $out")
        logger.info("Error Output is $err")
        stdout.set(out)
        stderr.set(err)
        exitValue.set(code)

        outputFile.orNull?.asFile?.let { file ->
            file.parentFile.mkdirs()
            file.writeText(out)
        }

        if (code != 0 && !ignoreExitValue.get()) {
            throw GradleException(
                "Python process '${command.first()}' exited with code $code.\n" +
                    "stdout:\n$out\nstderr:\n$err",
            )
        }
    }

    internal fun buildCommand(): List<String> {
        if (script.isPresent && venvExec.isPresent) {
            throw GradleException("PythonExec 'script' cannot be used together with 'venvExec'.")
        }
        if (script.isPresent && executable.isPresent) {
            throw GradleException("PythonExec 'script' cannot be used together with 'executable'.")
        }

        val executableCommand = resolveExecutableCommand()
        return if (script.isPresent) {
            listOf(executableCommand, script.get().asFile.canonicalFile.absolutePath) + arguments.get()
        } else {
            listOf(executableCommand) + arguments.get()
        }
    }

    private fun resolveExecutableCommand(): String =
        when {
            script.isPresent ->
                envService.get().resolveExecutable("python").absolutePath
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
                    "PythonExec requires 'script', 'venvExec', or 'executable'.",
                )
        }
}
