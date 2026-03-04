# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android client library for tracking user events (Nosara/Tracks), A/B testing (ExPlat), and crash logging (Sentry). Published to Automattic's S3 Maven repository. Used internally by Automattic mobile apps.

## Build Commands

```bash
./gradlew assembleDebug              # Build all modules
./gradlew testDebugUnitTest          # Run all unit tests
./gradlew lintDebug                  # Run lint checks
./gradlew ciktlint                   # Run ktlint checks
./gradlew buildHealth                # Dependency analysis
./gradlew :experimentation:apiCheck  # Binary compatibility check (experimentation only)
```

To run a single test class:

```bash
./gradlew :experimentation:testDebugUnitTest --tests "com.automattic.android.experimentation.ExPlatTest"
./gradlew :crashlogging:testDebugUnitTest --tests "com.automattic.android.tracks.crashlogging.SentryCrashLoggingTest"
```

**Note:** `gradle.properties` is gitignored. CI copies `gradle.properties-example` before building. Locally, copy it manually if needed: `cp gradle.properties-example gradle.properties`.

## Architecture

Three independently published library modules plus a sample app and benchmark module:

| Module | Artifact | Language | Description |
|--------|----------|----------|-------------|
| `AutomatticTracks` | `com.automattic:Automattic-Tracks-Android` | Java + Kotlin | Core event tracking with SQLite storage and background network transmission |
| `experimentation` | `com.automattic.tracks:experimentation` | Kotlin | ExPlat A/B testing SDK with file-based caching and OkHttp REST client |
| `crashlogging` | `com.automattic.tracks:crashlogging` | Kotlin | Sentry-based crash logging, performance monitoring, and error tracking |
| `sampletracksapp` | — | Kotlin | Sample app demonstrating library usage |
| `benchmark` | — | Kotlin | Performance benchmarking |

### Key architectural details

- **AutomatticTracks** uses a queue-based architecture with background threads for event buffering, DB-to-network transfer, and network transmission. Events expire after 14 days.
- **experimentation** has `explicitApi()` enabled and uses binary compatibility validation (`apiCheck`). Public API surface is tracked in `experimentation/api/experimentation.api`.
- **crashlogging** wraps Sentry SDK via `SentryErrorTrackerWrapper` and exposes a `CrashLoggingDataProvider` interface for host apps to supply user/app context.
- Modules publish independently to S3. No product flavors or build variants.

## Testing Conventions

- **Frameworks:** JUnit 4, AssertJ, Mockito-Kotlin, kotlinx-coroutines-test, MockWebServer
- **Assertions:** Use AssertJ (`assertThat`), not kotlin.test assertions
- **Naming:** Backtick-quoted names: `` `given ..., when ..., then ...` ``
- **Coroutines:** Use `runTest` from `kotlinx.coroutines.test`

## Important Gotchas

- Build files use **Groovy** (`build.gradle`, `settings.gradle`), not Kotlin DSL
- Dependency versions are defined as `ext` properties in root `build.gradle`, not a version catalog
- Lint treats warnings as errors (`warningsAsErrors true`). `AutomatticTracks` uses a lint baseline file
- CI runs `./gradlew lint ciktlint` (not `lintDebug`) — the `ciktlint` task is from `ktlint.gradle`

## Publishing

CI publishes on every PR commit and trunk merge:
- PR: `{pr-number}-{commit-full-sha1}`
- Trunk merge: `trunk-{commit-full-sha1}`
- Tag: `{tag-name}`
