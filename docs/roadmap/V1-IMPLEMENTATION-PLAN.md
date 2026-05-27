# V1 Implementation Backlog (OSS-Friendly)

This backlog translates v1.0 decisions into executable, reviewable work for open-source contributors.

## Epic 1: Core `PythonExec` Behavior

Objectives:
- Stabilize process execution contract.
- Ensure stdout/stderr/exit code are consistently captured.

Tasks:
- Define and document execution contract for environment executables (`venvExec`) and explicit `executable`.
- Add/verify task properties for arguments and resolved executable behavior.
- **Deferred v1.1:** `outputFile`, script-file execution mode (see `docs/DSL.md`).
- Add unit tests for:
  - successful exit path
  - non-zero exit path
  - output propagation
- Add Gradle functional test fixture proving task execution in a sample project.

Definition of done:
- Tests cover happy path and failure path.
- Documentation references concrete task usage snippets.

## Epic 2: First-Class `ignoreExitValue`

Objectives:
- Make non-fatal non-zero exits configurable at task level.

Tasks:
- Add `@Input` property:
  - `abstract val ignoreExitValue: Property<Boolean>`
  - default convention `false`.
- Gate `GradleException` throw behind `!ignoreExitValue.get()`.
- Preserve `exitValue` for downstream consumers regardless of throw behavior.
- Add tests for:
  - default `false` causes failure on exit != 0
  - `true` allows task completion with recorded non-zero exit
- Add docs with policy examples:
  - warn-only lint
  - audit/report task

Definition of done:
- No `doLast` workaround required for warn-only workflows.
- Behavior is backwards-compatible by default.

## Epic 3: Shared `PythonEnvService` BuildService

Objectives:
- Eliminate bootstrap races and repeated resolution overhead in parallel builds.

Tasks:
- Implement `PythonEnvService : BuildService<Params>`.
- Add service parameters for Python/Conda/env-manager configuration.
- Compute and cache platform and resolved environment root lazily.
- Register service via `project.gradle.sharedServices.registerIfAbsent(...)`.
- Apply `spec.maxParallelUsages.set(1)`.
- Wire tasks with `task.envService.set(envService)` and `task.usesService(envService)`.
- Add concurrency-focused functional tests:
  - parallel tasks sharing service
  - bootstrap performed once

Definition of done:
- No install-directory race under parallel execution.
- Single bootstrap semantics verified by tests/log assertions.

## Epic 4: `envManager` Backend Abstraction (`conda` + `uv`)

Objectives:
- Keep task API stable while enabling backend selection.

Tasks:
- Add `envManager` extension property:
  - convention `"conda"`
  - accepted values `"conda"` or `"uv"` with validation.
- Add `uvVersion` and `uvRepoUrl` extension properties.
- Implement `UvInstaller`:
  - idempotent install
  - platform-specific archive mapping
  - executable bit handling
- Implement `UvEnvResolver`:
  - map executable lookup in `.venv`.
- Route resolver calls through `PythonEnvService` by backend.
- Add backend-parity test suite:
  - identical task definitions under Conda and uv.

Definition of done:
- Backend switch is configuration-only.
- Both backends pass functional fixtures in CI.

## Epic 5: Test and CI Quality Gates

Objectives:
- Ensure maintainable confidence for OSS changes.

Tasks:
- Add test matrix:
  - unit tests for core behavior
  - Gradle TestKit functional tests for end-to-end behavior
  - parallel execution regression tests
- Add static quality checks:
  - `detekt` (config: `config/detekt/detekt.yml`)
  - `ktlint` via `org.jlleitschuh.gradle.ktlint`
- Add CI workflow(s) for PR and main branches:
  - fail mode on PR
  - optional warn-only examples on main/nightly

Definition of done:
- CI protects all core decisions in the decision log.
- New contributor PRs receive deterministic pass/fail outcomes.

## Epic 6: Documentation and Release Readiness

Objectives:
- Make adoption and contribution straightforward.

Tasks:
- Publish DSL reference for extension and task properties (`docs/DSL.md` — implemented properties only).
- Close `condaRepoPassword` credentials decision (`docs/decisions/conda-repo-password.md`).
- Add migration notes for users currently using workaround patterns.
- Maintain decision log and open question list.
- Add v1.0 release checklist:
  - docs complete
  - tests green
  - unresolved blockers cleared

Definition of done:
- Contributor can implement a backlog task from docs alone.
- Release checklist is complete and auditable.

## Sequencing

Recommended order:
1. Epic 1
2. Epic 2
3. Epic 3
4. Epic 4
5. Epic 5
6. Epic 6

Parallelizable after Epic 1:
- Epic 2 and Epic 3 can run in separate PRs if task boundaries are kept narrow.
- Epic 6 can begin once Epics 2-4 APIs stabilize.

## Acceptance Criteria for v1.0

- `ignoreExitValue` is first-class and documented.
- `PythonEnvService` prevents parallel bootstrap races; `PythonExec` uses `venvExec` → `resolveExecutable`.
- `envManager = "uv"` works with parity-focused behavior; real `CondaInstaller` / `UvInstaller` downloads (idempotent).
- `detekt` and `ktlintCheck` run in CI.
- `condaRepoPassword` resolved via Gradle credentials API (ADR accepted).
- **Out of v1.0 scope:** `outputFile`, script-file execution (v1.1 backlog).

## v1.1 Backlog (explicit deferrals)

| Item | Notes |
|------|--------|
| `outputFile` on `PythonExec` | Write stdout/stderr or tool output to a configured file |
| Script execution mode | Run `.py` files with interpreter resolution |
| Backend-parity functional tests with live downloads | Optional CI matrix job (network-dependent) |
