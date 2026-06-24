# 02-04 Summary — Optional Network CI

**Status:** Complete  
**Verified:** `./gradlew check` (2026-05-26)

## Delivered

- `BackendParityFunctionalTest` — real `envSetup` without sentinel; gated by `@EnabledIfEnvironmentVariable(named = "CI_NETWORK_TESTS", matches = "true")`
- `.github/workflows/ci.yml` — `integration` job on `main` push with `CI_NETWORK_TESTS=true`
- Default PR CI unchanged (offline functional tests only)

## Verification

- Test skipped locally and on PR CI; integration job configured for main-branch pushes
- TEST-02 satisfied
