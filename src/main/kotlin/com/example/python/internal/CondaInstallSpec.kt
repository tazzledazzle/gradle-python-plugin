package com.example.python.internal

import java.io.File

data class CondaInstallSpec(
    val version: String,
    val installer: String,
    val repoUrl: String,
    val installDir: File,
    val platform: PlatformSpec,
    val repoUsername: String?,
    val repoPassword: String?,
    val repoHeaders: Map<String, String>
)
