# EduReader

EduReader is an Android EPUB reader built with Kotlin, Jetpack Compose, and a clean layered architecture.

## Project Scope

- Import local `.epub` files selected via Android document picker.
- Parse EPUB structure (`container.xml`, OPF, manifest, spine, navigation) and render chapter content.
- Persist reading progress and restore the last opened book.
- Provide runtime monitoring and crash diagnostics.

## Architecture

### General Approach

The app follows a pragmatic Clean Architecture style with explicit boundaries:

- `presentation`: UI, interaction handling, screen state, user intents.
- `domain`: use cases, repository interfaces, business models, result/error contracts.
- `data`: repository implementations, local storage, EPUB parsing, Android-specific import source resolution.
- `core`: DI setup, monitoring/logging abstractions, shared utilities.

This separation keeps UI and framework concerns isolated from business logic and allows replacing data sources without changing use cases.

### Layer Responsibilities

- **Presentation (`presentation/`)**
  - `ReaderViewModel` orchestrates user intents and state transitions.
  - `ReaderState` is modeled as explicit states: `Idle`, `Importing`, `Ready`, `Failure`.
  - Compose screens (`ReaderScreen`, `ReaderContent`) render based on immutable state snapshots.
- **Domain (`domain/`)**
  - Use cases (`ImportEpubFromUriUseCase`, `GetBookUseCase`, `SaveReadingProgressUseCase`, etc.) encode application workflows.
  - `DomainResult` and `DomainError` provide a typed contract for success/failure.
  - Repository interfaces (`EpubRepository`, `ReadingProgressRepository`) define behavior independently from storage/parsing details.
- **Data (`data/`)**
  - `EpubRepositoryImpl` and `ReadingProgressRepositoryImpl` implement repository contracts.
  - `ZipEpubParser` parses EPUB archives and validates required structure.
  - `AndroidEpubImportSourceResolver` copies selected content URI into app-private storage.
  - SharedPreferences-backed data sources persist catalog mappings and reading progress.
- **Core (`core/`)**
  - Hilt modules configure bindings and storage providers.
  - `AppLogger` decouples domain/presentation logging from concrete logger implementation.
  - `MonitoringInitializer` initializes Timber and configures Crashlytics behavior by build type.

### State Management

- Single source of truth: `MutableStateFlow<ReaderState>` in `ReaderViewModel`.
- UI consumes `StateFlow` via Compose `collectAsState()`.
- User actions are modeled as intents (`ReaderIntent`) and processed in one entry point (`onIntent`).
- Progress persistence is debounced (500 ms) to reduce frequent writes while scrolling.
- On app backgrounding, pending progress is flushed immediately.

### Error Handling and Non-standard Scenarios

- Domain-level typed errors:
  - `Validation` (unsupported extension),
  - `NotFound` (missing file/book),
  - `Parsing` (invalid EPUB structure/content),
  - `Storage` (I/O and URI access failures),
  - `Unknown` (fallback).
- UI receives user-facing failures via `ReaderState.Failure`.
- Operational errors are reported through `AppLogger` with contextual tags.
- Non-standard scenarios explicitly handled:
  - Empty EPUB spine -> fail with dedicated message.
  - Missing/invalid MIME marker in EPUB -> parsing failure.
  - Corrupted ZIP/XML -> parsing failure with diagnostic cause.
  - Unsafe ZIP paths (zip-slip) -> extraction denied.
  - Missing TOC -> fallback generated from spine entries.

### Key Decisions and Rationale

- **StateFlow + explicit sealed state**: deterministic rendering and easier recovery logic.
- **Use cases + repositories**: testable and framework-agnostic business workflows.
- **App-private file copy before parsing**: stable random access and simplified parser I/O.
- **SharedPreferences for metadata/progress**: low operational complexity for current app size.
- **Deferred persistence (debounce + flush on pause)**: balance between durability and write frequency.

### Known Limitations and Vulnerable Areas

- No encryption for imported EPUB files in internal storage.
- SharedPreferences is not ideal for large-scale metadata or multi-book analytics.
- Parser uses regex for nav extraction and may be less robust for malformed/complex XHTML navigation.
- Release build currently has `isMinifyEnabled = false`, increasing APK size and reverse-engineering surface.
- Crashlytics is optional at runtime; if unavailable, diagnostics quality is reduced to local logs.
- No CI workflow is configured in repository yet (checks are local unless external pipeline is added).

## Production Operations

### Support Model

- **Monitoring stack**
  - Timber (`DebugTree` in debug, custom release tree in release).
  - Firebase Crashlytics in release mode when available.
- **Incident detection**
  - Crashlytics: crash clusters, stack traces, affected versions.
  - Timber logs: local reproduction and context diagnostics.
- **Prioritization**
  - P0: startup crash/data loss/cannot open EPUB.
  - P1: reading flow broken for a subset of files.
  - P2: non-critical UX defects and cosmetic issues.
- **Response process**
  - Triage incoming issue (impact, reproducibility, scope).
  - Reproduce with sample EPUB and environment details.
  - Prepare fix with focused regression checks.
  - Release patch and monitor crash-free sessions trend.

