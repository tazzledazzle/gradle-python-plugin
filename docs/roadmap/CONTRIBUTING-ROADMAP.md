# Contributing Roadmap

This project is in planning-first mode. Contributors are encouraged to pick narrowly scoped tasks and submit small, reviewable pull requests.

## Contribution Priorities

Priority 1:
- `ignoreExitValue` task-level behavior and tests.

Priority 2:
- `PythonEnvService` shared service wiring and parallel execution tests.

Priority 3:
- `uv` backend installer/resolver integration and backend parity tests.

Priority 4:
- Documentation hardening, examples, and release checklist validation.

## Good First Issues

- Add tests validating non-fatal non-zero exits (`ignoreExitValue = true`).
- Add docs examples for warn-only lint task configuration.
- Add validation helper for allowed `envManager` values.
- Add/update decision log entries as behavior is finalized.

## PR Expectations

- Keep PRs focused on one backlog slice.
- Include tests with behavior changes.
- Update docs when public behavior or DSL changes.
- Avoid unrelated refactors in feature PRs.

## Definition of Done (Per PR)

- Relevant tests pass locally and in CI.
- Backward compatibility preserved unless explicitly documented.
- Public DSL changes include examples.
- Changelog/release notes impact is described in PR summary.

## Suggested PR Slicing

- PR 1: `ignoreExitValue` property + tests + docs snippet.
- PR 2: `PythonEnvService` registration/wiring + parallel fixture tests.
- PR 3: `uv` installer + resolver + backend-selection tests.
- PR 4: CI matrix hardening and contributor docs polish.

## Review Checklist

- Correctness:
  - Does behavior match roadmap decisions?
  - Are failure modes explicit and tested?
- Concurrency:
  - Are shared resources accessed safely in parallel runs?
- Security:
  - Are secrets/credentials excluded from logs and outputs?
- UX:
  - Is configuration discoverable and minimally surprising?

## Communication Guidelines

- Open an issue before large changes.
- Reference roadmap epic/task in PR description.
- Call out trade-offs and rejected alternatives in PR notes.
- Prefer concrete reproduction steps for bug reports.
