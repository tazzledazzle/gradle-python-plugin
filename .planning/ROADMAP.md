# Roadmap: Gradle Python Plugin

## Overview

Phase 1 delivered the v1.0 plugin: `PythonExec`, `PythonEnvService`, conda/uv backends, static analysis CI, DSL docs, and Plugin Portal publishing setup. Phase 2 shipped v1.1 execution extensions (`outputFile`, `script`), release hardening tests/docs, and optional integration CI.

## Phases

- [x] **Phase 1: v1.0 Foundation** - Core plugin, env service, installers, CI, Portal publish config
- [x] **Phase 2: v1.1 Features & Hardening** - `outputFile`, `script` mode, migration guide, functional tests

## Phase Details

### Phase 1: v1.0 Foundation

**Goal**: Shippable Gradle Python plugin with managed environments and OSS quality gates
**Depends on**: Nothing
**Requirements**: EXEC-01, EXEC-02, EXEC-03, EXEC-04, ENV-01, ENV-02, ENV-03, OSS-01, OSS-02, OSS-03
**Success Criteria** (what must be TRUE):

  1. `./gradlew check` passes on main
  2. Plugin applies via `id("com.tazzledazzle.python")` and runs `PythonExec` with `venvExec`
  3. Parallel `envSetup` tasks do not race on install directory

**Plans**: Shipped outside GSD tracking (complete)

### Phase 2: v1.1 Features & Hardening

**Goal**: Extend `PythonExec` with `outputFile` and `script` execution; close release documentation and TestKit gaps
**Depends on**: Phase 1
**Requirements**: EXEC-05, EXEC-06, EXEC-07, TEST-01, DOC-01, DOC-02, TEST-02
**Success Criteria** (what must be TRUE):

  1. `PythonExec` can write stdout to `outputFile` after execution
  2. `PythonExec` can run a `.py` file via managed `python` when `script` is set
  3. `docs/MIGRATION.md` documents v1 workaround replacements
  4. `./gradlew check` passes including new unit and functional tests

**Plans**: 4 plans (complete)

**Wave 1**

- [x] 02-01-PLAN.md — Release hardening: TestKit functional test, MIGRATION.md, DSL ignoreExitValue examples

**Wave 2**

- [x] 02-02-PLAN.md — `outputFile` property and stdout persistence (EXEC-05)

**Wave 3**

- [x] 02-03-PLAN.md — `script` execution mode with mutual-exclusivity validation (EXEC-06, EXEC-07)

**Wave 4**

- [x] 02-04-PLAN.md — Optional backend parity CI with CI_NETWORK_TESTS gate (TEST-02)

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. v1.0 Foundation | — | Complete | 2026-05-26 |
| 2. v1.1 Features & Hardening | 4/4 | Complete | 2026-05-26 |
