## `ignoreExitValue` - Why It Would Be First-Class

The short answer: **data pipeline and quality gate tasks where a non-zero exit is semantically meaningful but not a build failure.**

Concrete cases:

**`pytest` with partial failures in a coverage gate.** `pytest` exits `1` when tests fail, `2` on collection error, `5` when no tests were found. If you're running `pytest` as a pre-merge quality signal rather than a hard gate - say, in a reporting task that aggregates results across modules - you want the output file written and the build to continue so downstream tasks (a JUnit XML publisher, a Slack notifier) can consume it. Without `ignoreExitValue`, the `GradleException` short-circuits the whole graph.

**`pip check` or `pip install --dry-run`.** These exit non-zero to report dependency conflicts. You may want to log the conflict, write a report, and continue - not abort the build - especially in a dependency audit task that runs alongside the main build.

**`mypy` or `ruff` in warn-only mode.** Lint tools exit non-zero on violations. If your policy is "warn on main, fail on PR branches," you need a per-task `ignoreExitValue` flag so the same task definition can be toggled by environment without duplicating the task.

**The alternative without it** is that every consumer has to wrap their `PythonExec` task in a `doLast` block that checks `exitValue.get()` manually and conditionally throws - which is exactly the boilerplate a first-class property eliminates.

**Verdict:** it belongs in v1.0, not backlog. The implementation is three lines - a nullable `Property<Boolean>` that gates the `GradleException` in `PythonExecutor` - and the cost of not having it is every data-pipeline or lint use case requiring a workaround.

---

Now the updated TDD sections:

## Updated: Open Questions

The following were resolved and promoted to decisions:

| Question | Resolution |
|---|---|
| `ignoreExitValue` first-class on `PythonExec`? | **Yes, v1.0.** Needed for lint/test/audit tasks where non-zero exit is informational, not fatal. |
| `BuildService` for shared conda process pool? | **Yes, v1.0.** See new section below. |
| Support `uv` as alternative env manager? | **Yes, v1.0.** See new section below. |
| `condaRepoPassword` as Gradle credentials API type? | Open - owner: Terence, target: before v1.0 cut. |

## Addition 1: `PythonEnvService` - BuildService for Shared Conda State

### Why

Without a `BuildService`, every `PythonExec` task that runs in a parallel Gradle build will independently attempt to resolve the conda root path, and any task that triggers `envSetup` while another is already mid-install will race on the same directory. Additionally, repeated PATH resolution and interpreter validation across 10+ tasks in a large build adds measurable overhead. A `BuildService` with `maxParallelUsages = 1` serializes environment bootstrap and shares the resolved conda root as build-scoped state.

### Design

`PythonEnvService` is a `BuildService<PythonEnvService.Params>` registered by `PythonPlugin` at apply time. It holds the resolved conda root and Python binary path as service-level state computed once on first access. All `PythonExec` and `EnvSetupTask` instances declare it as a `@ServiceReference`, which Gradle uses to enforce the `maxParallelUsages` constraint and to inject the shared instance.

```kotlin
package com.example.python.service

import com.example.python.internal.CondaInstallSpec
import com.example.python.internal.CondaInstaller
import com.example.python.internal.PlatformSpec
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File

abstract class PythonEnvService : BuildService<PythonEnvService.Params>, AutoCloseable {

    interface Params : BuildServiceParameters {
        val pythonVersion: Property<String>
        val condaVersion: Property<String>
        val condaInstaller: Property<String>
        val condaRepoUrl: Property<String>
        val condaRepoUsername: Property<String>
        val condaRepoPassword: Property<String>
        val installDir: Property<File>
        val systemArch: Property<String>
        val envManagerType: Property<String>   // "conda" | "uv"
    }

    // Computed once, shared across all tasks in the build
    val platform: PlatformSpec by lazy {
        CondaInstaller.detectPlatform(parameters.systemArch.orNull)
    }

    val condaRoot: File by lazy {
        val spec = buildCondaSpec()
        CondaInstaller.installIfAbsent(spec)
    }

    fun resolveExecutable(execName: String): File =
        when (parameters.envManagerType.get()) {
            "uv"    -> UvEnvResolver.resolve(parameters.installDir.get(), execName, platform)
            else    -> com.example.python.internal.VenvExecutableResolver.resolve(
                condaRoot, parameters.pythonVersion.get(), execName, platform
            )
        }

    private fun buildCondaSpec() = CondaInstallSpec(
        version       = parameters.condaVersion.get(),
        installer     = parameters.condaInstaller.get(),
        repoUrl       = parameters.condaRepoUrl.get(),
        installDir    = parameters.installDir.get(),
        platform      = platform,
        repoUsername  = parameters.condaRepoUsername.orNull,
        repoPassword  = parameters.condaRepoPassword.orNull,
        repoHeaders   = emptyMap()
    )

    override fun close() {
        // No persistent resources to release; hook available for future
        // connection pool cleanup (e.g., conda daemon mode)
    }
}
```

### Wiring in `PythonPlugin`

