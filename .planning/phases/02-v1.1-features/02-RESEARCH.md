# Phase 2: v1.1 Features & Hardening - Research

**Researched:** 2026-06-23
**Domain:** Gradle Plugin API (Kotlin), TestKit functional testing, PythonExec task extension
**Confidence:** HIGH

## Summary

Phase 2 extends the existing `PythonExec` task with two v1.1 execution modes (`outputFile`, `script`) and closes release gaps (TestKit coverage, migration guide, DSL examples). The implementation stays within the current architecture: command resolution in `buildCommand()`, process execution in `executeProcess()`, environment resolution via `PythonEnvService.resolveExecutable()`. No new external dependencies are required.

**Primary recommendation:** Extend `PythonExec` incrementally — add `@OutputFile`/`RegularFileProperty` for stdout persistence and `@InputFile`/`RegularFileProperty` for script mode; centralize mode validation at the start of `buildCommand()`; reuse existing unit-test fixtures (`sh`/`cmd` branching) and functional-test sentinel pattern; ship docs in parallel with code.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| stdout capture & exit policy | Gradle task (`PythonExec`) | — | Already owned by task `@TaskAction` |
| stdout file persistence (`outputFile`) | Gradle task (`PythonExec`) | Gradle incremental engine (`@OutputFile`) | Task writes file; Gradle tracks outputs for UP-TO-DATE |
| script command assembly | Gradle task (`PythonExec.buildCommand`) | `PythonEnvService` (python path) | Task owns command shape; service resolves managed `python` |
| managed python path resolution | `PythonEnvService` | conda/uv installers | Existing `resolveExecutable("python")` — no new service API |
| functional end-to-end verification | TestKit (`functionalTest` source set) | Unit tests (`ProjectBuilder`) | TestKit validates plugin-in-build; unit tests validate task logic in isolation |
| migration / DSL documentation | Docs (`docs/MIGRATION.md`, `docs/DSL.md`) | README link | User-facing contract; DSL is source of truth |
| optional network CI (TEST-02) | GitHub Actions (`ci.yml`) | gated functional test | CI tier owns env gating; test tier owns assertion |

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### outputFile
- Write **captured stdout** to `outputFile` after process completes (not shell redirect during run)
- Use `RegularFileProperty` with `@OutputFile` + `@Optional`
- Create parent directories before write

#### script execution
- When `script` is set: command = `[resolveExecutable("python"), scriptPath] + arguments`
- **Mutually exclusive** with `venvExec` — fail at `buildCommand()` with `GradleException`
- `executable` remains for non-script explicit tools only

#### Testing
- Unit tests per property; functional TestKit for `PythonExec` and script mode
- Reuse sentinel/bootstrap patterns from `ParallelExecutionFunctionalTest` (no network in default functional tests)
- TEST-02 optional: `CI_NETWORK_TESTS=true` gate on `main` push only

#### Documentation
- `docs/MIGRATION.md`: map `doLast` exit checks → `ignoreExitValue`, manual conda paths → `venvExec`, etc.
- Update `docs/DSL.md` — remove v1.1 deferrals when features ship

### Claude's Discretion
- Exact test fixture commands (`sh`/`cmd` branching) matching existing `PythonExecTest.kt`
- Whether TEST-02 ships in same phase or as follow-up if CI flaky

