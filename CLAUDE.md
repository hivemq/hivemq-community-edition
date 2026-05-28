# Agent Instructions

Notes for AI coding agents (and humans) contributing to **hivemq-community-edition**.

## Build / publish surface — keep these in sync

This repo publishes through **three different paths**, each with its own configuration. A change to one often requires a matching change in the others.

| Path | Config | Java version | Build command |
|---|---|---|---|
| GitHub Actions CI (`push`) | [`.github/workflows/check.yml`](.github/workflows/check.yml) | 11 + 21 | `./gradlew test javadoc hivemqZip` |
| GitHub Actions Publish (`release:published`) | [`.github/workflows/publish.yml`](.github/workflows/publish.yml) | 11 + 21 | `./gradlew publishEmbeddedPublicationToMavenCentral` |
| JitPack (consumer-on-demand) | [`jitpack.yml`](jitpack.yml) | 21 | `./gradlew publishToMavenLocal -PsigningDisabled=true` |

**Rule of thumb:** if you change any of the following, re-verify the *publish path* still works on a clean checkout with JDK 21 only (the jitpack environment is the strictest):

- `build.gradle.kts` (especially anything touching `publishing`, `signing`, `shadow`, `components["java"]`)
- `gradle/libs.versions.toml` (plugin or SDK version bumps)
- `settings.gradle.kts` (included builds)
- `gradle/wrapper/gradle-wrapper.properties` (Gradle version)
- Java toolchain version anywhere

### The pre-tag smoke test

Run this locally before cutting a release tag:

```bash
./gradlew publishToMavenLocal -PsigningDisabled=true
```

That command is what jitpack runs. If it fails locally, jitpack will fail too — and so will Maven Central publishing on release. The `jitpack-smoke` CI job runs the same command on every PR that touches build files.

## Common foot-guns

1. **`jitpack.yml` drift.** `jitpack.yml` has its own `jdk:` and `install:` directives that are independent of the GitHub Actions workflows. When `hivemq-extension-sdk` was removed as an included build and the toolchain rose to JDK 21, `jitpack.yml` had to be updated separately. Touch `settings.gradle.kts` or change the Java toolchain → update `jitpack.yml` in the same PR.
