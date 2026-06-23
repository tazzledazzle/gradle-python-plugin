# Gradle Python Plugin v1.1 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Ship v1.1 with `outputFile` and script execution on `PythonExec`, close remaining v1 release documentation gaps, and add optional network-backed backend parity coverage in CI.

**Architecture:** Extend `PythonExec` command resolution with a third mode (`script` → managed `python` + script path) while keeping `venvExec` and `executable` unchanged. Add post-execution persistence for `outputFile` (stdout capture). Keep environment bootstrap in `PythonEnvService`; do not duplicate installer logic in the task.

**Tech Stack:** Kotlin 2.x, Gradle Plugin API (`Property`, `RegularFileProperty`, `BuildService`), Gradle TestKit, JUnit 5 (kotlin.test), detekt, ktlint.

**Prerequisites:** v1.0 complete on `main` (`./gradlew check` green). Read `docs/DSL.md`, `docs/decisions/conda-repo-password.md`, and `src/main/kotlin/com/example/python/tasks/PythonExec.kt` before starting.

---

## Part A — v1 Release Hardening (do first)

### Task 1: `PythonExec` functional test (TestKit)

**Files:**
- Create: `src/functionalTest/kotlin/com/example/python/PythonExecFunctionalTest.kt`
- Reference: `src/functionalTest/kotlin/com/example/python/ParallelExecutionFunctionalTest.kt`

**Step 1: Write the failing test**

```kotlin
@Test
fun `pythonExec runs explicit executable and captures output`() {
    val projectDir = Files.createTempDirectory("python-exec-functional").toFile()
    writeSettings(projectDir)
    File(projectDir, "build.gradle.kts").writeText(
        """
        plugins { id("com.tazzledazzle.python") }
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
        """.trimIndent(),
    )
    val result = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments("runEcho")
        .build()
    assertEquals(TaskOutcome.SUCCESS, result.task(":runEcho")?.outcome)
}
```

**Step 2: Run test to verify it fails (or passes if already works)**

Run: `./gradlew functionalTest --tests "*PythonExecFunctionalTest" --no-daemon`
Expected: PASS once fixture is correct (new file should compile and run).

**Step 3: Adjust fixture if platform-specific flakes**

Use the same `sh` / `cmd` branching pattern as `PythonExecTest.kt`.

**Step 4: Run full functional suite**

Run: `./gradlew functionalTest --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/functionalTest/kotlin/com/example/python/PythonExecFunctionalTest.kt
git commit -m "test: add PythonExec functional test via TestKit"
```

---

### Task 2: Migration notes for pre-v1 workarounds

**Files:**
- Create: `docs/MIGRATION.md`
- Modify: `docs/DSL.md` (link to migration doc in intro)
- Modify: `README.md` (add pointer at top: “User docs: DSL.md, Migration: MIGRATION.md”)

**Step 1: Write `docs/MIGRATION.md`**

Document these replacements:

| Old pattern | v1+ replacement |
|-------------|-----------------|
| `doLast { if (exitValue.get() != 0) ... }` | `ignoreExitValue.set(true)` |
| Manual conda path in every task | `venvExec.set("pytest")` + `envSetup` / service |
| Plain-text repo password in build script | `condaRepoPassword` property or `condaRepoPassword` in `gradle.properties` |
| Per-task env bootstrap | `EnvSetupTask` + shared `PythonEnvService` |

**Step 2: Link from DSL.md**

Add one line under the title: `See also: [Migration guide](../MIGRATION.md).`

**Step 3: Commit**

```bash
git add docs/MIGRATION.md docs/DSL.md README.md
git commit -m "docs: add migration guide for v1 workarounds"
```

---

### Task 3: `ignoreExitValue` usage examples in DSL

**Files:**
- Modify: `docs/DSL.md` (expand `PythonExec` section)

**Step 1: Add examples**

```markdown
### Warn-only lint (non-fatal exit)

```kotlin
tasks.register<PythonExec>("ruffCheck") {
    venvExec.set("ruff")
    arguments.set(listOf("check", "src"))
    ignoreExitValue.set(true)
}
doLast {
    if (exitValue.get() != 0) {
        logger.warn("Ruff reported issues (exit ${exitValue.get()})")
    }
}
```
```

