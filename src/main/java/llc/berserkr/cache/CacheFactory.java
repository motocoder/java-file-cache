package llc.berserkr.cache;

import llc.berserkr.cache.converter.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import llc.berserkr.cache.converter.BytesStringConverter;

public class CacheFactory {
    
    /**
     * 
     * @param maxSize
     * @param expireTimeout
     * @param cacheRoot - must be unique to this cache. Can not be any other cache's root directory.
     * @param sizeConverter
     * @return
     */
    public static final <Value> Cache<String, Value> getSerializingFileCache(
            final int maxSize,
            final int expireTimeout,
            final File cacheRoot,
            final Converter<Integer, Value> sizeConverter
    ) throws IOException {

        final File dataFolder = new File(cacheRoot, "data");
        final File tempFolder = new File(cacheRoot, "temp");

        final File expiringRoot = new File(cacheRoot, "expiringRoot");

        final File expiringDataFolder = new File(expiringRoot, "data");
        final File expiringTempFolder = new File(expiringRoot, "temp");

        final StreamFileCache diskCache = new StreamFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                new KeyConvertingCache<>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        final ValueConvertingCache<String, Value, byte[]> fileCache =
            new ValueConvertingCache<String, Value, byte []>(
                    new ValueConvertingCache<String, byte [], InputStream>(
                            keyConvertingCache,
                            new ReverseConverter<byte [], InputStream>(new InputStreamConverter())
                    ),
                    new SerializingConverter<Value>()
            );

        final StreamFileCache diskCache2 = new StreamFileCache(expiringTempFolder);

        final KeyConvertingCache<String, byte [], InputStream> expringPersistDiskCache =
                new KeyConvertingCache<>(diskCache2, new ReverseConverter<>(new BytesStringConverter()));

        final Cache<String, Value> cache =
                new FilePersistedExpiringCache<Value>(
                        new FilePersistedMaxSizeCache<Value>(
                                dataFolder,
                                fileCache,
                                sizeConverter,
                                maxSize
                        ),
                    new ValueConvertingCache<>(expringPersistDiskCache, new ReverseConverter<>(new InputStreamConverter())),
                        (long)expireTimeout,
                        (long)(expireTimeout * 2)
                );

        return new SynchronizedCache<String, Value>(cache);

    }

    public static final <Value> Cache<String, Value> getSerializingMaxCountFileCache(
        final int maxCount,
        final File cacheRoot,
        final Consumer<Value> onRemoved
    ) throws IOException {

        final File dataFolder = new File(cacheRoot, "data");

        final BytesFileCache diskCache = new BytesFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        final ValueConvertingCache<String, Value, byte[]> fileCache =
            new ValueConvertingCache<String, Value, byte []>(
                keyConvertingCache,
                new SerializingConverter<Value>()
            );

        final Cache<String, Value> cache =
            new FilePersistedMaxCountCache<Value>(
                dataFolder,
                fileCache,
                maxCount,
                onRemoved
            );

        return new SynchronizedCache<String, Value>(cache);

    }
//
//    /**
//     * example - (FifoCache<DownloadingJob>)CacheFactory.getFIFOCache(DownloadingJob.class, rootFile)
//     *
//     * @param returnType - the generic type of the return fifo cache
//     * @param cacheRoot
//     * @return
//     */
//    public static <Job> FifoCache<Job> getFIFOCache(final String queueKey, final Class<Job> returnType, File cacheRoot) {
//
////        final File cacheDir = FileNames.getDir(FileNames.IMPRESSION_JOBS, context, Context.MODE_APPEND);
//
//        final File cacheDir = new File(cacheRoot, "file-cache");
//
//        final Converter<Job, byte[]> valueConvertJobToBytes = new SerializingConverter<Job>();
//        final Converter<byte[], InputStream> valueConvertBytesToStream = new ReverseConverter<byte[], InputStream> (new InputStreamConverter());
//        final Converter<Long, String> keyConvertLongToString = new LongStringConverter();
//
//        final Cache<Long, Job> converted = CacheFactory.getHashCashFileCacheLong(cacheDir, valueConvertBytesToStream, keyConvertLongToString, valueConvertJobToBytes);
//
//
////        final File listCacheDir = FileNames.getDir(FileNames.IMPRESSION_LIST, context, Context.MODE_APPEND);
//
//        final Converter<LinkedList<Long>, byte[]> valueConvertListToBytes = new SerializingConverter<LinkedList<Long>>();
//        final Converter<byte[], InputStream> valueConvertBytesToStream2 = new ReverseConverter<byte[], InputStream> (new InputStreamConverter());
//
//        final File listCacheDir = new File(cacheRoot, "fifo-list");
//
//        final Cache<String, LinkedList<Long>> listCache = CacheFactory.getHashCashFileCache(listCacheDir, valueConvertBytesToStream2, valueConvertListToBytes);
//
//
//        final PushProvider<LinkedList<Long>> provider;
//
//        {
//
//            final ResourceProvider<LinkedList<Long>> rootProvider =
//                    new DefaultResourceProvider<LinkedList<Long>>() {
//
//                        @Override
//                        public LinkedList<Long> provide() throws ResourceException {
//                            return new LinkedList<Long>();
//                        }
//
//                    };
//
//            provider = new SynchronizedPushProvider<LinkedList<Long>>(
//                    new CachedPushProvider<LinkedList<Long>>(queueKey, listCache, rootProvider)
//            );
//
//        }
//
//
//        return new FifoCache<Job>(provider, converted);
//
//    }
//
//    /**
//     *
//     * @param maxSize
//     * @param cacheRoot - must be unique to this cache. Can not be any other cache's root directory.
//     * @param sizeConverter
//     * @return
//     *
//     * example - CacheFactory.getMaxSizeFileCache(DataSizes.ONE_MB, propsFile, new FixedSizeConverter<Serializable>(1024));
//     *
//     */
    public static final <Value> Cache<String, Value> getMaxSizeFileCache(
            final long maxSize,
            final File cacheRoot,
            final Converter<Integer, Value> sizeConverter
    ) throws IOException {

        final File dataFolder = new File(cacheRoot, "data");

        final StreamFileCache diskCache = new StreamFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
            new KeyConvertingCache<String, byte[], InputStream>(
                diskCache, new ReverseConverter<>(new BytesStringConverter())
            );

        final ValueConvertingCache<String, Value, byte[]> fileCache =
            new ValueConvertingCache<String, Value, byte []>(
                new ValueConvertingCache<String, byte [], InputStream>(
                    keyConvertingCache,
                    new ReverseConverter<byte [], InputStream>(new InputStreamConverter())
                ),
                new SerializingConverter<Value>()
            );

        final Cache<String, Value> cache =
                new FilePersistedMaxSizeCache<Value>(
                        dataFolder,
                        fileCache,
                        sizeConverter,
                        maxSize
                );

        return new SynchronizedCache<String, Value>(cache);

    }

