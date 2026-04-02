# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew build                      # Build all modules and run all tests (Java + native C++)
./gradlew :core:test                 # Run only Java tests
./gradlew :nativelib:runNativeTests  # Run only C++ native tests (requires cmake)
./gradlew clean build                # Clean then build
./gradlew :core:publishToMavenLocal  # Publish core to local Maven repo

# Run a single test class
./gradlew :core:test --tests "BytesFileCacheTest"
./gradlew :core:test --tests "llc.berserkr.cache.BytesFileCacheTest"
```

Gradle 9.1.0, Kotlin DSL. Version catalog at `gradle/libs.versions.toml`.

Group: `llc.berserkr`, Artifact: `java-file-cache`, Version: `1.0.2`.

## Project Structure

Multi-module Gradle build:

- **`core/`** — Pure Java library (Java 21). The persistent file-backed cache. JUnit 5 + FEST Assert for tests.
- **`nativelib/`** — Android NDK library (C++23, minSdk 33). JNI bridge to a native `NativeCache` class (currently a stub). Google Test for C++ tests. Depends on `:core`.

Source lives under `core/src/` and `nativelib/src/`, not the root `src/`.

## Architecture

Persistent, file-backed cache library. Core storage uses two files per cache: a **hash index file** (fixed-size buckets) and a **segmented blob file** (forward-linked list of variable-length segments). Lookups are O(1) via the hash; eviction policies traverse the linked list (O(N)).

### Core Interfaces

- `Cache<K, V>` — 6 ops: `put`, `get`, `exists`, `remove`, `clear`, `getAll`
- `Converter<Old, New>` — 2 ops: `convert`, `restore` (bidirectional)

### Layered Design (Decorator Pattern)

Functionality is composed by wrapping caches:

```
User-facing type (e.g., String key, Serializable value)
       ↓ KeyConvertingCache / ValueConvertingCache
byte[] key, byte[] value
       ↓ FilePersistedMaxSizeCache / FilePersistedMaxCountCache / FilePersistedExpiringCache
Eviction tracking (file-persisted linked list)
       ↓ SynchronizedCache (optional coarse lock)
BytesFileCache  ←→  FileHash  ←→  SegmentedFile
```

**Base caches:** `BytesFileCache` (bytes→bytes), `StreamFileCache` (bytes→InputStream)

**Eviction decorators** (all in `llc.berserkr.cache`):
- `FilePersistedMaxSizeCache` — LRU by total byte size
- `FilePersistedMaxCountCache` — FIFO by entry count
- `FilePersistedExpiringCache` — TTL-based (lazy cleanup, no background thread)

**Conversion decorators:** `KeyConvertingCache`, `ValueConvertingCache`, `KeyEncodingCache`

**Thread safety:** `SynchronizedCache` (coarse `synchronized`) or `CacheLocks` (per-bucket `ReadWriteLock`). `LocalRandomAccess` uses `ThreadLocal` file handles.

### Storage Internals (`hash/` package)

- `FileHash` / `StreamingFileHash` — hash table backed by two disk files
- `SegmentedFile` — forward-linked list segments with a 1024-byte transaction log at the file head for crash recovery
- Segment states: `FREE_STATE`, `BOUND_STATE`, `TRANSITIONAL_STATE`
- `SegmentedBytesDataManager` / `SegmentedStreamingDataManager` — manage segment I/O

### Native Module (`nativelib/`)

C++ `NativeCache` class exposed to Java/Android via JNI (`nativelib.cpp` bridges to `NativeCacheBridge`). Currently a stub — only `put` increments the entry count; `get`/`exists`/`remove` are no-ops. Native C++ tests use Google Test (fetched via CMake FetchContent). Host-side tests are triggered by the Gradle `check` task.

### Factory Methods

`CacheFactory` provides pre-wired decorator stacks (e.g., `stringMaxSizeCache`, `stringExpiringCache`) to avoid boilerplate composition.

### Known Limitations

- O(N) linked-list traversal for large key sets during eviction
- No background expiration thread — expiry is lazy on access
- Sequential file seeks can bottleneck under heavy concurrent writes
- `FilePersistedMaxSizeStreamCache` is an intentionally non-functional stub