**Step 2: Commit**

```bash
git add docs/DSL.md
git commit -m "docs: add ignoreExitValue policy examples to DSL reference"
```

---

## Part B — `outputFile` on `PythonExec`

### Task 4: `outputFile` property and stdout persistence

**Files:**
- Modify: `src/main/kotlin/com/example/python/tasks/PythonExec.kt`
- Create: `src/test/kotlin/com/example/python/tasks/PythonExecOutputFileTest.kt`
- Modify: `docs/DSL.md`

**Step 1: Write the failing test**

```kotlin
@Test
fun `writes stdout to outputFile when configured`() {
    val project = ProjectBuilder.builder().build()
    val output = File(project.buildDir, "out/result.txt").apply { parentFile.mkdirs() }
    val task = project.tasks.create("pythonExecOut", PythonExec::class.java)
    task.executable.set(if (isWindows()) "cmd" else "sh")
    task.arguments.set(
        if (isWindows()) listOf("/c", "echo hello>\"${output.absolutePath}\"")
        else listOf("-c", "printf hello")
    )
  // For non-Windows: set outputFile and assert file content after executeProcess
    task.outputFile.set(output)
    task.executeProcess()
    assertEquals("hello", output.readText().trim())
}
```

Prefer a test that does **not** shell-redirect: set `outputFile`, run `printf hello`, assert plugin wrote stdout to the file.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*PythonExecOutputFileTest" --no-daemon`
Expected: FAIL (unresolved reference `outputFile`)

**Step 3: Implement minimal property and write logic**

```kotlin
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile

@get:OutputFile
@get:Optional
abstract val outputFile: RegularFileProperty

// In executeProcess(), after stdout.set(out):
outputFile.orNull?.asFile?.let { file ->
    file.parentFile.mkdirs()
    file.writeText(out)
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*PythonExecOutputFileTest" --no-daemon`
Expected: PASS

**Step 5: Update `docs/DSL.md`**

Add `outputFile` row to `PythonExec` table; remove from “Deferred to v1.1”.

**Step 6: Run static analysis**

Run: `./gradlew detekt ktlintCheck --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add src/main/kotlin/com/example/python/tasks/PythonExec.kt \
  src/test/kotlin/com/example/python/tasks/PythonExecOutputFileTest.kt docs/DSL.md
git commit -m "feat: add outputFile to persist PythonExec stdout"
```

---

## Part C — Script execution mode

### Task 5: `script` property and command building

**Files:**
- Modify: `src/main/kotlin/com/example/python/tasks/PythonExec.kt`
- Modify: `src/main/kotlin/com/example/python/service/PythonEnvService.kt` (only if helper needed)
- Create: `src/test/kotlin/com/example/python/tasks/PythonExecScriptTest.kt`
- Modify: `docs/DSL.md`, `docs/roadmap/V1-IMPLEMENTATION-PLAN.md` (move script off v1.1 table)

**Step 1: Write failing tests**

```kotlin
@Test
fun `buildCommand uses managed python plus script path`() {
    // ProjectBuilder + mock envService (pattern from PythonExecVenvExecTest)
    // script.set(project.file("scripts/check.py"))
    // assert command == listOf(python.absolutePath, script.absolutePath) + args
}

@Test
fun `script is mutually exclusive with venvExec`() {
    // assertFailsWith<GradleException> when both set at buildCommand() time
}
```

**Step 2: Run tests to verify failure**

Run: `./gradlew test --tests "*PythonExecScriptTest" --no-daemon`
Expected: FAIL

**Step 3: Implement `script` property**

```kotlin
@get:InputFile
@get:Optional
abstract val script: RegularFileProperty

private fun resolveExecutableCommand(): String = when {
    script.isPresent -> envService.get().resolveExecutable("python").absolutePath
    venvExec.isPresent -> {
        // ...
    }
    executable.isPresent -> {
        // ...
    }
    else -> throw GradleException("...")
}

internal fun buildCommand(): List<String> {
    val base = resolveExecutableCommand()
    return if (script.isPresent) {
        listOf(base, script.get().asFile.absolutePath) + arguments.get()
    } else {
        listOf(base) + arguments.get()
    }
}
```

Add validation: `script` + `venvExec` cannot both be present.

**Step 4: Run tests**

