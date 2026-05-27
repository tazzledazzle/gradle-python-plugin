package com.example.python

import com.example.python.tasks.EnvSetupTask
import com.example.python.tasks.PythonExec
import org.gradle.api.Plugin
import org.gradle.api.Project

class PythonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("python", PythonExtension::class.java)
        project.tasks.register("envSetup", EnvSetupTask::class.java)
        project.tasks.register("pythonExec", PythonExec::class.java)
    }
}
