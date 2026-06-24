# Requirements: Gradle Python Plugin

**Defined:** 2026-05-26
**Core Value:** Deterministic Python execution in Gradle builds with managed environments

## v1.0 Requirements (shipped — Phase 1)

### Execution

- [x] **EXEC-01**: `PythonExec` captures stdout, stderr, and exit value
- [x] **EXEC-02**: `PythonExec` supports `venvExec` via `PythonEnvService.resolveExecutable`
- [x] **EXEC-03**: `PythonExec` supports explicit `executable` for PATH/local tools
- [x] **EXEC-04**: Non-zero exit fails build unless `ignoreExitValue` is true

### Environment

- [x] **ENV-01**: `PythonEnvService` serializes bootstrap (`maxParallelUsages = 1`)
- [x] **ENV-02**: `envManager` supports `conda` (default) and `uv`
- [x] **ENV-03**: Idempotent `CondaInstaller` and `UvInstaller`

### OSS

- [x] **OSS-01**: `detekt` and `ktlintCheck` in CI
- [x] **OSS-02**: DSL reference (`docs/DSL.md`)
- [x] **OSS-03**: Gradle Plugin Portal publish configuration

## v1.1 Requirements (Phase 2)

### Execution extensions

- [ ] **EXEC-05**: `outputFile` on `PythonExec` persists captured stdout to a configured file
- [ ] **EXEC-06**: `script` property runs `.py` files via managed `python` interpreter
- [ ] **EXEC-07**: `script` and `venvExec` are mutually exclusive with clear validation error

### Hardening

- [ ] **TEST-01**: Gradle TestKit functional test for `PythonExec` end-to-end
- [ ] **DOC-01**: Migration guide for pre-v1 workaround patterns (`docs/MIGRATION.md`)
- [ ] **DOC-02**: `ignoreExitValue` policy examples in DSL reference

### CI (optional)

- [ ] **TEST-02**: Optional network-backed backend parity test on `main` CI only

## Out of Scope

| Feature | Reason |
|---------|--------|
| pip/conda package DSL | Keep plugin focused on execution |
| IDE integration | Separate tooling |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| EXEC-01–04, ENV-01–03, OSS-01–03 | Phase 1 | Complete |
| EXEC-05–07, TEST-01, DOC-01–02 | Phase 2 | Pending |
| TEST-02 | Phase 2 | Pending (optional) |
