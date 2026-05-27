# Gradle Python Plugin v1 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver a v1.0 Gradle Python plugin implementation with `ignoreExitValue`, shared environment bootstrap via `BuildService`, and `conda`/`uv` backend support.

**Architecture:** Keep task-facing APIs stable (`PythonExec`, extension DSL) while routing environment resolution through a shared `PythonEnvService`. Implement backend-specific behavior behind internal installers/resolvers, then protect decisions with unit + functional tests and CI checks.

**Tech Stack:** Kotlin, Gradle plugin APIs, Gradle TestKit, JUnit (or project-standard Kotlin test framework), CI workflows.

---

### Task 1: Scaffold plugin project structure

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `src/main/kotlin/com/example/python/PythonPlugin.kt`
- Create: `src/main/kotlin/com/example/python/PythonExtension.kt`
- Create: `src/main/kotlin/com/example/python/tasks/PythonExec.kt`
- Create: `src/main/kotlin/com/example/python/tasks/EnvSetupTask.kt`
- Test: `src/test/kotlin/com/example/python/PluginSmokeTest.kt`

**Step 1: Write the failing test**

- Write plugin smoke test that applies plugin and asserts task/extension registration.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*PluginSmokeTest" -i`
Expected: FAIL due to missing plugin/task classes.

**Step 3: Write minimal implementation**

- Add minimal plugin, extension, and task class registration.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*PluginSmokeTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add build.gradle.kts settings.gradle.kts src/main src/test
git commit -m "feat: scaffold gradle python plugin core structure"
```

### Task 2: Implement core process execution in `PythonExec`

**Files:**
- Modify: `src/main/kotlin/com/example/python/tasks/PythonExec.kt`
- Test: `src/test/kotlin/com/example/python/tasks/PythonExecTest.kt`

**Step 1: Write the failing test**

- Add tests for successful process execution with captured stdout/stderr and exit code.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*PythonExecTest" -i`
Expected: FAIL for unimplemented execution logic.

**Step 3: Write minimal implementation**

- Implement process execution and output/exit capture contract.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*PythonExecTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/com/example/python/tasks/PythonExec.kt src/test/kotlin/com/example/python/tasks/PythonExecTest.kt
git commit -m "feat: implement pythonexec process execution contract"
```

### Task 3: Add `ignoreExitValue` behavior

**Files:**
- Modify: `src/main/kotlin/com/example/python/tasks/PythonExec.kt`
- Modify: `src/main/kotlin/com/example/python/PythonPlugin.kt`
- Test: `src/test/kotlin/com/example/python/tasks/PythonExecIgnoreExitValueTest.kt`

**Step 1: Write the failing test**

- Add tests for default fail-fast and explicit ignore mode.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*IgnoreExitValue*" -i`
Expected: FAIL due to missing property/logic.

**Step 3: Write minimal implementation**

- Add `ignoreExitValue` input property (default `false`) and gate throw logic.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*IgnoreExitValue*" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/com/example/python/tasks/PythonExec.kt src/main/kotlin/com/example/python/PythonPlugin.kt src/test/kotlin/com/example/python/tasks/PythonExecIgnoreExitValueTest.kt
git commit -m "feat: add ignoreexitvalue control for pythonexec"
```

### Task 4: Introduce `PythonEnvService` for shared bootstrap

**Files:**
- Create: `src/main/kotlin/com/example/python/service/PythonEnvService.kt`
- Modify: `src/main/kotlin/com/example/python/PythonPlugin.kt`
- Modify: `src/main/kotlin/com/example/python/tasks/PythonExec.kt`
- Modify: `src/main/kotlin/com/example/python/tasks/EnvSetupTask.kt`
- Test: `src/test/kotlin/com/example/python/service/PythonEnvServiceTest.kt`

**Step 1: Write the failing test**

- Add tests for service registration and one-time lazy bootstrap semantics.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*PythonEnvServiceTest" -i`
Expected: FAIL due to missing service.

**Step 3: Write minimal implementation**