Run: `./gradlew test --tests "*PythonExecScriptTest" --no-daemon`
Expected: PASS

**Step 5: Functional test with fake python + script**

Create `src/functionalTest/kotlin/com/example/python/PythonExecScriptFunctionalTest.kt`:
- `prepareCondaSentinel` + fake `python` binary in env path (mirror `PythonExecVenvExecTest` setup)
- Script file `print("ok")` or shell script stub
- Assert task SUCCESS

**Step 6: Update docs and roadmap**

- `docs/DSL.md`: document `script` property and exclusivity rules
- `docs/roadmap/PROJECT-ROADMAP.md`: check off v1.1 script item when done

**Step 7: Commit**

```bash
git add src/main/kotlin/com/example/python/tasks/PythonExec.kt \
  src/test/kotlin/com/example/python/tasks/PythonExecScriptTest.kt \
  src/functionalTest/kotlin/com/example/python/PythonExecScriptFunctionalTest.kt \
  docs/DSL.md docs/roadmap/PROJECT-ROADMAP.md
git commit -m "feat: add script execution mode to PythonExec"
```

---

## Part D — Backend parity CI (optional, network)

### Task 6: GitHub Actions matrix job for live bootstrap

**Files:**
- Modify: `.github/workflows/ci.yml`
- Create: `src/functionalTest/kotlin/com/example/python/BackendParityFunctionalTest.kt` (tagged or separate source set)

**Step 1: Add test that uses real `envSetup` without sentinel**

```kotlin
// @EnabledIfEnvironmentVariable(named = "CI_NETWORK_TESTS", matches = "true")
@Test
fun `conda envSetup creates install sentinel`() {
    // ...
    }
```

**Step 2: Add CI job `integration`**

```yaml
integration:
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
  runs-on: ubuntu-latest
  env:
    CI_NETWORK_TESTS: true
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: 21
    - run: ./gradlew functionalTest --tests "*BackendParityFunctionalTest" --no-daemon
```

**Step 3: Document in `docs/roadmap/V1-IMPLEMENTATION-PLAN.md`**

Note: PR builds skip network tests; `main` runs weekly or on push.

**Step 4: Commit**

```bash
git add .github/workflows/ci.yml src/functionalTest docs/roadmap/V1-IMPLEMENTATION-PLAN.md
git commit -m "ci: add optional network-backed backend parity test on main"
```

---

## Part E — Release bookkeeping

### Task 7: Close v1.1 in roadmap and bump docs version

**Files:**
- Modify: `docs/roadmap/V1-IMPLEMENTATION-PLAN.md`
- Modify: `docs/roadmap/PROJECT-ROADMAP.md`
- Modify: `docs/DSL.md` (remove v1.1 deferral section or mark shipped)

**Step 1: Verify full check**

Run: `./gradlew check --no-daemon`
Expected: BUILD SUCCESSFUL

**Step 2: Update checklists**

- Mark `outputFile` and script execution complete
- Add v1.1 release checklist section

**Step 3: Commit**

```bash
git add docs/roadmap docs/DSL.md
git commit -m "docs: mark v1.1 features complete in roadmap"
```

---

## Sequencing

| Order | Task | Depends on |
|-------|------|------------|
| 1 | Task 1 — PythonExec functional test | — |
| 2 | Task 2 — Migration notes | — |
| 3 | Task 3 — ignoreExitValue DSL examples | — |
| 4 | Task 4 — outputFile | Task 1 (optional) |
| 5 | Task 5 — script mode | Task 4 |
| 6 | Task 6 — backend parity CI | Task 5 |
| 7 | Task 7 — release bookkeeping | Tasks 4–6 |

Tasks 1–3 can run in parallel. Tasks 4 and 5 are sequential.

## Verification commands (every PR)

```bash
./gradlew detekt ktlintCheck test functionalTest --no-daemon
```

## Definition of done (v1.1)

- [ ] `outputFile` persists stdout; documented in `docs/DSL.md`
- [ ] `script` runs `.py` via managed `python`; mutual exclusion with `venvExec` enforced
- [ ] Unit + functional tests cover new properties
- [ ] `docs/MIGRATION.md` published
- [ ] `./gradlew check` green
- [ ] Roadmap/checklists updated
