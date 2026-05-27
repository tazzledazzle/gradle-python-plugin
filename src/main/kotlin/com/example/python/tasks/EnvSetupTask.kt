package com.example.python.tasks

import com.example.python.service.PythonEnvService
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.TaskAction

abstract class EnvSetupTask : DefaultTask() {
    @get:ServiceReference("pythonEnvService")
    abstract val envService: Property<PythonEnvService>

    @TaskAction
    fun setupEnvironment() {
        envService.get().bootstrapEnvironment()
    }
}
