# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew build          # Build and run all tests
./gradlew test           # Run all tests
./gradlew clean build    # Clean then build
./gradlew publishToMavenLocal  # Publish to local Maven repo

# Run a single test class
./gradlew test --tests "BytesFileCacheTest"
./gradlew test --tests "llc.berserkr.cache.BytesFileCacheTest"
```

Group: `llc.berserkr`, Version: `1.0.2`, Java target: 8+.

## Architecture

This is a persistent, file-backed cache library. The core storage uses two files per cache: a **hash index file** (fixed-size buckets) and a **segmented blob file** (forward-linked list of variable-length segments). Lookups are O(1) via the hash, but eviction policies traverse the linked list, making large caches O(N) on eviction.

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
- `FilePersistedMaxSizeStreamCache` — intentionally non-functional stub

**Conversion decorators:** `KeyConvertingCache`, `ValueConvertingCache`, `KeyEncodingCache`

**Thread safety:** `SynchronizedCache` (coarse `synchronized`) or `CacheLocks` (per-bucket `ReadWriteLock`). `LocalRandomAccess` uses `ThreadLocal` file handles.

### Storage Internals (`hash/` package)

- `FileHash` / `StreamingFileHash` — hash table backed by two disk files
- `SegmentedFile` — forward-linked list segments with a 1024-byte transaction log at the file head for crash recovery
- Segment states: `FREE_STATE`, `BOUND_STATE`, `TRANSITIONAL_STATE`
- `SegmentedBytesDataManager` / `SegmentedStreamingDataManager` — manage segment I/O

### Factory Methods

`CacheFactory` provides pre-wired stacks (e.g., `stringMaxSizeCache`, `stringExpiringCache`) to avoid boilerplate decorator composition.

### Converters (`converter/` package)

| Converter | Direction |
|-----------|-----------|
| `BytesStringConverter` | `byte[]` ↔ `String` |
| `SerializingConverter<V>` | `Serializable` ↔ `byte[]` |
| `InputStreamConverter` | `InputStream` ↔ `byte[]` |
| `StringSizeConverter` | `String` → `Integer` (byte count) |
| `ReverseConverter<K,V>` | Inverts any converter |

### Known Limitations

- O(N) linked-list traversal for large key sets during eviction
- No background expiration thread — expiry is lazy on access
- Sequential file seeks can bottleneck under heavy concurrent writes
