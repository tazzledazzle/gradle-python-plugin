# Gradle Python Plugin Project Roadmap

## Vision

Build an open-source Gradle plugin that makes Python execution in JVM-first builds deterministic, fast, and CI-friendly across Conda and uv backends.

## v1.0 Scope

In scope:
- `PythonExec` task with robust process execution, output capture, and exit-code handling.
- First-class `ignoreExitValue` property on `PythonExec`.
- Shared `PythonEnvService` `BuildService` for serialized bootstrap and shared resolved state.
- `envManager` backend selection (`conda` default, `uv` opt-in).
- Plugin DSL and task wiring for `EnvSetupTask` and `PythonExec`.
- Functional and integration test coverage for parallel task scenarios.

Out of scope for v1.0:
- Additional environment managers beyond `conda` and `uv`.
- Remote cache-specific optimization tuning.
- Automatic migration tooling from external Python Gradle plugins.

## Product Principles

- Fail fast by default; allow explicit non-fatal behavior when requested (`ignoreExitValue`).
- Keep the public DSL minimal and stable.
- Prefer deterministic install/resolve behavior over implicit machine state.
- Optimize for CI repeatability and local developer ergonomics.

## Milestones

## Implementation Status (2026-05-26)

- M1 Core Execution Substrate: complete
- M2 Build Concurrency Correctness: complete
- M3 Backend Abstraction (`conda` + `uv`): complete (installer stubs in place; production download logic deferred)
- M4 Policy Controls and Quality Gates: complete (`ignoreExitValue`)
- M5 OSS Hardening and Release Readiness: in progress (CI added; release checklist below)

## v1.0 Release Checklist

- [x] `PythonExec` execution contract with stdout/stderr/exit capture
- [x] `ignoreExitValue` first-class property (default `false`)
- [x] `PythonEnvService` registered with `maxParallelUsages = 1`
- [x] `envManager` supports `conda` and `uv`
- [x] Unit and functional test suites pass locally
- [x] GitHub Actions CI runs `test` and `functionalTest`
- [ ] `condaRepoPassword` credentials API decision finalized

## M1: Core Execution Substrate

Outcomes:
- `PythonExec` executes scripts and commands from managed Python environments.
- Exit value, stdout, stderr are captured and exposed for downstream logic.
- Baseline tests for happy-path and failure-path execution.

Exit criteria:
- Non-zero exits fail build by default.
- Output file handling works for successful and failed runs.
- No regressions in current execution flow.

## M2: Build Concurrency Correctness

Outcomes:
- `PythonEnvService` introduced and wired as shared service.
- Environment bootstrap is serialized with `maxParallelUsages = 1`.
- Parallel `PythonExec` tasks can run after bootstrap without race failures.

Exit criteria:
- Parallel Gradle run confirms single bootstrap in logs.
- No directory race conditions under repeated CI runs.

## M3: Backend Abstraction (`conda` + `uv`)

Outcomes:
- `envManager` extension property available and validated.
- `UvInstaller` and `UvEnvResolver` integrated under shared service.
- Existing Conda path remains default and stable.

Exit criteria:
- Same `PythonExec` task definitions work under both backends.
- Backend-specific tests pass in CI matrix.

## M4: Policy Controls and Quality Gates

Outcomes:
- `ignoreExitValue` implemented as first-class `Property<Boolean>` defaulting to `false`.
- Warn-only workflows (lint/audit/reporting) supported without boilerplate wrappers.
- Documentation includes explicit usage guidance for non-fatal exits.

Exit criteria:
- Tests verify both fail-fast and ignore modes.
- Example workflows for `pytest`, `ruff`, `pip check` are documented.

## M5: OSS Hardening and Release Readiness

Outcomes:
- Contributor-facing docs and issue taxonomy in place.
- CI validates formatting, static checks, and functional tests.
- v1.0 release checklist and decision log finalized.

Exit criteria:
- Green CI on supported OS/arch matrix.
- No unresolved v1 blockers in decision log.

## Risks and Mitigations

- uv platform archive mapping drift.
  - Mitigation: centralize target mapping and add matrix tests per platform key.
- Race conditions in shared install directory.
  - Mitigation: service-level serialization and idempotent installers.
- Credential handling for `condaRepoPassword`.
  - Mitigation: finalize credentials API decision before v1 cut, avoid logging secrets.
- Divergence in behavior between backends.
  - Mitigation: backend-parity test suite with identical task fixtures.

## Success Metrics

- Bootstrap once behavior in parallel builds is reliable.
- Backend switch requires only `envManager` config changes.
- Warn-only quality tasks complete build while exposing actionable outputs.
- External contributors can complete "good first issues" with documented paths.
