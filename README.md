# Java File Cache

A persistent, file-backed cache library written in Java, compatible with Android. Provides a composable, decorator-based cache system with multiple eviction strategies, thread safety options, and flexible key/value type conversions. Includes an optional native (C++) locking implementation via JNI for Android NDK environments.

## Project Structure

Multi-module Gradle build (Gradle 9.1.0, Kotlin DSL):

```
java-file-cache/
├── core/          — Pure Java library (Java 21). Cache implementation, locking, converters, tests.
├── nativelib/     — Android NDK library (C++23, minSdk 33). Native cache locks via JNI. Google Test.
├── app/           — Android demo app showcasing the cache with async image loading.
└── gradle/        — Version catalog (libs.versions.toml), wrapper.
```

- **`core/`** — The persistent file-backed cache library. JUnit 5 + FEST Assert for tests. Published as `llc.berserkr:java-file-cache:1.0.2`.
- **`nativelib/`** — Android NDK module with C++ implementations of `NativeCacheLocks` and `NativeCache` (stub), exposed to Java via JNI. Google Test for native tests, host-built shared library for JVM tests. Depends on `:core`.
- **`app/`** — Android application (`llc.berserkr.androidfilecache`) demonstrating the cache library with multi-threaded image caching using country flag assets.

## Overview

This project implements a persistent key/value cache backed by two files on disk. The core storage engine uses a **hash index file** mapped to a **forward-linked list of segments**, enabling efficient lookups with collision handling and crash recovery. On top of this storage engine, a rich set of decorators adds eviction policies, type conversion, thread safety, and more.

The library is designed to be composed: you combine small, focused building blocks to create a cache tailored to your exact requirements.

## Architecture

### File Storage Mechanism

The cache is built on two files:

- **Hash Index File**: A fixed-size file where each position represents a hash bucket. Each bucket stores an 8-byte pointer (a `long`) into the segment/blob file. The position is calculated as `Math.abs(hashCode(key)) % hashSize`.

- **Segment File**: A forward-linked list of variable-sized segments. Each segment stores one or more `(key, value)` pairs as a `Set<Pair<byte[], byte[]>>`, handling hash collisions. The layout of each segment is:
  ```
  [segmentSize: 4 bytes] [type: 1 byte] [fillSize: 4 bytes] [payload: variable]
  ```

  Segment types include:
  - `FREE_STATE`: Available space
  - `BOUND_STATE`: Allocated segment
  - `TRANSITIONAL_STATE`: In-transition
  - `WRITING_TRANSACTION`, `MERGE_TRANSACTION`, `ADD_END_TRANSACTION`: For crash recovery

A 1024-byte transaction log at the head of the segment file enables recovery from partial writes on restart.

### Design Patterns

- **Decorator Pattern**: The primary composition mechanism. Wrap any `Cache` with another `Cache` to add behavior (synchronization, type conversion, eviction, etc.).
- **Factory Pattern**: `CacheFactory` provides static factory methods that pre-wire common decorator stacks. `CacheLocksFactory` selects between Java and native lock implementations.
- **Strategy Pattern**: The `Converter` interface abstracts type transformations, allowing any data type to be used as a key or value.

### Thread Safety

Thread safety is provided at multiple levels:

| Mechanism | Granularity | Notes |
|---|---|---|
| `SynchronizedCache` | Coarse (all operations) | `synchronized` methods |
| `CacheLocks` | Per-bucket reader/writer locks | Concurrent reads, exclusive writes |
| `CacheLocksFactory` | Lock implementation selection | Chooses Java or native locks at runtime |
| `LocalRandomAccess` | Per-thread file handles | `ThreadLocal<RandomAccessFile>` for r/rw modes |
| `ConcurrentHashMap` | Hash lock registry | Thread-safe lock lookup in `StreamingFileHash` |

For most use cases, wrapping your cache in `SynchronizedCache` is sufficient. For high-read-throughput scenarios, the bucket-level `CacheLocks` allow concurrent reads from different buckets.