    /**
     *
     * @param expireTimeout
     * @param cacheRoot - must be unique to this cache. Can not be any other cache's root directory.
     * @param sizeConverter
     * @return
     */
    public static final <Value> Cache<String, Value> getExpiringFileCache(
        final long expireTimeout,
        final File cacheRoot,
        final Converter<Integer, Value> sizeConverter
    ) throws IOException {

        final File dataFolder = new File(cacheRoot, "data");

        final File expiringRoot = new File(cacheRoot, "expiringRoot");

        final File expiringDataFolder = new File(expiringRoot, "data");

        final BytesFileCache diskCache = new BytesFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
            new KeyConvertingCache<String, byte[], byte []>(
                diskCache, new ReverseConverter<>(new BytesStringConverter())
            );

        final ValueConvertingCache<String, Value, byte[]> fileCache =
            new ValueConvertingCache<String, Value, byte []>(
                keyConvertingCache,
                new SerializingConverter<Value>()
            );

        final BytesFileCache expringPersistDiskCache = new BytesFileCache(expiringDataFolder);
        final KeyConvertingCache<String, byte [], byte []> keyConvertingExpiringCache =
            new KeyConvertingCache<String, byte[], byte []>(
                expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter())
            );

        final Cache<String, Value> cache =
            new FilePersistedExpiringCache<Value>(
                fileCache,
                keyConvertingExpiringCache,
                expireTimeout,
                expireTimeout
            );

        return new SynchronizedCache<String, Value>(cache);

    }
