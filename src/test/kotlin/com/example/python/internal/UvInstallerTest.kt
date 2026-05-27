package com.example.python.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UvInstallerTest {
    @Test
    fun `installIfAbsent is idempotent for uv binary path`() {
        val installDir = createTempDir(prefix = "uv-installer")
        val platform = PlatformSpec(os = "Linux", arch = "x86_64")

        val first = UvInstaller.installIfAbsent("0.4.0", installDir, platform)
        val second = UvInstaller.installIfAbsent("0.4.0", installDir, platform)

        assertEquals(first.absolutePath, second.absolutePath)
        assertTrue(first.exists())
        assertTrue(first.canExecute())
    }
}