### Deferred Ideas (OUT OF SCOPE)
- Live Miniforge/uv download in every PR CI run (optional TEST-02 only on main)
- Additional environment managers
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EXEC-05 | `outputFile` persists captured stdout | `@OutputFile` + `RegularFileProperty`; write in `executeProcess()` after capture, before exit check; parent dir creation |
| EXEC-06 | `script` runs `.py` via managed `python` | `@InputFile` + `RegularFileProperty`; extend `buildCommand()` to prepend script path; use `envService.resolveExecutable("python")` |
| EXEC-07 | `script` and `venvExec` mutually exclusive | Validate at start of `buildCommand()` with clear `GradleException`; unit test with `assertFailsWith` |
| TEST-01 | TestKit functional test for `PythonExec` | `GradleRunner` + `withPluginClasspath()`; explicit-executable fixture (no network); mirror `ParallelExecutionFunctionalTest` |
| DOC-01 | Migration guide | `docs/MIGRATION.md` with workaround→replacement table; link from DSL/README |
| DOC-02 | `ignoreExitValue` DSL examples | Expand `docs/DSL.md` with warn-only lint / conditional logging patterns |
| TEST-02 | Optional network backend parity on main | Separate CI job; `CI_NETWORK_TESTS=true`; real `envSetup` without sentinel; JUnit env gate |
</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Gradle Plugin API | 8.14.3 (project wrapper) | `RegularFileProperty`, `@OutputFile`, `@InputFile`, `@Optional` | Already used; task property annotations are the Gradle-native incremental-build contract [CITED: docs.gradle.org/current/javadoc] |
| Gradle TestKit | via `gradleTestKit()` | Functional plugin tests | Official plugin testing mechanism; project already configured [CITED: docs.gradle.org/current/userguide/test_kit.html] |
| kotlin.test + JUnit Platform | existing | Unit/functional assertions | Matches `build.gradle.kts` and existing test classes |
| detekt + ktlint | existing | Static analysis gate | Already in `check` task |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `ProjectBuilder` | Gradle Test Fixtures | Isolated task unit tests | `buildCommand()`, `executeProcess()`, property validation |
| `GradleRunner` | TestKit | End-to-end plugin-in-build tests | TEST-01, script functional test |
| JUnit `@EnabledIfEnvironmentVariable` | JUnit 5 | Gate TEST-02 | Optional network tests only on main CI |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Post-execution file write | Shell redirect in command | Locked decision rejects redirect — breaks capture semantics and cross-platform tests |
| `@Input` String for script path | `@InputFile RegularFileProperty` | String loses content-based incremental invalidation [CITED: InputFile javadoc] |
| Real conda download in PR CI | Sentinel stub (current pattern) | Sentinel is fast/offline; real download reserved for TEST-02 |

**Installation:** No new packages. Phase uses existing Gradle APIs and test infrastructure.

**Version verification:**
```bash
./gradlew --version  # Gradle 8.14.3 (verified 2026-06-23)
```

## Package Legitimacy Audit

> No external packages are installed in this phase. All work uses Gradle Plugin API, TestKit, and existing project dependencies.

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
User build.gradle.kts
        │
        ▼
  PythonPlugin.apply()
        │ registers pythonEnvService (BuildService)
        ▼
  PythonExec task
        │
        ├─ buildCommand()
        │     ├─ validate: script ⊥ venvExec (EXEC-07)
        │     ├─ script mode → envService.resolveExecutable("python") + script path
        │     ├─ venvExec mode → envService.resolveExecutable(name)
        │     └─ executable mode → explicit path/PATH
        │
        ├─ ProcessBuilder(command) → capture stdout/stderr/exit
        │
        ├─ stdout.set(out)  [Internal Property]
        ├─ outputFile.orNull → mkdirs + writeText(out)  [EXEC-05]
        └─ exit check → GradleException unless ignoreExitValue

Tests
  Unit (ProjectBuilder) ──► buildCommand / executeProcess logic
  Functional (TestKit)  ──► plugin applied in temp project, task SUCCESS
  Optional CI (main)    ──► real envSetup download (TEST-02)
```

### Recommended Project Structure

```
src/main/kotlin/com/tazzledazzle/python/tasks/
  PythonExec.kt                    # MODIFY: outputFile, script, validation

src/test/kotlin/com/tazzledazzle/python/tasks/
  PythonExecOutputFileTest.kt      # CREATE
  PythonExecScriptTest.kt          # CREATE
  PythonExecTest.kt                # REFERENCE: sh/cmd fixtures

src/functionalTest/kotlin/com/tazzledazzle/python/
  PythonExecFunctionalTest.kt      # CREATE (TEST-01)
  PythonExecScriptFunctionalTest.kt # CREATE (script mode)
  ParallelExecutionFunctionalTest.kt # REFERENCE: sentinel pattern
  BackendParityFunctionalTest.kt   # CREATE (TEST-02, optional)

docs/
  MIGRATION.md                     # CREATE (DOC-01)
  DSL.md                           # MODIFY: ship v1.1 props, examples (DOC-02)

.github/workflows/ci.yml           # MODIFY: optional integration job (TEST-02)
```

### Pattern 1: Managed output property (`outputFile`)

**What:** Declare optional task output as `RegularFileProperty` annotated `@OutputFile`.
**When to use:** Any task-produced file Gradle should track for incremental builds.
**Example:**

```kotlin
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Optional

