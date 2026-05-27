package com.example.python

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class PythonExtension {
    abstract val pythonVersion: Property<String>
    abstract val condaVersion: Property<String>
    abstract val condaInstaller: Property<String>
    abstract val condaRepoUrl: Property<String>
    abstract val condaRepoUsername: Property<String>
    abstract val condaRepoPassword: Property<String>
    abstract val installDir: DirectoryProperty
    abstract val systemArch: Property<String>
    abstract val envManager: Property<String>
    abstract val uvVersion: Property<String>
    abstract val uvRepoUrl: Property<String>
}
