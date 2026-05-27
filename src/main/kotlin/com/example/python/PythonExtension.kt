package com.example.python

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class PythonExtension {
    abstract val pythonVersion: Property<String>
    abstract val condaVersion: Property<String>
    abstract val condaInstaller: Property<String>
    abstract val condaRepoUrl: Property<String>
    abstract val condaRepoUsername: Property<String>

    /**
     * Optional explicit password. When unset, the plugin reads the Gradle property
     * `${condaRepoCredentialsName}Password` (default property: `condaRepoPassword`).
     */
    abstract val condaRepoPassword: Property<String>

    /**
     * Base name for Conda repository credential Gradle properties (`{name}Username`, `{name}Password`).
     */
    abstract val condaRepoCredentialsName: Property<String>

    abstract val installDir: DirectoryProperty
    abstract val systemArch: Property<String>
    abstract val envManager: Property<String>
    abstract val uvVersion: Property<String>
    abstract val uvRepoUrl: Property<String>
}
