package com.tazzledazzle.python

import com.tazzledazzle.python.service.PythonEnvService
import com.tazzledazzle.python.tasks.EnvSetupTask
import com.tazzledazzle.python.tasks.PythonExec
import org.gradle.api.Plugin
import org.gradle.api.Project

class PythonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("python", PythonExtension::class.java)
        configureExtensionDefaults(extension, project)

        val condaPassword =
            extension.condaRepoPassword.orElse(
                extension.condaRepoCredentialsName.flatMap { name ->
                    project.providers.gradleProperty("${name}Password")
                },
            ).orElse("")
        val condaUsername =
            extension.condaRepoUsername.orElse(
                extension.condaRepoCredentialsName.flatMap { name ->
                    project.providers.gradleProperty("${name}Username")
                },
            ).orElse("")

        val envService =
            project.gradle.sharedServices.registerIfAbsent(
                "pythonEnvService",
                PythonEnvService::class.java,
            ) { spec ->
                spec.maxParallelUsages.set(1)
                spec.parameters.pythonVersion.set(extension.pythonVersion)
                spec.parameters.condaVersion.set(extension.condaVersion)
                spec.parameters.condaInstaller.set(extension.condaInstaller)
                spec.parameters.condaRepoUrl.set(extension.condaRepoUrl)
                spec.parameters.condaRepoUsername.set(condaUsername)
                spec.parameters.condaRepoPassword.set(condaPassword)
                spec.parameters.installDir.set(extension.installDir)
                spec.parameters.systemArch.set(extension.systemArch)
                spec.parameters.envManagerType.set(extension.envManager)
                spec.parameters.uvVersion.set(extension.uvVersion)
                spec.parameters.uvRepoUrl.set(extension.uvRepoUrl)
            }

        project.tasks.register("envSetup", EnvSetupTask::class.java)
        project.tasks.register("pythonExec", PythonExec::class.java)

        project.tasks.withType(PythonExec::class.java).configureEach { task ->
            task.ignoreExitValue.convention(false)
            task.envService.set(envService)
            task.usesService(envService)
        }

        project.tasks.withType(EnvSetupTask::class.java).configureEach { task ->
            task.envService.set(envService)
            task.usesService(envService)
        }
    }

    private fun configureExtensionDefaults(
        extension: PythonExtension,
        project: Project,
    ) {
        extension.pythonVersion.convention("3.12.0")
        extension.condaVersion.convention("24.11.3-0")
        extension.condaInstaller.convention("miniforge")
        extension.condaRepoUrl.convention(
            "https://github.com/conda-forge/miniforge/releases/download/",
        )
        extension.condaRepoUsername.convention("")
        extension.condaRepoCredentialsName.convention("condaRepo")
        extension.installDir.convention(project.layout.projectDirectory)
        extension.envManager.convention("conda")
        extension.uvVersion.convention("0.4.0")
        extension.uvRepoUrl.convention("https://github.com/astral-sh/uv/releases/download/")
    }
}