```kotlin
// In PythonPlugin.apply():
val envService = project.gradle.sharedServices.registerIfAbsent(
    "pythonEnvService",
    PythonEnvService::class.java
) { spec ->
    spec.maxParallelUsages.set(1)   // serialize env bootstrap; executor tasks run in parallel
    spec.parameters.pythonVersion.set(extension.pythonVersion)
    spec.parameters.condaVersion.set(extension.condaVersion)
    spec.parameters.condaInstaller.set(extension.condaInstaller)
    spec.parameters.condaRepoUrl.set(extension.condaRepoUrl)
    spec.parameters.condaRepoUsername.set(extension.condaRepoUsername)
    spec.parameters.condaRepoPassword.set(extension.condaRepoPassword)
    spec.parameters.installDir.set(resolvedInstallDir)
    spec.parameters.systemArch.set(extension.systemArch)
    spec.parameters.envManagerType.set(extension.envManager)  // "conda" | "uv"
}

project.tasks.withType(PythonExec::class.java).configureEach { task ->
    task.envService.set(envService)
    task.usesService(envService)    // declares the service dependency for parallel constraint
}

project.tasks.withType(EnvSetupTask::class.java).configureEach { task ->
    task.envService.set(envService)
    task.usesService(envService)
}
```

### Effect on Parallel Builds

`maxParallelUsages = 1` gates only the service acquisition - the `condaRoot` lazy property means the actual install runs exactly once. Once `condaRoot` is resolved and cached in the service, subsequent `resolveExecutable()` calls from parallel `PythonExec` tasks are reads against an already-materialized `File`, which are safe to run concurrently. The service does not serialize task execution, only environment bootstrap.

## Addition 2: `uv` Support in v1.0

### Why in v1.0, Not Backlog

`uv` (Astral) has become the dominant Python environment manager for projects that don't need Conda's scientific package ecosystem. It is 10-100x faster than pip for dependency resolution and install, ships as a single static binary with no runtime dependencies, and manages Python version installation natively as of v0.3+. For build systems work - where install speed directly impacts CI cycle time - defaulting to Conda while leaving `uv` as a future concern would make the plugin a poor fit for the majority of non-scientific Python projects from day one.

### Design: `envManager` as the Selection Axis

A new `envManager` property on `PythonExtension` selects the environment backend. The `PythonEnvService` routes all resolver calls through this selection. The task surface (`PythonExec`, `EnvSetupTask`) is identical regardless of backend - the abstraction is fully internal.

```kotlin
// PythonExtension additions
val envManager: Property<String> = objects.property(String::class.java)
    .convention("conda")   // "conda" | "uv"

// uv version pin (used when envManager = "uv")
val uvVersion: Property<String> = objects.property(String::class.java)
    .convention("0.4.0")

val uvRepoUrl: Property<String> = objects.property(String::class.java)
    .convention("https://github.com/astral-sh/uv/releases/download/")
```

### `UvInstaller.kt`

```kotlin
package com.example.python.internal

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object UvInstaller {

    /**
     * Downloads and installs the uv binary for the current platform.
     * Install path: <installDir>/.gradle/python/uv/<version>/uv[.exe]
     * Idempotent: skips if binary already exists.
     */
    fun installIfAbsent(
        version: String,
        installDir: File,
        platform: PlatformSpec,
        repoUrl: String = "https://github.com/astral-sh/uv/releases/download/"
    ): File {
        val binName = if (platform.os == "Windows") "uv.exe" else "uv"
        val uvBin = File(installDir, ".gradle/python/uv/$version/$binName")

        if (uvBin.exists() && uvBin.canExecute()) return uvBin

        uvBin.parentFile.mkdirs()

        val archiveName = buildArchiveName(platform)
        val downloadUrl = "${repoUrl.trimEnd('/')}/$version/$archiveName"

        val tempArchive = File(System.getProperty("java.io.tmpdir"), archiveName)
        downloadFile(downloadUrl, tempArchive)
        extractBinary(tempArchive, uvBin, platform)
        uvBin.setExecutable(true)

        return uvBin
    }

    /**
     * Installs the requested Python version via uv and creates a project venv.
     * Equivalent to: uv python install <version> && uv venv .venv
     */
    fun setupEnv(uvBin: File, pythonVersion: String, projectDir: File) {
        runCommand(listOf(uvBin.absolutePath, "python", "install", pythonVersion), projectDir)
        runCommand(listOf(uvBin.absolutePath, "venv", "--python", pythonVersion, ".venv"), projectDir)
    }

    private fun buildArchiveName(platform: PlatformSpec): String {
        val target = when {
            platform.os == "Windows" && platform.arch == "x86_64" -> "x86_64-pc-windows-msvc"
            platform.os == "Linux"   && platform.arch == "x86_64" -> "x86_64-unknown-linux-musl"
            platform.os == "Linux"   && platform.arch == "arm64"  -> "aarch64-unknown-linux-musl"
            platform.os == "MacOSX"  && platform.arch == "arm64"  -> "aarch64-apple-darwin"
            platform.os == "MacOSX"  && platform.arch == "x86_64" -> "x86_64-apple-darwin"
            else -> throw IllegalStateException("Unsupported platform: ${platform.os}/${platform.arch}")
        }
        val ext = if (platform.os == "Windows") "zip" else "tar.gz"
        return "uv-$target.$ext"
    }

    private fun downloadFile(url: String, dest: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.inputStream.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun extractBinary(archive: File, dest: File, platform: PlatformSpec) {
        val command = if (platform.os == "Windows") {
            listOf("powershell", "-Command",
                "Expand-Archive -Path '${archive.absolutePath}' -DestinationPath '${dest.parent}' -Force")
        } else {
            listOf("tar", "-xzf", archive.absolutePath, "-C", dest.parent, "--strip-components=1", "uv")
        }
        runCommand(command, dest.parentFile)
    }

    private fun runCommand(command: List<String>, workingDir: File) {
        val exit = ProcessBuilder(command).directory(workingDir).inheritIO().start().waitFor()
        if (exit != 0) throw IllegalStateException("Command failed (exit $exit): $command")
    }
}
```

