# Gradle Python Plugin

[![CI](https://github.com/tazzledazzle/gradle-python-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/tazzledazzle/gradle-python-plugin/actions/workflows/ci.yml)
[![Gradle Plugin Portal](https://img.shields.io/badge/Gradle%20Plugin%20Portal-com.tazzledazzle.python-blue)](https://plugins.gradle.org/plugin/com.tazzledazzle.python)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Run Python tooling from Gradle builds with managed **Conda** or **uv** environments, safe parallel bootstrap, and first-class exit-code policy.

Built for JVM-first repos (Spring, Kotlin, Android backends) that need Python for scripts, data pipelines, ML assets, or quality gates — without shell wrappers or duplicated env setup.

## Features

- **`PythonExec` task** — run a managed tool (`pytest`, `ruff`, …) or an explicit executable; captures stdout, stderr, and exit code
- **`ignoreExitValue`** — warn-only lint/test/audit tasks without `doLast` boilerplate (default: fail on non-zero exit)
- **`PythonEnvService`** — Gradle `BuildService` serializes environment bootstrap (`maxParallelUsages = 1`) while executor tasks run in parallel
- **`envManager`** — `"conda"` (default, Miniforge) or `"uv"` (fast pure-Python stacks)
- **`EnvSetupTask`** — explicit bootstrap task (`envSetup`) when you want env ready before other work

## Requirements

- Gradle 8.x
- JDK 17+
- Network access on first run (downloads Miniforge or uv binary)

## Quick start

### 1. Apply the plugin

**Published (recommended for portfolio projects):**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

`build.gradle.kts`:

```kotlin
plugins {
    id("com.tazzledazzle.python") version "0.1.0"
}
```

> See the [latest version on the Plugin Portal](https://plugins.gradle.org/plugin/com.tazzledazzle.python).

**Composite / local development:**

```kotlin
// settings.gradle.kts
includeBuild("../gradle-python-plugin")
```

```kotlin
// build.gradle.kts
plugins {
    id("com.tazzledazzle.python")
}
```

### 2. Configure the environment

```kotlin
python {
    pythonVersion.set("3.12.0")
    envManager.set("uv") // or "conda" (default)
}
```

### 3. Bootstrap and run tools

```kotlin
tasks.named("check") {
    dependsOn("envSetup")
}

tasks.register<PythonExec>("runPytest") {
    dependsOn("envSetup")
    venvExec.set("pytest")
    arguments.set(listOf("tests/", "-v"))
}

tasks.register<PythonExec>("ruffCheck") {
    dependsOn("envSetup")
    venvExec.set("ruff")
    arguments.set(listOf("check", "src"))
    ignoreExitValue.set(true) // report violations without failing the build
}
```

## Configuration reference

Full property and task reference: **[docs/DSL.md](docs/DSL.md)**

| Extension property | Default | Description |
|--------------------|---------|-------------|
| `pythonVersion` | `3.12.0` | Python version for bootstrap |
| `envManager` | `conda` | `conda` or `uv` |
| `condaVersion` | `24.11.3-0` | Miniforge release |
| `uvVersion` | `0.4.0` | uv release when using `envManager = "uv"` |

Private Conda mirrors: set `condaRepoUsername` / `condaRepoPassword` in the DSL or `gradle.properties`. See [docs/decisions/conda-repo-password.md](docs/decisions/conda-repo-password.md).

## How it works

```text
  build.gradle.kts
        │
        ▼
  PythonPlugin ──► PythonEnvService (shared, serialized bootstrap)
        │                    │
        ├─ envSetup ─────────┤──► CondaInstaller / UvInstaller
        └─ PythonExec ───────┘──► resolveExecutable("pytest") → ProcessBuilder
```

Install artifacts live under `.gradle/python/` in the project directory. After the first bootstrap, parallel `PythonExec` tasks share resolved paths safely.

## Development

```bash
git clone https://github.com/tazzledazzle/gradle-python-plugin.git
cd gradle-python-plugin
./gradlew check
```

| Command | Purpose |
|---------|---------|
| `./gradlew test` | Unit tests |
| `./gradlew functionalTest` | Gradle TestKit integration tests |
| `./gradlew detekt ktlintCheck` | Static analysis |

## Publishing

This plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.tazzledazzle.python).

Maintainers: see **[docs/PUBLISHING.md](docs/PUBLISHING.md)** for API keys, `publishPlugins`, and CI tag releases.

## Documentation

| Doc | Contents |
|-----|----------|
| [docs/DSL.md](docs/DSL.md) | Public API reference |
| [docs/MIGRATION.md](docs/MIGRATION.md) | Upgrading from pre-v1 workarounds |
| [docs/PUBLISHING.md](docs/PUBLISHING.md) | Plugin Portal release process |
| [docs/DESIGN.md](docs/DESIGN.md) | Architecture and design decisions |
| [docs/roadmap/PROJECT-ROADMAP.md](docs/roadmap/PROJECT-ROADMAP.md) | Milestones and release checklist |
| [docs/roadmap/CONTRIBUTING-ROADMAP.md](docs/roadmap/CONTRIBUTING-ROADMAP.md) | Contributor guide |

## Roadmap

v1.1 features (`outputFile`, `script` execution) are implemented on `main`. See [docs/DSL.md](docs/DSL.md).

## License

MIT — see [LICENSE](LICENSE).
