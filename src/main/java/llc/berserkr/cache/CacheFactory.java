package llc.berserkr.cache;

import llc.berserkr.cache.converter.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.function.Consumer;

import llc.berserkr.cache.converter.BytesStringConverter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.provider.*;

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
        
        final File expiringRoot = new File(cacheRoot, "expiringRoot");
        
        final File expiringDataFolder = new File(expiringRoot, "data");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
            new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));


        final ValueConvertingCache<String, Value, byte[]> fileCache = 
            new ValueConvertingCache<>(
                keyConvertingCache,
                new SerializingConverter<>()
            );

        final FileHashCache expringPersistDiskCache = new FileHashCache(expiringDataFolder);

        final KeyConvertingCache<String, byte [], byte []> expiringkeyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));
            
        final Cache<String, Value> cache = 
            new FilePersistedExpiringCache<Value>(                    
                new FilePersistedMaxSizeCache<Value>(
                    dataFolder,
                    fileCache,
                    sizeConverter,
                    maxSize
                ),
                expiringkeyConvertingCache,
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
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
        
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
    
    /**
     * 
     * @param maxSize
     * @param cacheRoot - must be unique to this cache. Can not be any other cache's root directory.
     * @param sizeConverter
     * @return
     * 
     * example - CacheFactory.getMaxSizeFileCache(DataSizes.ONE_MB, propsFile, new FixedSizeConverter<Serializable>(1024));
     * 
     */
    public static final <Value> Cache<String, Value> getMaxSizeFileCache(
    	final long maxSize,
        final File cacheRoot,
        final Converter<Integer, Value> sizeConverter
    ) throws IOException {
        
        final File dataFolder = new File(cacheRoot, "data");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        final ValueConvertingCache<String, Value, byte[]> fileCache = 
            new ValueConvertingCache<String, Value, byte []>(
                    keyConvertingCache,
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
        final File tempFolder = new File(cacheRoot, "temp");
        
        final File expiringRoot = new File(cacheRoot, "expiringRoot");
        
        final File expiringDataFolder = new File(expiringRoot, "data");
        final File expiringTempFolder = new File(expiringRoot, "temp");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
        
        final ValueConvertingCache<String, Value, byte[]> fileCache = 
            new ValueConvertingCache<String, Value, byte []>(
                    keyConvertingCache,
                    new SerializingConverter<Value>()
                );
            
        final FileHashCache expringPersistDiskCache = new FileHashCache(expiringDataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache2 =
                new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));

        final Cache<String, Value> cache = 
            new FilePersistedExpiringCache<Value>(                    
                fileCache,
                keyConvertingCache2,
                expireTimeout,
                expireTimeout
            );
        
        return new SynchronizedCache<String, Value>(cache);
        
    }
    
    /**
     *
     * @param cacheRoot - must be unique to this cache. Can not be any other cache's root directory.
     * @return
     */
    public static final <Value> Cache<String, Value> getHashCashFileCache(
        final File cacheRoot
    ) throws IOException {
        
        final File dataFolder = new File(cacheRoot, "data");

        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        final ValueConvertingCache<String, Value, byte[]> fileCache = 
            new ValueConvertingCache<String, Value, byte []>(
                    keyConvertingCache,
                    new SerializingConverter<Value>()
                );
        
        return new SynchronizedCache<String, Value>(fileCache);
        
    }
    
    /**
     * CacheFactory.getStreamHashCacheFileCache(musicFolder);
     * 
     * @param cacheRoot
     * @return
     */
    public static Cache<String, byte []> getStreamHashCacheFileCache(
        final File cacheRoot
    ) throws IOException {
        
        final File dataFolder = new File(cacheRoot, "data");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
        
        return new SynchronizedCache<String, byte []>(keyConvertingCache);
        
    }
    
    public static final <Value> Cache<String, Value> getHashCashFileCache(
        final File cacheRoot,
        final Converter<byte[], InputStream> valueConvertBytesToStream2,
        final Converter<Value, byte[]> valueConvertListToBytes
    ) throws IOException {
		
		final File dataFolder = new File(cacheRoot, "data");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        return new SynchronizedCache<String, Value>(
                new ValueConvertingCache<String, Value, byte[]>(
                        keyConvertingCache,
                        valueConvertListToBytes
                )
        );
        
	}
    
    public static final <Value> Cache<String, Value> getHashCashFileCache(
            final File cacheRoot,
            final Converter<Value, byte[]> valueConvertListToBytes
    ) throws IOException {
        
        final File dataFolder = new File(cacheRoot, "data");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        return new SynchronizedCache<String, Value>(
            new ValueConvertingCache<String, Value, byte[]>(
                keyConvertingCache,
                valueConvertListToBytes
            )
        );
        
    }


	
	public static final <Value> Cache<String, Value> getMaxSizeExpiringFileCache(
        final File cacheRoot,
        final int maxSize,
        final int expireTimeout,
        final Converter<Integer, Value> sizeConverter, 
        final Converter<byte[], InputStream> valueConvertBytesToStream,
        final Converter<Value, byte[]> valueConvertJobToBytes
    ) throws IOException {
        
        final File dataFolder = new File(cacheRoot, "data");
        final File tempFolder = new File(cacheRoot, "temp");
        
        final File expiringRoot = new File(cacheRoot, "expiringRoot");
        
        final File expiringDataFolder = new File(expiringRoot, "data");
        final File expiringTempFolder = new File(expiringRoot, "temp");
        
        final FileHashCache diskCache = new FileHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
        
        final Cache<String, Value> fileCache = 
            new SynchronizedCache<String, Value> (
                    new ValueConvertingCache<String, Value, byte[]> (
                        keyConvertingCache,
                        valueConvertJobToBytes
                    )
                
            );
            
        final FileHashCache expringPersistDiskCache = new FileHashCache(expiringDataFolder);

        final KeyConvertingCache<String, byte [], byte []> expiringKeyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));
            
        final Cache<String, Value> cache = 
            new FilePersistedExpiringCache<Value>(     
                new FilePersistedMaxSizeCache<Value>(
                    dataFolder,
                    fileCache,
                    sizeConverter,
                    maxSize
                ),
                expiringKeyConvertingCache,
                (long)expireTimeout,
                (long)(expireTimeout * 2)
            );
        
        return new SynchronizedCache<String, Value>(cache);
                
    }



}
