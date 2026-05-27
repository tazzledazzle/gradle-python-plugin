package com.example.python

import com.example.python.service.PythonEnvService
import com.example.python.tasks.EnvSetupTask
import com.example.python.tasks.PythonExec
import org.gradle.api.Plugin
import org.gradle.api.Project

class PythonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("python", PythonExtension::class.java)
        configureExtensionDefaults(extension, project)

        val envService = project.gradle.sharedServices.registerIfAbsent(
            "pythonEnvService",
            PythonEnvService::class.java
        ) { spec ->
            spec.maxParallelUsages.set(1)
            spec.parameters.pythonVersion.set(extension.pythonVersion)
            spec.parameters.condaVersion.set(extension.condaVersion)
            spec.parameters.condaInstaller.set(extension.condaInstaller)
            spec.parameters.condaRepoUrl.set(extension.condaRepoUrl)
            spec.parameters.condaRepoUsername.set(extension.condaRepoUsername)
            spec.parameters.condaRepoPassword.set(extension.condaRepoPassword)
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

    private fun configureExtensionDefaults(extension: PythonExtension, project: Project) {
        extension.pythonVersion.convention("3.12.0")
        extension.condaVersion.convention("24.11.3-0")
        extension.condaInstaller.convention("miniforge")
        extension.condaRepoUrl.convention("https://github.com/conda-forge/miniforge/releases/latest/download/")
        extension.installDir.convention(project.layout.projectDirectory)
        extension.envManager.convention("conda")
        extension.uvVersion.convention("0.4.0")
        extension.uvRepoUrl.convention("https://github.com/astral-sh/uv/releases/download/")
    }
}