#### CacheLocks: Java vs Native

`CacheLocksFactory` provides two implementations of the `CacheLocks` interface:

- **`CacheLocksImpl`** (Java) — Per-instance monitor object for the fast path (`IgnoredWriteLocks`), shared monitor for the slow path (`StandardSharedWriteLocks`).
- **`NativeCacheLocksImpl`** (C++ via JNI) — Per-instance `std::shared_mutex` for the fast path, shared `std::mutex` + `std::condition_variable` for the slow path.

Both use the same fast/slow path optimization:
- **Fast path** (`IgnoredWriteLocks`): Per-key locking only. Writes on one key do not block reads or writes on other keys. Each lock instance has its own synchronization primitive with zero cross-instance contention.
- **Slow path** (`StandardSharedWriteLocks`): Global write lock. When any key is being written, all reads on all keys are blocked. Retained for legacy use cases requiring strict global consistency.

```java
// Java locks (default)
CacheLocks lock = CacheLocksFactory.createJavaWithIgnoredWriteLocks();

// Native locks (when native library is available)
CacheLocks lock = CacheLocksFactory.createNativeWithIgnoredWriteLocks();

// Auto-select: native if available, otherwise Java
CacheLocks lock = CacheLocksFactory.createWithIgnoredWriteLocks(false);
```

### Native Module (`nativelib/`)

The native module provides C++ implementations exposed to Java/Android via JNI:

- **`NativeCacheLocks`** — Reader-writer lock implementation using `std::shared_mutex` (fast path) or `std::mutex` + `std::condition_variable` (slow path). JNI bridge via `NativeCacheLocksImpl` in the `core` module.
- **`NativeCache`** — Stub hash cache interface (implementation TBD). JNI bridge in `nativelib.cpp`.

Native C++ tests use Google Test (fetched via CMake FetchContent). The Gradle build compiles a host-side shared library (`libnativelib`) so that JVM unit tests in `core` can load and test the native implementation directly.

---

## Installation

### Gradle

```groovy
implementation 'llc.berserkr:java-file-cache:1.0.2'
```

---

## Core Interfaces

### `Cache<Key, Value>`

The root interface implemented by all cache types:

```java
void put(Key key, Value value) throws IOException;
Value get(Key key) throws IOException;
boolean exists(Key key) throws IOException;
void remove(Key key) throws IOException;
void clear() throws IOException;
List<Value> getAll(List<Key> keys) throws IOException;
```

### `Converter<Old, New>`

A two-way type converter:

```java
New convert(Old value);     // Old → New (e.g., for storage)
Old restore(New value);     // New → Old (e.g., for retrieval)
```

Built-in converters:

| Converter | Direction |
|---|---|
| `BytesStringConverter` | `byte[]` ↔ `String` |
| `SerializingConverter<V>` | `Serializable` ↔ `byte[]` |
| `InputStreamConverter` | `InputStream` ↔ `byte[]` |
| `InputStreamStringConverter` | `InputStream` ↔ `String` |
| `StringSizeConverter` | `Integer` (size) ↔ `String` |
| `SerializingStreamConverter<V>` | `Serializable` ↔ `InputStream` |
| `ReverseConverter<K, V>` | Wraps any converter and inverts its direction |

---

## Cache Types

### Base Caches

#### `BytesFileCache`

The simplest cache. Stores raw `byte[]` keys and `byte[]` values directly in the hash/segment files. No eviction.

```java
final BytesFileCache cache = new BytesFileCache(hashCacheDir);

cache.put("key".getBytes(), "value".getBytes());

final String value = cache.get("key".getBytes());

assertEquals("value", value);
```

#### `StreamFileCache`

Like `BytesFileCache` but values are stored/retrieved as `InputStream`. Uses the streaming segment file manager.

---

### Decorator Caches

These wrap any `Cache` to add behavior without changing storage logic.

#### `SynchronizedCache<K, V>`