### `UvEnvResolver.kt`

```kotlin
package com.example.python.internal

import java.io.File

object UvEnvResolver {

    /**
     * Resolves a named executable from the uv-managed .venv.
     * uv creates the venv at <projectDir>/.venv by convention.
     */
    fun resolve(projectDir: File, execName: String, platform: PlatformSpec): File {
        val venvDir = File(projectDir, ".venv")
        return if (platform.os == "Windows") {
            val scripts = File(venvDir, "Scripts/$execName.exe")
            val direct  = File(venvDir, "$execName.exe")
            if (scripts.exists()) scripts else direct
        } else {
            File(venvDir, "bin/$execName")
        }
    }
}
```

### Consumer Usage

```kotlin
// Switch to uv backend - everything else is identical
pythonPlugin {
    envManager    = "uv"
    pythonVersion = "3.12.0"
    uvVersion     = "0.4.0"
}

tasks.register<PythonExec>("runTests") {
    venvExec = "pytest"
    arguments.addAll("tests/", "-v")
    outputFile.set(layout.buildDirectory.file("test-results/pytest.xml"))
}

tasks.register<PythonExec>("installDeps") {
    venvExec  = "uv"          // uv itself as the exec (for `uv sync`, `uv add`, etc.)
    arguments.addAll("sync")
}
```

## Addition 3: `ignoreExitValue` on `PythonExec`

```kotlin
/**
 * When true, a non-zero exit code from the Python process does NOT throw a
 * GradleException. The exit code is still captured in `exitValue` for
 * inspection in doLast blocks or by downstream tasks.
 *
 * Use cases:
 *   - pytest / ruff / mypy in warn-only mode (exit 1 = violations found, not build broken)
 *   - pip check in audit/reporting tasks
 *   - Any script where non-zero exit is informational
 *
 * Default: false (build fails on non-zero exit)
 */
@get:Input
abstract val ignoreExitValue: Property<Boolean>

// In PythonPlugin.configureEach:
task.ignoreExitValue.convention(false)

// In PythonExec.execute(), replace the hard throw with:
if (result.exitCode != 0 && !ignoreExitValue.get()) {
    throw GradleException(
        "Python script '${resolvedScript.name}' exited with code ${result.exitCode}.\n" +
        "stdout:\n${result.stdout}\nstderr:\n${result.stderr}"
    )
}
```

## Updated Trade-offs

| Decision | Choice | Rationale | Rejected Alternative |
|---|---|---|---|
| `BuildService` max parallelism | `maxParallelUsages = 1` | Serializes bootstrap only; post-bootstrap executor calls are concurrent reads | Per-task isolation - races on shared install directory |
| `uv` vs Conda as default | Conda default, `uv` opt-in | Conda covers scientific/C-extension packages without a compiler; uv is faster for pure-Python stacks | `uv` as default - wrong for numpy/scipy/torch consumers |
| `envManager` as string property | `"conda"` or `"uv"` | Simple, readable in DSL; validated at task execution time | Sealed class or enum - cleaner type-safety but heavier DSL for a two-value set |
| `ignoreExitValue` default | `false` | Fail-fast is the correct default for build tasks | `true` default - masks real failures silently |

## Updated Decision Log

| Date | Decision | Rationale |
|---|---|---|
| 2026-05-26 | Miniforge3 as default Conda installer | Avoids Anaconda TOS in CI; conda-forge covers scientific packages |
| 2026-05-26 | `PythonExecutor` promoted to `BuildService` in v1.0 | Parallel build correctness requires serialized bootstrap; shared lazy state eliminates redundant installs |
| 2026-05-26 | `uv` support in v1.0 via `envManager = "uv"` | Install speed advantage is directly felt in CI; deferring creates a two-version migration problem later |
| 2026-05-26 | `ignoreExitValue` in v1.0 | Required for lint/test/audit tasks; implementation is trivial; absence forces every consumer to write workarounds |
| 2026-05-26 | `venvExec` replaces raw `pythonExecutable` when `useVenv = true` | Matches reference plugin pattern; pip/pytest/black dispatch is a first-class use case |