# Java File Cache

A persistent, file-backed cache library written in Java, compatible with Android. Provides a composable, decorator-based cache system with multiple eviction strategies, thread safety options, and flexible key/value type conversions.

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
- **Factory Pattern**: `CacheFactory` provides static factory methods that pre-wire common decorator stacks.
- **Strategy Pattern**: The `Converter` interface abstracts type transformations, allowing any data type to be used as a key or value.

### Thread Safety

Thread safety is provided at multiple levels:

| Mechanism | Granularity | Notes |
|---|---|---|
| `SynchronizedCache` | Coarse (all operations) | `synchronized` methods |
| `CacheLocks` | Per-bucket reader/writer locks | Concurrent reads, exclusive writes |
| `LocalRandomAccess` | Per-thread file handles | `ThreadLocal<RandomAccessFile>` for r/rw modes |
| `ConcurrentHashMap` | Hash lock registry | Thread-safe lock lookup in `StreamingFileHash` |
| `volatile` fields | Counter visibility | `readers`/`writers` in `CacheLocks` |

For most use cases, wrapping your cache in `SynchronizedCache` is sufficient. For high-read-throughput scenarios, the bucket-level `CacheLocks` allow concurrent reads from different buckets.

---

## Installation

### Gradle

```groovy
implementation 'llc.berserkr:java-file-cache:1.0.0'
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

- Java 8+
- Android compatible
- SLF4J (logging facade; bring your own binding)

---

## License

See [LICENSE](LICENSE).
