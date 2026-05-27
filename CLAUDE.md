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

## Common foot-guns (real bugs we have hit)

1. **`jitpack.yml` drift.** When `hivemq-extension-sdk` was removed as an included build (#632) and the toolchain rose to JDK 21 (#635), `jitpack.yml` kept its `openjdk11` + included-build `before_install`. JitPack 2026.5 failed silently and the cached failure had to be deleted by hand in the JitPack UI. Touch `settings.gradle.kts` or change the Java toolchain → update `jitpack.yml` in the same PR.
2. **Shadow plugin 9.x publication variants.** Shadow 9.x adds `shadowRuntimeElements` to the `java` component lazily. The old top-level idiom
   ```kotlin
   javaComponent.withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) { skip() }
   ```
   throws `Variant for configuration 'shadowRuntimeElements' does not exist in component 'java'`. Use the plugin-level toggle (Shadow 9.1.0+, PR [GradleUp/shadow#1662](https://github.com/GradleUp/shadow/pull/1662)):
   ```kotlin
   shadow {
       addShadowVariantIntoJavaComponent = false
   }
   ```
3. **Branch protection on `master`.** The release flow requires temporarily disabling "Do not allow bypassing the above settings" on rule [5047065](https://github.com/hivemq/hivemq-community-edition/settings/branch_protection_rules/5047065) to push the version-bump commit. **Always re-enable it** after pushing.

## Releasing

The full CE release runbook lives in the dev handbook:
https://github.com/hivemq/hivemq-dev-handbook/blob/master/_kanban/documentation/templates/ce-release.md

Key steps that span multiple repos:

- This repo: version bump in `gradle.properties`, `gradle/libs.versions.toml` (`hivemq-extensionSdk`), `README.adoc`; run `./gradlew updateThirdPartyLicenses`; commit `Version to YYYY.i`; tag `YYYY.i`; create GitHub release; approve publish workflow.
- `hivemq-website-cms`: add blogpost under `src/lib/cms/releases/hivemq-ce/`, then promote `develop` → `staging` → `production`.
- After tagging, **verify** the JitPack build at `https://jitpack.io/#hivemq/hivemq-community-edition/<version>` actually succeeded — a stale `jitpack.yml` will not be caught by the GitHub Actions publish workflow.