@get:OutputFile
@get:Optional
abstract val outputFile: RegularFileProperty

// In executeProcess(), after stdout.set(out):
outputFile.orNull?.asFile?.let { file ->
    file.parentFile.mkdirs()  // locked decision; Gradle also creates parent for @OutputFile [CITED: OutputFile javadoc]
    file.writeText(out)
}
```

**Ordering:** Write after capture, before exit-code failure — consumers get file content even when `ignoreExitValue` is false and task throws (stdout already captured).

### Pattern 2: Script input property (`script`)

**What:** Declare script path as `@InputFile` `RegularFileProperty` so content changes invalidate the task.
**When to use:** Any input file whose contents affect task behavior.
**Example:**

```kotlin
import org.gradle.api.tasks.InputFile

@get:InputFile
@get:Optional
abstract val script: RegularFileProperty

private fun resolveExecutableCommand(): String = when {
    script.isPresent -> envService.get().resolveExecutable("python").absolutePath
    venvExec.isPresent -> envService.get().resolveExecutable(venvExec.get()).absolutePath
    executable.isPresent -> { /* existing path logic */ }
    else -> throw GradleException("PythonExec requires 'script', 'venvExec', or 'executable'.")
}

internal fun buildCommand(): List<String> {
    if (script.isPresent && venvExec.isPresent) {
        throw GradleException(
            "PythonExec 'script' cannot be used together with 'venvExec'. " +
                "Use 'script' for managed python + .py file, or 'venvExec' for a named tool.",
        )
    }
    val exe = resolveExecutableCommand()
    return if (script.isPresent) {
        listOf(exe, script.get().asFile.absolutePath) + arguments.get()
    } else {
        listOf(exe) + arguments.get()
    }
}
```

**Discretion note:** CONTEXT locks `script` ⊥ `venvExec`. Because `executable` is "for non-script explicit tools only," also reject `script` + `executable` with the same validation block to avoid ambiguous command modes.

### Pattern 3: Unit test with platform branching

**What:** Use `sh`/`cmd` stubs for cross-platform process tests without Python installed.
**When to use:** All `executeProcess()` tests including `outputFile`.
**Example:** Copy `successCommand()` / `isWindows()` from `PythonExecTest.kt`:

```kotlin
// Source: src/test/kotlin/.../PythonExecTest.kt (project)
private fun successCommand(): Pair<String, List<String>> =
    if (isWindows()) {
        "cmd" to listOf("/c", "(set /p=hello<nul) & exit /b 0")
    } else {
        "sh" to listOf("-c", "printf 'hello'; exit 0")
    }
```

Assert `outputFile` content equals captured stdout — **do not** shell-redirect into the output file in the test (that would bypass plugin write logic).

### Pattern 4: envService mock for script/venvExec unit tests

**What:** Register a test `BuildService` and lay down fake binaries under `.venv/bin/` or conda env paths.
**When to use:** `buildCommand()` tests that call `resolveExecutable`.
**Example:** Follow `PythonExecVenvExecTest` — create fake `python` at `.venv/bin/python` (uv) or conda env bin path, register service with `envManagerType = "uv"`.

### Pattern 5: TestKit functional test (no network)

**What:** Temp project + plugin classpath + sentinel to skip real conda download.
**When to use:** TEST-01 and script functional tests in default CI.
**Example:**

```kotlin
// Source: ParallelExecutionFunctionalTest.kt (project) + TestKit docs
GradleRunner.create()
    .withProjectDir(projectDir)
    .withPluginClasspath()
    .withArguments("runEcho")
    .build()
```

**Sentinel task:** `prepareCondaSentinel` writes `.gradle/python/conda/{version}/.installed` so `EnvSetupTask`/`resolveExecutable` paths exist without network.

**Script functional test extension:** After sentinel, write a stub `python` script that echoes expected output; register `PythonExec` with `script.set(...)` and optional `outputFile`; assert task SUCCESS and file contents.

### Pattern 6: Optional network test gate (TEST-02)

**What:** JUnit 5 environment condition + separate CI job on `main` only.
**When to use:** Backend parity validation with real Miniforge/uv download.

```kotlin
@EnabledIfEnvironmentVariable(named = "CI_NETWORK_TESTS", matches = "true")
@Test
fun `conda envSetup creates install sentinel without stub`() { /* no prepareCondaSentinel */ }
```

```yaml
# ci.yml — separate job, main push only
integration:
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
  env:
    CI_NETWORK_TESTS: true
  steps:
    - run: ./gradlew functionalTest --tests "*BackendParityFunctionalTest" --no-daemon
