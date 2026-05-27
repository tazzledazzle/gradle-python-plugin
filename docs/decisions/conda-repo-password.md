# ADR: Conda Repository Password via Gradle Credentials API

**Status:** Accepted (2026-05-26)  
**Owner:** Terence

## Context

Private Conda/Miniforge mirrors may require HTTP basic authentication. Storing passwords in plain `build.gradle.kts` is unsafe and complicates CI.

## Decision

Use Gradle's **credentials property convention** (`{name}Password` / `{name}Username` in `gradle.properties` or CI secrets), with an optional explicit DSL override. This matches how Gradle wires `PasswordCredentials` without failing builds when credentials are absent.

### Resolution order

1. `python { condaRepoPassword.set("...") }` when set directly on the extension
2. Gradle property `${condaRepoCredentialsName}Password` (default property name: `condaRepoPassword`)
3. Empty string when unset (public repos)

### Configuration

**Option A — `gradle.properties` (recommended for CI secrets):**

```properties
condaRepoUsername=ci-user
condaRepoPassword=${CONDA_REPO_PASSWORD}
```

**Option B — `settings.gradle.kts` credentials container:**

Declare `PasswordCredentials` named `condaRepo`; Gradle exposes `condaRepoUsername` / `condaRepoPassword` properties to the build.

## Consequences

- Passwords are not logged by the plugin.
- Download helpers (`CondaInstaller`, future authenticated uv mirrors) receive resolved credentials via `PythonEnvService` parameters.
- Release checklist item **condaRepoPassword credentials API** is closed for v1.0.

## Rejected alternatives

- Plain-text extension-only password (no credentials integration)
- Environment-variable-only lookup without Gradle credentials (less idiomatic for Gradle plugins)
