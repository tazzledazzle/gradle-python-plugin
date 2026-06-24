# Publishing to the Gradle Plugin Portal

## One-time setup

1. Create an account at [plugins.gradle.org](https://plugins.gradle.org/).
2. Generate an API key under **Account → API Keys**.
3. Add credentials to `~/.gradle/gradle.properties` (never commit these):

```properties
gradle.publish.key=<your-api-key>
gradle.publish.secret=<your-api-secret>
```

4. Claim the plugin ID `com.tazzledazzle.python` on first publish (Gradle will prompt in the portal UI).

## Publish locally

```bash
./gradlew check publishPlugins
```

`publishPlugins` runs validation, signs the plugin metadata, and uploads to the portal. New versions may take a few minutes to appear in search.

## Publish from CI

The [publish workflow](../.github/workflows/publish.yml) runs on version tags (`v*`).

Add GitHub repository secrets:

| Secret | Value |
|--------|--------|
| `GRADLE_PUBLISH_KEY` | Portal API key |
| `GRADLE_PUBLISH_SECRET` | Portal API secret |

Release flow:

```bash
# Bump version in gradle.properties, commit, then:
git tag v0.1.0
git push origin main --tags
```

## Consumer setup (portfolio projects)

In `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

In `build.gradle.kts`:

```kotlin
plugins {
    id("com.tazzledazzle.python") version "0.1.0"
}
```

Use the [latest published version](https://plugins.gradle.org/plugin/com.tazzledazzle.python) when bumping.
