# Gradle Python Plugin DSL Reference (v1.1)

This document lists **implemented** properties and tasks. See [Migration Guide](MIGRATION.md) for upgrading from pre-v1 workarounds.

## Plugin

```kotlin
plugins {
    id("com.tazzledazzle.python") version "0.1.0"
}
```

Access the extension as `python { }` (extension name: `python`).

## Extension: `python`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `pythonVersion` | `String` | `"3.12.0"` | Python version for environment bootstrap |
| `envManager` | `String` | `"conda"` | `"conda"` or `"uv"` |
| `installDir` | `Directory` | project directory | Root used for env/bootstrap paths |
| `systemArch` | `String` | (auto) | Optional override: `x86_64` or `arm64` |
| `condaVersion` | `String` | `"24.11.3-0"` | Miniforge/Conda bootstrap version |
| `condaInstaller` | `String` | `"miniforge"` | Installer flavor label |
| `condaRepoUrl` | `String` | Miniforge GitHub releases URL | Download base URL |
| `condaRepoUsername` | `String` | `""` | HTTP basic auth username for private repo |
| `condaRepoPassword` | `String` | (see credentials) | Explicit password override |
| `condaRepoCredentialsName` | `String` | `"condaRepo"` | Gradle credentials name for password lookup |
| `uvVersion` | `String` | `"0.4.0"` | uv release version |
| `uvRepoUrl` | `String` | Astral uv releases URL | uv download base URL |

### Private Conda repository credentials

Password resolution order:

1. `python { condaRepoPassword.set("...") }` if set in build script
2. Gradle property `${condaRepoCredentialsName}Password` (default: `condaRepoPassword`)

Example `~/.gradle/gradle.properties`:

```properties
condaRepoUsername=myuser
condaRepoPassword=mypassword
```

## Tasks

### `PythonExec`

Runs a process using a managed-environment tool, a Python script, or an explicit executable.

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `script` | `RegularFile` | one of `script` / `venvExec` / `executable` | `.py` file run via managed `python` |
| `venvExec` | `String` | one of `script` / `venvExec` / `executable` | Tool name resolved via `PythonEnvService` (e.g. `"pytest"`) |
| `executable` | `String` | one of `script` / `venvExec` / `executable` | Explicit executable path or PATH command |
| `arguments` | `List<String>` | no | Arguments passed after executable/script |
| `outputFile` | `RegularFile` | no | Persists captured stdout after execution |
| `ignoreExitValue` | `Boolean` | no (default `false`) | When `true`, non-zero exit does not fail the build |
| `stdout` | `String` | output | Captured stdout (read-only after execution) |
| `stderr` | `String` | output | Captured stderr |
| `exitValue` | `Int` | output | Process exit code |

`script` is mutually exclusive with `venvExec` and `executable`.

#### Managed tool example

```kotlin
tasks.register<PythonExec>("runPytest") {
    dependsOn("envSetup")
    venvExec.set("pytest")
    arguments.set(listOf("tests/", "-v"))
}
```

#### Script file example

```kotlin
tasks.register<PythonExec>("runCheck") {
    dependsOn("envSetup")
    script.set(layout.projectDirectory.file("scripts/check.py"))
    arguments.set(listOf("--strict"))
}
```

#### Explicit executable example

```kotlin
tasks.register<PythonExec>("runEcho") {
    executable.set("sh")
    arguments.set(listOf("-c", "printf hello"))
}
```

#### Persist stdout to a file

```kotlin
tasks.register<PythonExec>("captureOut") {
    venvExec.set("pytest")
    arguments.set(listOf("tests/", "-q"))
    outputFile.set(layout.buildDirectory.file("reports/python-out.txt"))
}
```

#### Warn-only quality gate

Use `ignoreExitValue` when a non-zero exit is informational (lint, audit, reporting):

```kotlin
tasks.register<PythonExec>("ruffCheck") {
    dependsOn("envSetup")
    venvExec.set("ruff")
    arguments.set(listOf("check", "src"))
    ignoreExitValue.set(true)
    doLast {
        if (exitValue.get() != 0) {
            logger.warn("Ruff reported issues (exit ${exitValue.get()})")
        }
    }
}
```

#### Strict vs warn-only

Fail-fast (default):

```kotlin
tasks.register<PythonExec>("strictLint") {
    venvExec.set("ruff")
    arguments.set(listOf("check", "src"))
    // ignoreExitValue defaults to false — build fails on violations
}
```

Warn-only with post-task logging:

```kotlin
tasks.register<PythonExec>("warnLint") {
    venvExec.set("ruff")
    arguments.set(listOf("check", "src"))
    ignoreExitValue.set(true)
    doLast {
        if (exitValue.get() != 0) {
            logger.lifecycle("Lint finished with exit ${exitValue.get()} — see stdout/stderr on task")
        }
    }
}
```

See [MIGRATION.md](MIGRATION.md) for replacing manual `doLast` exit checks.

### `EnvSetupTask`

Bootstraps the configured environment (`conda` or `uv`) once per build via shared `PythonEnvService`.

Registered by default as `envSetup`.