### Failures and Error Correction Process

- Errors surface via `DomainResult.Failure` and are logged with operation context.
- Root-cause analysis combines:
  - domain error category,
  - stack trace/cause (if present),
  - specific EPUB sample that triggered the issue.
- Typical remediation flow:
  1. Add/extend validation in parser/import.
  2. Improve fallback behavior in ViewModel/UI.
  3. Add regression test case (unit/instrumented) for failed scenario.
  4. Publish patch and verify telemetry deltas.

### Releases and Risk Mitigation

- Recommended release flow:
  1. Local checks: `./gradlew test`, `./gradlew connectedAndroidTest` (if device/emulator available), `./gradlew lint`.
  2. Build release artifact: `./gradlew assembleRelease`.
  3. Stage rollout in Play Console (small percentage first), monitor crash and ANR metrics.
  4. Expand rollout after stability confirmation.
- Risk controls:
  - Keep changes small and feature-focused.
  - Validate with a corpus of real-world EPUB files (valid + intentionally corrupted).
  - Preserve compatibility with previously imported local books and saved progress.

## EPUB Handling

### Local Storage and Processing Flow

1. User picks a document URI via SAF (`OpenDocument`).
2. Content is copied into app-private `filesDir/imported_epub`.
3. EPUB is validated and parsed as ZIP archive.
4. Archive is extracted into `filesDir/epub_extracted/<hashed-path>`.
5. Parsed metadata/spine/TOC are used for reader navigation and rendering.
6. Catalog mapping and progress are persisted in SharedPreferences.

### Current Approach Constraints

- Files remain local to app internal storage; no cloud sync.
- Re-imports can create multiple copies of same logical book.
- No cleanup policy for old imported/extracted files yet.
- Very large EPUB files may increase storage pressure and extraction time.

## Performance

### Potential Bottlenecks

- EPUB extraction and XML parsing for large archives.
- Frequent progress saves during intense scrolling if debounce is bypassed.
- WebView rendering complexity for heavy chapter HTML/CSS.
- Storage growth from accumulated imported/extracted books.

### Optimization Techniques

- Debounced progress persistence and explicit flush on lifecycle pause.
- Lazy chapter navigation using spine index rather than full-book in-memory rendering.
- Persistent extracted cache marker (`.extracted`) to avoid repeated extraction.
- Keep parser and file I/O on `Dispatchers.IO`.

### Additional Optimization Opportunities

- Add background cleanup policy for stale imported/extracted books.
- Replace SharedPreferences with Room for scalable indexing/querying.
- Precompute and cache chapter offsets/TOC mappings for very large books.
- Add macrobenchmark/perf tracing for cold start, import latency, chapter switch time.

## Dependencies and Toolchain

### Build and Language

- Android Gradle Plugin: `8.12.3`
- Gradle Wrapper: `8.13`
- Kotlin: `2.2.21`
- Java/Kotlin JVM target: `17`
- compileSdk / targetSdk: `36`
- minSdk: `24`

### Core Runtime Libraries

- AndroidX Core KTX `1.18.0`
- Lifecycle Runtime/ViewModel KTX `2.10.0`
- Activity Compose `1.13.0`
- Compose BOM `2026.03.01` (+ UI, UI Graphics, Tooling Preview, Material3)
- Hilt Android `2.56.2` + KSP compiler
- Hilt Navigation Compose `1.3.0`
- Timber `5.0.1`
- Firebase BoM `34.12.0` + Crashlytics

### Testing Libraries

- JUnit `4.13.2`
- AndroidX JUnit `1.3.0`
- Espresso Core `3.7.0`
- Compose UI test artifacts from Compose BOM

## Third-party Libraries: Why They Were Chosen

- **Hilt**: standard Android DI with compile-time graph validation; limitation: generated code and annotation processing complexity.
- **Timber**: lightweight structured logging API; limitation: not a full observability backend itself.
- **Firebase Crashlytics**: production crash aggregation and grouping; limitation: external service dependency and optional availability in restricted environments.
- **Jetpack Compose**: declarative UI with direct state-driven rendering; limitation: potential recomposition/performance tuning required for complex screens.

## Setup and Run

### Prerequisites

- Android Studio (latest stable recommended, with Kotlin 2.2+ support).
- Android SDK Platform 36 installed.
- JDK 17 available (or use Android Studio embedded JDK 17).
- Android device/emulator with API 24+.

### Build and Launch (Debug)

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or run directly from Android Studio (`app` configuration).

### Run Quality Checks

```bash
./gradlew test
./gradlew lint
```

For instrumented tests (requires connected device/emulator):

```bash
./gradlew connectedAndroidTest
```

### Build Release Artifact

```bash
./gradlew assembleRelease
```

## Operational Notes

- Crashlytics collection is disabled in debug builds and enabled in release builds (if Firebase is available at runtime).
- If Crashlytics initialization fails, app continues with Timber-only logging.
- Current project does not include CI workflow files; all checks are executed locally unless integrated into external CI/CD.