```

### Anti-Patterns to Avoid

- **Shell redirect for `outputFile`:** Bypasses capture model; violates EXEC-05 locked decision.
- **Resolving script path in `@TaskAction` only:** Command must be testable via `buildCommand()` without running process.
- **Network in default functional tests:** Flaky/slow PR CI; use sentinel pattern.
- **Replacing test source set in `gradlePlugin.testSourceSets(...)`:** Overwrites default `test` set [CITED: GradlePluginDevelopmentExtension javadoc]; use `.add(functionalTestSourceSet)` if wiring explicitly.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Task output tracking | Custom stamp files / manual UP-TO-DATE | `@OutputFile RegularFileProperty` | Gradle incremental engine handles path + content invalidation [CITED: OutputFile javadoc] |
| Script input tracking | Raw String path `@Input` | `@InputFile RegularFileProperty` | Content-change detection built-in [CITED: InputFile javadoc] |
| Plugin-in-build testing | Execing gradle CLI manually | `GradleRunner` + `withPluginClasspath()` | Isolated temp projects, task outcome assertions [CITED: test_kit.html] |
| Managed python resolution | Duplicate conda/uv path logic in task | `PythonEnvService.resolveExecutable("python")` | Single resolver for conda + uv backends |
| Cross-platform echo fixtures | Assume `/bin/echo` or python installed | `sh`/`cmd` branching from `PythonExecTest` | CI runs ubuntu-latest; dev machines may differ |

**Key insight:** `PythonExec` is a thin orchestration layer — delegate file/property semantics and incremental behavior to Gradle annotations; delegate env paths to `PythonEnvService`.

## Common Pitfalls

### Pitfall 1: `@OutputFile` parent directory
**What goes wrong:** Write fails on nested path like `build/reports/out.txt`.
**Why it happens:** File doesn't exist yet on first run.
**How to avoid:** Call `parentFile.mkdirs()` before `writeText` (locked decision); Gradle `@OutputFile` also documents parent creation [CITED: OutputFile javadoc] — do both for clarity.
**Warning signs:** `FileNotFoundException` in unit tests using nested output paths.

### Pitfall 2: Validation order in `buildCommand()`
**What goes wrong:** Cryptic "python not found" before mutual-exclusivity error.
**Why it happens:** Resolution runs before validation when both `script` and `venvExec` set.
**How to avoid:** Check `script.isPresent && venvExec.isPresent` (and optionally `script && executable`) **first**.
**Warning signs:** EXEC-07 test expects specific message but gets resolver error.

### Pitfall 3: Functional test classpath for custom source set
**What goes wrong:** `withPluginClasspath()` empty — plugin not found in test build.
**Why it happens:** `functionalTest` source set not registered with `gradlePlugin.testSourceSets`.
**How to avoid:** Current project passes ( `:pluginUnderTestMetadata` runs); if new tests fail with unresolved plugin, add `gradlePlugin { testSourceSets.add(functionalTestSourceSet) }` per [CITED: test_kit.html].
**Warning signs:** TestKit build fails with "Plugin with id 'com.tazzledazzle.python' not found".

### Pitfall 4: Script functional test without fake python
**What goes wrong:** Task fails because managed `python` path doesn't exist.
**Why it happens:** Script mode always uses `envService`, not system python.
**How to avoid:** Mirror `PythonExecVenvExecTest` — create fake `python` under `.venv/bin/python` (uv) or conda env bin; depend on `prepareCondaSentinel`.
**Warning signs:** `FileNotFoundException` or non-zero exit in TestKit build.

### Pitfall 5: Writing `outputFile` only on success
**What goes wrong:** Downstream tasks can't read partial output from failed lint/test runs.
**Why it happens:** Write guarded by `if (code == 0)`.
**How to avoid:** Write after capture regardless of exit code; let `ignoreExitValue` control failure.
**Warning signs:** EXEC-05 test with failing process + `ignoreExitValue=true` finds empty file.

### Pitfall 6: TEST-02 flaking on main
**What goes wrong:** Network download timeouts fail main CI.
**Why it happens:** GitHub rate limits, conda CDN latency.
**How to avoid:** Separate job (not blocking PR `check`); allow follow-up deferral per Claude's discretion; generous timeout; consider `continue-on-error` only if user accepts.
**Warning signs:** Intermittent main-only failures on `BackendParityFunctionalTest`.

## Code Examples

### Persist stdout to outputFile

```kotlin
// Source: CONTEXT.md + Gradle OutputFile/RegularFileProperty javadoc
@get:OutputFile
@get:Optional
abstract val outputFile: RegularFileProperty

