# Migration Guide

Upgrade from pre-v1 shell scripts and Gradle `doLast` workarounds to the `com.tazzledazzle.python` plugin APIs.

See also: [DSL reference](DSL.md) for the full property list.

## Overview

Before v1.0, teams often ran Python via `Exec` tasks, hard-coded interpreter paths, or manual exit-code checks in `doLast`. The plugin provides `PythonExec`, shared environment bootstrap via `PythonEnvService`, and first-class policy controls.

## Workaround replacement table

| Old pattern | Plugin replacement |
|-------------|-------------------|
| `doLast { if (exitValue != 0) throw GradleException(...) }` | `ignoreExitValue.set(false)` (default) or `ignoreExitValue.set(true)` for warn-only |
| Hard-coded conda/venv path in `executable` | `venvExec.set("pytest")` (or tool name) + `dependsOn("envSetup")` |
| `executable.set(python)` + `arguments.set(listOf("script.py"))` | `script.set(layout.projectDirectory.file("script.py"))` (v1.1+) |
| Legacy `useVenv` toggle | Removed — use `venvExec` |
| Plain-text repo password in build script | `condaRepoPassword` DSL or `condaRepoPassword` in `gradle.properties` |

## Exit-code policy

**Before (manual check):**

```kotlin
tasks.register<Exec>("lint") {
    commandLine("ruff", "check", "src")
    isIgnoreExitValue = true
    doLast {
        if (executionResult.get().exitValue != 0) {
            logger.warn("Ruff reported issues")
        }
    }
}
```

**After (plugin-native):**

```kotlin
tasks.register<PythonExec>("lint") {
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

## Managed environment tools

**Before:**

```kotlin
executable.set("/opt/conda/envs/myenv/bin/pytest")
arguments.set(listOf("tests/"))
```

**After:**

```kotlin
python {
    pythonVersion.set("3.12.0")
}

tasks.named("check") {
    dependsOn("envSetup")
}

tasks.register<PythonExec>("runTests") {
    dependsOn("envSetup")
    venvExec.set("pytest")
    arguments.set(listOf("tests/", "-v"))
}
```

## Script files (v1.1+)

**Before:**

```kotlin
executable.set("/usr/bin/python3")
arguments.set(listOf("scripts/check.py", "--strict"))
```

**After:**

```kotlin
tasks.register<PythonExec>("runCheck") {
    dependsOn("envSetup")
    script.set(layout.projectDirectory.file("scripts/check.py"))
    arguments.set(listOf("--strict"))
}
```

`script` is mutually exclusive with `venvExec` and `executable`.

## Private Conda mirrors

Configure credentials via `gradle.properties` (`condaRepoUsername`, `condaRepoPassword`) or the DSL. See [conda-repo-password decision](decisions/conda-repo-password.md).