Adds coarse-grained thread safety via `synchronized` on all cache methods. Wraps any existing cache.

```java
final Cache<String, String> safe = new SynchronizedCache<>(existingCache);
```

#### `KeyConvertingCache<K, OldK, V>`

Converts the key type before delegating to an inner cache. Useful for adapting `String` keys to `byte[]`.

```java
// Convert String keys to byte[] for storage in BytesFileCache
final Cache<String, byte[]> cache = new KeyConvertingCache<>(
    new BytesFileCache(dir),
    new ReverseConverter<>(new BytesStringConverter())
);
```

#### `ValueConvertingCache<K, V, OldV>`

Converts the value type before delegating. Chain multiple to transform values through several types.

```java
// Store Java objects as bytes
final Cache<String, MyObject> cache = new ValueConvertingCache<>(
    innerBytesCache,
    new SerializingConverter<>()
);
```

#### `KeyEncodingCache<V>`

URL-encodes `String` keys before delegating. Supports UTF-8, ASCII, ISO-8859-1, UTF-16 encoding.

#### `ResourceLoaderCache<K, V>`

Wraps a read-only `ResourceLoader` as a `Cache`. Write operations (`put`, `remove`, `clear`) are no-ops.

---

### Eviction Caches

These add eviction policies backed by an additional persistence cache that tracks metadata (linked list topology, timestamps, counts).

#### `FilePersistedMaxSizeCache<V>` — LRU by byte size

Evicts least-recently-used entries when total size exceeds a configurable maximum.

- Uses a file-persisted **doubly-linked list** (`topKey` → ... → `bottomKey`) to track access order.
- Requires a `Converter<Integer, Value>` to compute the byte size of each value.
- On eviction: removes from the bottom of the list until size is within budget.

```java
final int maxSize = 5000; // bytes
final Converter<Integer, String> converter =
    new ReverseConverter<>(new StringSizeConverter());

final Cache<String, String> cache =
    CacheFactory.getMaxSizeFileCache(maxSize, dataFolder, converter);
```

#### `FilePersistedMaxCountCache<V>` — FIFO by entry count

Evicts oldest entries (FIFO) when the number of entries exceeds a configurable maximum.

- Uses the same file-persisted linked list structure.
- Accepts an optional `Consumer<Value>` callback invoked on every evicted entry.

```java
final File dataFolder = new File(root, "data");
final int maxCount = 50;

final Cache<String, String> cache =
    CacheFactory.getSerializingMaxCountFileCache(maxCount, dataFolder, (removed) -> {
        System.out.println("Evicted: " + removed);
    });
```

#### `FilePersistedExpiringCache<V>` — TTL-based expiration

Expires entries after a configurable time-to-live.

- Stores `lastUpdated` timestamps per key in a persistence cache.
- **Lazy cleanup**: rather than a background thread, cleanup is triggered during normal cache access once `cleanupTimeout` milliseconds have elapsed since the last cleanup pass.
- Cleanup removes all entries where `currentTime - lastUpdated > timeout`.

```java
final File dataFolder = new File(root, "data");
final int expiringValue = 2000; // ms

final Converter<Integer, String> converter =
    new ReverseConverter<>(new StringSizeConverter());

final Cache<String, String> cache =
    CacheFactory.getExpiringFileCache(expiringValue, dataFolder, converter);
```

---

## Factory Methods (`CacheFactory`)

`CacheFactory` pre-wires common decorator stacks so you don't have to compose them manually.

| Method | Eviction |
|---|---|
| `getMaxSizeFileCache(maxSize, dir, sizeConverter)` | LRU by size |
| `getExpiringFileCache(expireTimeout, dir, sizeConverter)` | TTL |
| `getSerializingFileCache(maxSize, expireTimeout, dir, sizeConverter)` | LRU by size + TTL |
| `getSerializingMaxCountFileCache(maxCount, dir, onRemoved)` | FIFO by count (with callback) |
| `getMaxSizeExpiringFileCache(dir, maxSize, expireTimeout, sizeConverter, valueToBytes)` | LRU by size + TTL (custom) |

