# Gradle Python Plugin

## What This Is

An open-source Gradle plugin (`com.tazzledazzle.python`) that runs Python tooling from JVM-first builds with managed Conda or uv environments, shared bootstrap via `PythonEnvService`, and configurable exit-code policy.

## Core Value

Deterministic, CI-friendly Python execution in Gradle without shell wrappers — one bootstrap per build, stable DSL, fail-fast by default with opt-in warn-only tasks.

## Requirements

See `.planning/REQUIREMENTS.md`

## Key Decisions

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-05-26 | Miniforge default; `envManager = uv` opt-in | Scientific packages vs speed |
| 2026-05-26 | `PythonEnvService` with `maxParallelUsages = 1` | Prevent bootstrap races |
| 2026-05-26 | `ignoreExitValue` first-class | Lint/audit warn-only workflows |
| 2026-05-26 | Gradle Plugin Portal publishing | Portfolio project consumption |
| 2026-05-26 | `outputFile` / `script` deferred to v1.1 | v1.0 shipped core execution first |

## Out of Scope

- Environment managers beyond conda/uv
- Remote cache tuning
- Auto-migration from third-party Python Gradle plugins
