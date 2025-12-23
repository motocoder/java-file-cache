This project has a file cache written in java (works on android).

The file cache is architected as a hash mapping to a forwardly linked list of segments. 

Everything runs off of two files, the hash has a file where the hash index is the position of the file for the key. Each position has 4 bytes to store an int which works like a pointer into the segmented file.
Inside the segmented file is a bucket of key/value combinations which use hashcode and equals similiar to a java hashmap to deduce equality. 


```Java

/****************************************************************************/

//simplest form handles everything in bytes

final BytesFileCache cache = new BytesFileCache(hashCacheDir);

cache.put("key".getBytes(), "value".getBytes());

final String value = cache.get("key".getBytes());

assertEquals("value", value);

/****************************************************************************/

//cache that marshals key/value strings and has a max size of 5000bytes
final int maxSize = 5000;
final Converter<Integer, String> converter = new ReverseConverter<Integer, String>(new StringSizeConverter());

final Cache<String, String> cache = CacheFactory.getMaxSizeFileCache(maxSize,dataFolder, converter);

/****************************************************************************/

//cache with a max count of 50 items
final File dataFolder = new File(root, "data");
final int maxCount = 50;

final Cache<String, String> cache = CacheFactory.getSerializingMaxCountFileCache(maxCount, dataFolder, (removed) -> {});

/****************************************************************************/

//cache that expires key/value pairs after 2 seconds

final File dataFolder = new File(root, "data");

final int expiringValue = 2000;

final Converter<Integer, String> converter = new ReverseConverter<Integer, String>(
        new StringSizeConverter());
      
final Cache<String, String> cache = CacheFactory.getExpiringFileCache(expiringValue, dataFolder, converter);

/****************************************************************************/

//Combining max size with max count. The other combinations are endless
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
        new SynchronizedCache<> (
            new ValueConvertingCache<> (
                new KeyConvertingCache<>(
                    new BytesFileCache(dataFolder), new ReverseConverter<>(new BytesStringConverter())
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
                new BytesFileCache(expiringDataFolder), new ReverseConverter<>(new BytesStringConverter())
            ),
            expireTimeout,
            (expireTimeout * 2L)
        );

    return new SynchronizedCache<>(cache);

}

```