//
//    /**
//     *
//     * @param cacheRoot - must be unique to this cache. Can not be any other cache's root directory.
//     * @return
//     */
//    public static final <Value> Cache<String, Value> getHashCashFileCache(
//            final File cacheRoot
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        final ValueConvertingCache<String, Value, byte[]> fileCache =
//                new ValueConvertingCache<String, Value, byte []>(
//                        new ValueConvertingCache<String, byte [], InputStream>(
//                                diskCache,
//                                new ReverseConverter<byte [], InputStream>(new InputStreamConverter())
//                        ),
//                        new SerializingConverter<Value>()
//                );
//
//        return new SynchronizedCache<String, Value>(fileCache);
//
//    }
//
//    /**
//     * CacheFactory.getStreamHashCacheFileCache(musicFolder);
//     *
//     * @param cacheRoot
//     * @return
//     */
//    public static final Cache<String, InputStream> getStreamHashCacheFileCache(
//            final File cacheRoot
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        return new SynchronizedCache<String, InputStream>(diskCache);
//
//    }
//
//    public static final Cache<String, InputStream> getMaxSizeStreamingFileCache(
//            final File cacheRoot,
//            final int maxSize
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        final Cache<String, InputStream> fileCache =
//                new SynchronizedCache<String, InputStream> (
//                        diskCache
//                );
//        final Cache<String, InputStream> cache =
//                new FilePersistedMaxSizeStreamCache(
//                        dataFolder,
//                        fileCache,
//                        maxSize
//                );
//
//        return new SynchronizedCache<String, InputStream>(cache);
//
//    }
//
//
//    public static final <Value> Cache<String, Value> getHashCashFileCache(
//            final File cacheRoot,
//            final Converter<byte[], InputStream> valueConvertBytesToStream2,
//            final Converter<Value, byte[]> valueConvertListToBytes
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        final Cache<String, Value> listCache =
//                new SynchronizedCache<String, Value>(
//                        new ValueConvertingCache<String, Value, byte[]>(
//                                new ValueConvertingCache<String, byte[], InputStream>(
//                                        new KeyEncodingCache<InputStream>(
//                                                diskCache
//                                        ),
//                                        valueConvertBytesToStream2
//                                ),
//                                valueConvertListToBytes
//                        )
//                );
//
//        return listCache;
//
//    }
//
//    public static final <Value> Cache<String, Value> getHashCashFileCache(
//            final File cacheRoot,
//            final Converter<Value, byte[]> valueConvertListToBytes
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        final Cache<String, Value> listCache =
//                new SynchronizedCache<String, Value>(
//                        new ValueConvertingCache<String, Value, byte[]>(
//                                new ValueConvertingCache<String, byte[], InputStream>(
//                                        new KeyEncodingCache<InputStream>(
//                                                diskCache
//                                        ),
//                                        new ReverseConverter<byte [], InputStream>(new InputStreamConverter())
//                                ),
//                                valueConvertListToBytes
//                        )
//                );
//
//        return listCache;
//
//    }
//
//    public static final <Value> Cache<String, Value> getHashCashFileCacheStream(
//            final File cacheRoot,
//            final Converter<Value, InputStream> valueConvertListToBytes
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        final Cache<String, Value> listCache =
//                new SynchronizedCache<String, Value>(
//                        new ValueConvertingCache<String, Value, InputStream>(
//                                new KeyEncodingCache<InputStream>(
//                                        diskCache
//                                ),
//                                valueConvertListToBytes
//                        )
//                );
//
//        return listCache;
//
//    }
//
//
//    public static final <Value> Cache<Long, Value> getHashCashFileCacheLong(
//            final File cacheRoot,
//            final Converter<byte[], InputStream> valueConvertBytesToStream,
//            final Converter<Long, String> keyConvertLongToString,
//            final Converter<Value, byte[]> valueConvertJobToBytes
//    ) {
//
//        final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//        final Cache<Long, Value> converted =
//                new SynchronizedCache<Long, Value> (
//                        new KeyConvertingCache<Long, String, Value> (
//                                new ValueConvertingCache<String, Value, byte[]> (
//                                        new ValueConvertingCache<String, byte[], InputStream> (
//                                                diskCache,
//                                                valueConvertBytesToStream
//                                        ),
//                                        valueConvertJobToBytes
//                                ),
//                                keyConvertLongToString
//                        )
//                );
//
//        return converted;
//
//    }
//
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

    /**
     *
     * CacheFactory.getMaxSizeExpiringFileCache(propsFile, DataSizes.FIVE_MB, (int)ONE_DAY);
     *
     * @param cacheRoot
     * @param maxSize
     * @param expireTimeout
     * @return
     */
    public static final Cache<String, InputStream> getMaxSizeExpiringFileCache(
        final File cacheRoot,
        final int maxSize,
        final int expireTimeout
    ) throws IOException {

        final File dataFolder = new File(cacheRoot, "data");
        final File expiringRoot = new File(cacheRoot, "expiringRoot");
        final File expiringDataFolder = new File(expiringRoot, "data");

        final StreamFileCache diskCache = new StreamFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
            new KeyConvertingCache<String, byte[], InputStream>(
                diskCache, new ReverseConverter<>(new BytesStringConverter())
            );

        final Cache<String, InputStream> fileCache =
            new SynchronizedCache<String, InputStream> (
                keyConvertingCache
            );

        final BytesFileCache expringPersistDiskCache = new BytesFileCache(expiringDataFolder);
        final KeyConvertingCache<String, byte [], byte []> keyConvertingExpiringCache =
                new KeyConvertingCache<String, byte[], byte []>(
                        expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter())
                );

        final Cache<String, InputStream> cache =
                new FilePersistedExpiringCache<InputStream>(
                        new FilePersistedMaxSizeStreamCache(
                                dataFolder,
                                fileCache,
                                maxSize
                        ),
                        keyConvertingExpiringCache,
                        (long)expireTimeout,
                        (long)(expireTimeout * 2)
                );

        return new SynchronizedCache<String, InputStream>(cache);

    }



}
