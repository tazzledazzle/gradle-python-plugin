# 02-01 Summary — Release Hardening

**Status:** Complete  
**Verified:** `./gradlew check` (2026-05-26)

## Delivered

- `PythonExecFunctionalTest` — TestKit end-to-end run with explicit `executable` (no network)
- `docs/MIGRATION.md` — v1 workaround → plugin-native replacements
- `docs/DSL.md` — `ignoreExitValue` warn-only and conditional logging examples
- `README.md` — link to migration guide

## Verification

- Functional test passes offline via `sh`/`cmd` echo pattern
- All must-have truths from plan satisfied
