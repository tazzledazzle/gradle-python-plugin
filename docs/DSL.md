# Gradle Python Plugin DSL Reference (v1.0)

This document lists **implemented** properties and tasks only. Deferred APIs are documented in [V1.1 scope](#deferred-to-v11).

## Plugin

```kotlin
plugins {
    id("com.example.python")
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

Or use the Gradle credentials API in `settings.gradle.kts` / build logic with name `condaRepo`.

## Tasks

### `PythonExec`

Runs a process using either an explicit executable or a managed-environment tool.

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `venvExec` | `String` | one of `venvExec` / `executable` | Tool name resolved via `PythonEnvService` (e.g. `"pytest"`) |
| `executable` | `String` | one of `venvExec` / `executable` | Explicit executable path or command |
| `arguments` | `List<String>` | no | Arguments passed to the process |
| `ignoreExitValue` | `Boolean` | no (default `false`) | When `true`, non-zero exit does not fail the build |
| `stdout` | `String` | output | Captured stdout (read-only after execution) |
| `stderr` | `String` | output | Captured stderr |
| `exitValue` | `Int` | output | Process exit code |

Example (managed environment):

```kotlin
tasks.register<PythonExec>("runPytest") {
    venvExec.set("pytest")
    arguments.set(listOf("tests/", "-v"))
    ignoreExitValue.set(true) // warn-only quality gate
}
```

Example (explicit executable):

```kotlin
tasks.register<PythonExec>("runScript") {
    executable.set("/usr/bin/python3")
    arguments.set(listOf("scripts/check.py"))
}
```

### `EnvSetupTask`

Bootstraps the configured environment (`conda` or `uv`) once per build via shared `PythonEnvService`.

Registered by default as `envSetup`.

## Deferred to v1.1

The following appear in design notes but are **not** in v1.0:

| Feature | Status |
|---------|--------|
| `outputFile` on `PythonExec` | Planned v1.1 |
| `script` / script-file execution mode | Planned v1.1 |
| `useVenv` toggle (legacy name) | Removed; use `venvExec` |

See `docs/roadmap/V1-IMPLEMENTATION-PLAN.md` for backlog tracking.