- Implement service params/state, register `sharedServices`, wire `usesService`.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*PythonEnvServiceTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/com/example/python/service/PythonEnvService.kt src/main/kotlin/com/example/python/PythonPlugin.kt src/main/kotlin/com/example/python/tasks/PythonExec.kt src/main/kotlin/com/example/python/tasks/EnvSetupTask.kt src/test/kotlin/com/example/python/service/PythonEnvServiceTest.kt
git commit -m "feat: add shared python environment build service"
```

### Task 5: Add `envManager` + uv backend internals

**Files:**
- Modify: `src/main/kotlin/com/example/python/PythonExtension.kt`
- Create: `src/main/kotlin/com/example/python/internal/UvInstaller.kt`
- Create: `src/main/kotlin/com/example/python/internal/UvEnvResolver.kt`
- Modify: `src/main/kotlin/com/example/python/service/PythonEnvService.kt`
- Test: `src/test/kotlin/com/example/python/internal/UvInstallerTest.kt`
- Test: `src/test/kotlin/com/example/python/internal/UvEnvResolverTest.kt`

**Step 1: Write the failing test**

- Add tests for env manager validation and uv resolver/installer behavior.

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*Uv*" -i`
Expected: FAIL due to missing uv classes.

**Step 3: Write minimal implementation**

- Add `envManager`, `uvVersion`, `uvRepoUrl`; route backend logic via service.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*Uv*" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/main/kotlin/com/example/python/PythonExtension.kt src/main/kotlin/com/example/python/internal/UvInstaller.kt src/main/kotlin/com/example/python/internal/UvEnvResolver.kt src/main/kotlin/com/example/python/service/PythonEnvService.kt src/test/kotlin/com/example/python/internal/UvInstallerTest.kt src/test/kotlin/com/example/python/internal/UvEnvResolverTest.kt
git commit -m "feat: add uv backend and env manager selection"
```

### Task 6: Add parallel functional tests with Gradle TestKit

**Files:**
- Create: `src/functionalTest/kotlin/com/example/python/ParallelExecutionFunctionalTest.kt`
- Modify: `build.gradle.kts`

**Step 1: Write the failing test**

- Add TestKit fixture with multiple `PythonExec` tasks under parallel execution.

**Step 2: Run test to verify it fails**

Run: `./gradlew functionalTest --tests "*ParallelExecutionFunctionalTest" -i`
Expected: FAIL before concurrency safety is fully wired.

**Step 3: Write minimal implementation**

- Ensure test source set/task setup and any missing service wiring required by fixture.

**Step 4: Run test to verify it passes**

Run: `./gradlew functionalTest --tests "*ParallelExecutionFunctionalTest" -i`
Expected: PASS.

**Step 5: Commit**

```bash
git add build.gradle.kts src/functionalTest/kotlin/com/example/python/ParallelExecutionFunctionalTest.kt
git commit -m "test: add parallel execution functional coverage"
```

### Task 7: Add CI quality gates and docs updates

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `README.md`
- Modify: `docs/roadmap/PROJECT-ROADMAP.md`
- Modify: `docs/roadmap/V1-IMPLEMENTATION-PLAN.md`
- Modify: `docs/roadmap/CONTRIBUTING-ROADMAP.md`

**Step 1: Write the failing test/check**

- Define expected CI checks (test, lint/style, functional tests) and verify missing pipeline.

**Step 2: Run check to verify it fails**

Run: `gh workflow lint` (or repo-specific validation command)
Expected: FAIL/absent workflow prior to adding CI config.

**Step 3: Write minimal implementation**

- Add CI workflow and document branch policy expectations.

**Step 4: Run checks to verify it passes**

Run: `./gradlew test functionalTest -i`
Expected: PASS locally (or explicitly document environment-dependent gaps).

**Step 5: Commit**

```bash
git add .github/workflows/ci.yml README.md docs/roadmap/*.md
git commit -m "ci: add v1 quality gates and roadmap-aligned docs"
```

### Task 8: Final release candidate verification

**Files:**
- Modify: `README.md` (release checklist section if needed)
- Modify: `docs/roadmap/PROJECT-ROADMAP.md` (status updates)

**Step 1: Write the failing checklist**

- Enumerate unresolved blockers from roadmap success criteria.

**Step 2: Run verification to confirm gaps**

Run: `./gradlew clean test functionalTest -i`
Expected: identify remaining blockers if any.

**Step 3: Write minimal implementation**

- Address only remaining blockers; avoid scope creep.

**Step 4: Run verification to confirm readiness**

Run: `./gradlew clean test functionalTest -i`
Expected: PASS with all v1 criteria met.

**Step 5: Commit**

```bash
git add README.md docs/roadmap/PROJECT-ROADMAP.md
git commit -m "chore: finalize v1 release readiness checklist"
```