// In executeProcess(), after stdout.set(out):
outputFile.orNull?.asFile?.let { file ->
    file.parentFile.mkdirs()
    file.writeText(out)
}
```

### Script mode command building

```kotlin
// Source: CONTEXT.md + PythonEnvService.resolveExecutable (project)
if (script.isPresent && venvExec.isPresent) {
    throw GradleException("'script' and 'venvExec' are mutually exclusive.")
}
val base = resolveExecutableCommand()
return if (script.isPresent) {
    listOf(base, script.get().asFile.absolutePath) + arguments.get()
} else {
    listOf(base) + arguments.get()
}
```

### TestKit functional test (explicit executable, no network)

```kotlin
// Source: v1.1 implementation plan + PythonExecTest fixtures
tasks.register<com.tazzledazzle.python.tasks.PythonExec>("runEcho") {
    executable.set(if (System.getProperty("os.name").contains("Windows", true)) "cmd" else "sh")
    arguments.set(
        if (System.getProperty("os.name").contains("Windows", true)) {
            listOf("/c", "echo hello")
        } else {
            listOf("-c", "printf hello")
        }
    )
}
```

### Sentinel stub (offline env bootstrap)

```kotlin
// Source: ParallelExecutionFunctionalTest.kt (project)
val condaSentinel = layout.projectDirectory.file(".gradle/python/conda/24.11.3-0/.installed")
tasks.register("prepareCondaSentinel") {
    outputs.file(condaSentinel)
    doLast {
        condaSentinel.asFile.parentFile.mkdirs()
        condaSentinel.asFile.writeText("installed")
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual `doLast { if (exitValue != 0) ... }` | `ignoreExitValue` property | v1.0 | DOC-01 migration maps old→new |
| `executable` + script path in `arguments` | `script` property + managed python | v1.1 (this phase) | Cleaner DSL; env-aware python resolution |
| stdout only on task properties | `outputFile` persistence | v1.1 (this phase) | Downstream tasks can `dependsOn` output file |
| Deferred v1.1 features in DSL | Shipped properties in `docs/DSL.md` | v1.1 release | Remove "Deferred to v1.1" section |

**Deprecated/outdated:**
- `docs/DSL.md` "Deferred to v1.1" table — remove when EXEC-05/06 ship
- `useVenv` toggle — already removed; mention in MIGRATION.md only if users ask

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Reject `script` + `executable` together (not only `script` + `venvExec`) | Pattern 2 | Ambiguous command if both set; planner should confirm with locked CONTEXT wording |
| A2 | `outputFile` written even when task throws on non-zero exit | Pattern 1 | Pipeline use cases lose output on failure |
| A3 | `gradlePlugin.testSourceSets.add(functionalTest)` not required (current metadata task suffices) | Pitfall 3 | New TestKit tests may fail until wired |
| A4 | `@EnabledIfEnvironmentVariable` available on kotlin.test/JUnit 5 classpath | Pattern 6 | Need explicit JUnit Jupiter dependency for TEST-02 |

**If A1 is wrong:** Only EXEC-07 (`script` ⊥ `venvExec`) is strictly required; `script` + `executable` could be left undefined.

## Open Questions

1. **Should `script` + `executable` also be rejected explicitly?**
   - What we know: CONTEXT says "`executable` remains for non-script explicit tools only"
   - What's unclear: No explicit EXEC requirement ID for this pair
   - Recommendation: Reject in same validation block as EXEC-07 — clearer errors, matches intent

2. **TEST-02 in same phase or follow-up?**
   - What we know: Marked optional; Claude's discretion
   - What's unclear: Network reliability on GitHub Actions
   - Recommendation: Implement test class + CI job skeleton; if flaky after 2–3 main runs, disable job and track follow-up

3. **Version bump for Plugin Portal publish?**
   - What we know: `gradle.properties` holds version; Phase 2 is feature release
   - What's unclear: Semver target (0.2.0 vs 0.1.1)
   - Recommendation: Planner confirms with user — out of strict phase scope but affects release bookkeeping task

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Gradle | Build/test | ✓ | 8.14.3 | — |
| JDK | Compile/test | ✓ | 25.0.2 (local); CI uses 21 | CI pins Java 21 in `ci.yml` |
| kotlin.test / JUnit Platform | Unit + functional tests | ✓ | via `kotlin("test")` | — |
| Gradle TestKit | TEST-01, functional tests | ✓ | bundled with `gradleTestKit()` | — |
| Network (Miniforge/uv) | TEST-02 only | ✓ on CI runners | — | Skip TEST-02; sentinel for default tests |
| System `sh` / `cmd` | Unit/functional fixtures | ✓ | OS-provided | Required on all CI OS targets |

**Missing dependencies with no fallback:** none for core phase scope (EXEC-05–07, TEST-01, DOC-01–02)

**Missing dependencies with fallback:**
- Network for TEST-02 — defer job or gate with env var (already planned)

## Project Constraints (from .cursor/rules/)

No `.cursor/rules/` directory found in project root. No additional Cursor rule directives apply.

## Suggested Implementation Sequencing

| Order | Work item | Depends on | Rationale |
|-------|-----------|------------|-----------|
| 1 | TEST-01 `PythonExecFunctionalTest` | — | Validates baseline before changes; can pass immediately |
| 2 | DOC-01 `MIGRATION.md` + DOC-02 DSL examples | — | Docs parallelizable with code |
| 3 | EXEC-05 `outputFile` + unit test | — | Smallest API extension; no service changes |
| 4 | EXEC-06/07 `script` + validation + unit tests | EXEC-05 optional | Touches `buildCommand()` core |
| 5 | Script functional test | EXEC-06 | End-to-end script mode |
| 6 | TEST-02 backend parity (optional) | EXEC-06 | Network-dependent; last |

## Sources

### Primary (HIGH confidence)
- [Gradle `@OutputFile` javadoc](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/OutputFile.html) — output tracking, parent dir creation
- [Gradle `RegularFileProperty` javadoc](https://docs.gradle.org/current/javadoc/org/gradle/api/file/RegularFileProperty.html) — managed property pattern
- [Gradle `@InputFile` javadoc](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/InputFile.html) — script input invalidation
- [Gradle TestKit user guide](https://docs.gradle.org/current/userguide/test_kit.html) — GradleRunner, functionalTest source set, `withPluginClasspath()`
- Project: `PythonExec.kt`, `PythonExecTest.kt`, `PythonExecVenvExecTest.kt`, `ParallelExecutionFunctionalTest.kt`, `build.gradle.kts`, `02-CONTEXT.md`

### Secondary (MEDIUM confidence)
- [Gradle binary plugin testing guide](https://docs.gradle.org/current/userguide/testing_binary_plugin_advanced.html) — `gradlePlugin.testSourceSets.add(functionalTest)`
- [GradlePluginDevelopmentExtension source](https://github.com/gradle/gradle/blob/master/platforms/extensibility/plugin-development/src/main/java/org/gradle/plugin/devel/GradlePluginDevelopmentExtension.java) — additive vs replace for testSourceSets
- `docs/plans/2026-05-26-gradle-python-plugin-v1.1-implementation.md` — task breakdown seed (package paths use old `com.example` — use `com.tazzledazzle`)

### Tertiary (LOW confidence)
- JUnit `@EnabledIfEnvironmentVariable` on kotlin.test classpath without explicit junit-jupiter import — verify at implementation time (A4)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Gradle APIs verified via official javadoc; project patterns confirmed in codebase
- Architecture: HIGH — extensions fit existing `PythonExec` / `PythonEnvService` split; CONTEXT locks key decisions
- Pitfalls: HIGH — derived from existing test patterns and Gradle annotation semantics

**Research date:** 2026-06-23
**Valid until:** 2026-07-23 (Gradle 8.x API stable; verify if upgrading wrapper)
