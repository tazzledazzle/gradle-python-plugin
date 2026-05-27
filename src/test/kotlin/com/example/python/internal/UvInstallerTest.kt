package com.example.python.internal

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UvInstallerTest {
    @Test
    fun `installIfAbsent is idempotent for uv binary path`() {
        val installDir = createTempDir(prefix = "uv-installer")
        val platform = PlatformSpec(os = "Linux", arch = "x86_64")
        val uvBin = File(installDir, ".gradle/python/uv/0.4.0/uv")
        uvBin.parentFile.mkdirs()
        uvBin.writeText("#!/bin/sh\necho uv\n")
        uvBin.setExecutable(true)

        val first = UvInstaller.installIfAbsent("0.4.0", installDir, platform)
        val second = UvInstaller.installIfAbsent("0.4.0", installDir, platform)

        assertEquals(first.absolutePath, second.absolutePath)
        assertTrue(first.exists())
        assertTrue(first.canExecute())
    }

    @Test
    fun `buildArchiveName maps linux x86_64`() {
        val platform = PlatformSpec(os = "Linux", arch = "x86_64")
        assertEquals(
            "uv-x86_64-unknown-linux-musl.tar.gz",
            UvInstaller.buildArchiveName(platform),
        )
    }
}
