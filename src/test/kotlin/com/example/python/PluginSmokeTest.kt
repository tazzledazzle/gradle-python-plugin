package com.example.python

import com.example.python.tasks.EnvSetupTask
import com.example.python.tasks.PythonExec
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginSmokeTest {
    @Test
    fun `plugin registers extension and tasks`() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(PythonPlugin::class.java)

        val extension = project.extensions.findByType(PythonExtension::class.java)
        val envSetup = project.tasks.findByName("envSetup")
        val pythonExec = project.tasks.findByName("pythonExec")

        assertNotNull(extension)
        assertNotNull(envSetup)
        assertNotNull(pythonExec)
        assertTrue(envSetup is EnvSetupTask)
        assertTrue(pythonExec is PythonExec)
    }
}