---

## Composing Caches Manually

The real power of this library is manual composition. Each decorator has a single responsibility and can be stacked in any order:

```java
// Thread-safe, size-limited, serializing cache over a raw BytesFileCache
final Cache<String, MyObject> cache =
    new SynchronizedCache<>(
        new FilePersistedMaxSizeCache<>(
            dataFolder,
            new ValueConvertingCache<>(                      // MyObject ↔ byte[]
                new KeyConvertingCache<>(                    // String key → byte[]
                    new BytesFileCache(dataFolder),
                    new ReverseConverter<>(new BytesStringConverter())
                ),
                new SerializingConverter<>()
            ),
            new ReverseConverter<>(new StringSizeConverter()),
            maxSize
        )
    );
```

### Full Example: Max Size + Expiring Cache

```java
public static final <Value> Cache<String, Value> getMaxSizeExpiringFileCache(
    final File cacheRoot,
    final int maxSize,
    final int expireTimeout,
    final Converter<Integer, Value> sizeConverter,
    final Converter<Value, byte[]> valueConvertJobToBytes
) throws IOException {

    final File dataFolder = new File(cacheRoot, "data");
    final File expiringRoot = new File(cacheRoot, "expiringRoot");
    final File expiringDataFolder = new File(expiringRoot, "data");

    final Cache<String, Value> fileCache =
        new SynchronizedCache<>(
            new ValueConvertingCache<>(
                new KeyConvertingCache<>(
                    new BytesFileCache(dataFolder),
                    new ReverseConverter<>(new BytesStringConverter())
                ),
                valueConvertJobToBytes
            )
        );

    final Cache<String, Value> cache =
        new FilePersistedExpiringCache<>(
            new FilePersistedMaxSizeCache<>(
                dataFolder,
                fileCache,
                sizeConverter,
                maxSize
            ),
            new KeyConvertingCache<>(
                new BytesFileCache(expiringDataFolder),
                new ReverseConverter<>(new BytesStringConverter())
            ),
            expireTimeout,
            (expireTimeout * 2L)
        );

    return new SynchronizedCache<>(cache);
}
```

---

## Building

```bash
./gradlew build                      # Build all modules and run all tests (Java + native C++)
./gradlew :core:test                 # Run only Java tests
./gradlew :nativelib:runNativeTests  # Run only C++ native tests (requires cmake)
./gradlew :app:assembleDebug         # Build the Android demo app
./gradlew :core:publishToMavenLocal  # Publish core to local Maven repo

# Run a single test class
./gradlew :core:test --tests "BytesFileCacheTest"
./gradlew :core:test --tests "llc.berserkr.cache.BytesFileCacheTest"
```

---

## Exception Hierarchy

All exceptions extend `ResourceException`:

| Exception | Cause |
|---|---|
| `ReadFailure` | Read operation failed |
| `WriteFailure` | Write operation failed |
| `OutOfSpaceException` | Segment file full |
| `NeedsSplitException` | Segment requires splitting |
| `SpaceFragementedException` | File fragmentation |
| `OutOfRetriesException` | Retry limit exceeded |
| `LinearStreamException` | Stream processing error |

---

## Known Limitations

- **Eviction caches have O(N) linked-list traversal** for large key sets; not suitable for caches with millions of entries.
- **No background expiration thread**: `FilePersistedExpiringCache` cleanup is lazy and triggered on access.
- **`FilePersistedMaxSizeStreamCache`** is intentionally non-functional (throws `RuntimeException`) and should not be used.
- Sequential file seeks can be a performance bottleneck under heavy write load.

---

## Requirements

- Java 21+
- Android compatible (minSdk 30 for app, minSdk 33 for nativelib)
- SLF4J (logging facade; bring your own binding)
- CMake 4.2.1+ (for native module, fetches Google Test automatically)

---

## License

See [LICENSE](LICENSE).
