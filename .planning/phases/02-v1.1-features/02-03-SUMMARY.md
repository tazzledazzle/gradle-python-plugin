# 02-03 Summary — script Mode

**Status:** Complete  
**Verified:** `./gradlew check` (2026-05-26)

## Delivered

- `PythonExec.script` — `@InputFile`; runs `.py` via managed `python` from `PythonEnvService`
- Mutual-exclusivity validation: `script` ⊥ `venvExec`, `script` ⊥ `executable`
- `PythonExecScriptTest` — command assembly and conflict tests
- `PythonExecScriptFunctionalTest` — offline TestKit with conda sentinel + stub python
- `docs/DSL.md` — full v1.1 API reference (deferral section removed)
- Script paths canonicalized on macOS (`/private/var` symlink fix)

## Verification

- Unit and functional tests pass; EXEC-06 and EXEC-07 satisfied
