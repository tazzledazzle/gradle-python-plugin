# Phase 2: v1.1 Features & Hardening - Context

**Gathered:** 2026-05-26
**Status:** Ready for planning
**Source:** Existing roadmap + `docs/plans/2026-05-26-gradle-python-plugin-v1.1-implementation.md`

<domain>
## Phase Boundary

Deliver v1.1 execution features deferred from v1.0:
- `outputFile` on `PythonExec` (persist captured stdout)
- `script` property (run `.py` via managed `python`)
- Release hardening: TestKit functional test, `docs/MIGRATION.md`, DSL `ignoreExitValue` examples
- Optional: network-backed backend parity CI job on `main` only

Out of scope: new env managers, pip DSL, IDE plugins.
</domain>

<decisions>
## Implementation Decisions

### outputFile
- Write **captured stdout** to `outputFile` after process completes (not shell redirect during run)
- Use `RegularFileProperty` with `@OutputFile` + `@Optional`
- Create parent directories before write

### script execution
- When `script` is set: command = `[resolveExecutable("python"), scriptPath] + arguments`
- **Mutually exclusive** with `venvExec` — fail at `buildCommand()` with `GradleException`
- `executable` remains for non-script explicit tools only

### Testing
- Unit tests per property; functional TestKit for `PythonExec` and script mode
- Reuse sentinel/bootstrap patterns from `ParallelExecutionFunctionalTest` (no network in default functional tests)
- TEST-02 optional: `CI_NETWORK_TESTS=true` gate on `main` push only

### Documentation
- `docs/MIGRATION.md`: map `doLast` exit checks → `ignoreExitValue`, manual conda paths → `venvExec`, etc.
- Update `docs/DSL.md` — remove v1.1 deferrals when features ship

### Claude's Discretion
- Exact test fixture commands (`sh`/`cmd` branching) matching existing `PythonExecTest.kt`
- Whether TEST-02 ships in same phase or as follow-up if CI flaky
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Public API
- `docs/DSL.md` — Current implemented properties (v1.0 baseline)
- `src/main/kotlin/com/tazzledazzle/python/tasks/PythonExec.kt` — Command resolution to extend

### Patterns
- `src/test/kotlin/com/tazzledazzle/python/tasks/PythonExecVenvExecTest.kt` — envService mock pattern
- `src/functionalTest/kotlin/com/tazzledazzle/python/ParallelExecutionFunctionalTest.kt` — TestKit + sentinel pattern

### Publishing
- `docs/PUBLISHING.md` — Portal consumption for portfolio projects
- `build.gradle.kts` — Plugin id `com.tazzledazzle.python`, version in `gradle.properties`

### Prior plan (reference only)
- `docs/plans/2026-05-26-gradle-python-plugin-v1.1-implementation.md` — Task breakdown seed
</canonical_refs>

<specifics>
## Specific Ideas

- Portfolio projects consume via Plugin Portal: `id("com.tazzledazzle.python") version "0.1.0"`
- README already OSS-style; DSL is source of truth for properties
</specifics>

<deferred>
## Deferred Ideas

- Live Miniforge/uv download in every PR CI run (optional TEST-02 only on main)
- Additional environment managers
</deferred>

---

*Phase: 02-v1.1-features*
*Context gathered: 2026-05-26*
