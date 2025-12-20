package llc.berserkr.cache;

import llc.berserkr.cache.converter.*;
import llc.berserkr.cache.exception.ResourceException;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class CacheVaryingConfigurationTest {
//
//	private static final Logger logger = LoggerFactory.getLogger(FilePersistedMaxSizeCacheTest.class);
//
//	private static final byte [] TEN_BYTES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
//
//	static {
//	    BasicConfigurator.configure();
//	}
//
//	@Test
//	public void testStackedMaxSizeInExpiringCaches() throws IOException {
//
//		final int maxSize = 180;
//		final int expireTimeout = 2000;
//
//		final File cacheRoot = new File("./target/test-files/temp2/");
//
//		deleteRoot(cacheRoot);
//
//		final File dataFolder = new File(cacheRoot, "data");
//        final File tempFolder = new File(cacheRoot, "temp");
//
//        final File expiringRoot = new File(cacheRoot, "expiringRoot");
//
//        final File expiringDataFolder = new File(expiringRoot, "data");
//        final File expiringTempFolder = new File(expiringRoot, "temp");
//
//        FileHashCache diskCache = new FileHashCache(dataFolder);
//
//        KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
//                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//        ValueConvertingCache<String, String, byte[]> fileCache =
//            new ValueConvertingCache<String, String, byte []>(
//                    keyConvertingCache,
//                    new SerializingConverter<String>()
//                );
//
//        FileHashCache expringPersistDiskCache = new FileHashCache(expiringDataFolder);
//
//        KeyConvertingCache<String, byte [], byte []> expiringKeyConvertingCache =
//                new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//
//        Converter<Integer, String> converter = new ReverseConverter<Integer, String>(new StringSizeConverter());
//
//        Cache<String, String> cache =
//            new FilePersistedExpiringCache<String>(
//                new FilePersistedMaxSizeCache<String>(
//                    dataFolder,
//                    fileCache,
//                    converter,
//                    maxSize
//                ),
//                expiringKeyConvertingCache,
//                (long)expireTimeout,
//                (long)(expireTimeout * 2)
//            );
//
//        try {
//
//        	final String TEN_BYTES_STRING = new String(TEN_BYTES);
//
//			cache.put("1", TEN_BYTES_STRING);
//
//			Thread.sleep(100);
//
//			cache.put("2", TEN_BYTES_STRING);
//
//			assertNotNull(cache.get("1"));
//			assertNotNull(cache.get("2"));
//
//			cache.put("3", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("4", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("5", TEN_BYTES_STRING);
//
//			assertNull(cache.get("1"));
//			assertNull(cache.get("2"));
//			assertNull(cache.get("3"));
//			assertNotNull(cache.get("4"));
//			assertNotNull(cache.get("5"));
//
//			cache = null;
//			fileCache = null;
//			diskCache = null;
//
//			Thread.sleep(500);
//
//			assertNull(cache);
//
//			diskCache = new FileHashCache(dataFolder);
//
//            keyConvertingCache =
//                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//	        fileCache =
//	            new ValueConvertingCache<String, String, byte []>(
//                        keyConvertingCache,
//	                    new SerializingConverter<String>()
//	                );
//
//	        expringPersistDiskCache = new FileHashCache(expiringDataFolder);
//            expiringKeyConvertingCache =
//                    new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//	        converter = new ReverseConverter<Integer, String>(new StringSizeConverter());
//
//	        cache =
//	            new FilePersistedExpiringCache<String>(
//	                new FilePersistedMaxSizeCache<String>(
//	                    dataFolder,
//	                    fileCache,
//	                    converter,
//	                    maxSize
//	                ),
//                    expiringKeyConvertingCache,
//	                (long)expireTimeout,
//	                (long)(expireTimeout * 2)
//	            );
//
//			assertTrue(cache.exists("4"));
//			assertNotNull(cache.get("5"));
//
//			cache.put("6", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("7", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("8", TEN_BYTES_STRING);
//
//		}
//		catch (InterruptedException e1) {
//			fail();
//		}
//		catch (ResourceException e) {
//		    fail();
//		}
//
//        try {
//
//			assertFalse(cache.exists("4"));
//			assertNull(cache.get("5"));
//			assertNull(cache.get("6"));
//			assertNotNull(cache.get("7"));
//			assertNotNull(cache.get("8"));
//
//			Thread.sleep(expireTimeout);
//
//			assertNull(cache.get("8"));
//
//			cache.clear();
//
//
//		}
//		catch (ResourceException e) {
//
//			fail("cant get here");
//			e.printStackTrace();
//
//		}
//		catch (InterruptedException e) {
//
//			fail("cant get here");
//			e.printStackTrace();
//
//		}
//
//		cache = null;
//
//		deleteRoot(cacheRoot);
//
//	}
//
//	@Test
//	public void testStackedExpiringInMaxSizeCaches() throws IOException {
//
//		final int maxSize = 180;
//		final int expireTimeout = 2000;
//
//		final File cacheRoot = new File("./target/test-files/temp3/");
//
//		deleteRoot(cacheRoot);
//
//		final File dataFolder = new File(cacheRoot, "data");
//
//        final File expiringRoot = new File(cacheRoot, "expiringRoot");
//
//        final File expiringDataFolder = new File(expiringRoot, "data");
//
//        FileHashCache diskCache = new FileHashCache(dataFolder);
//        KeyConvertingCache<String, byte[], byte[]> keyConvertingCache = new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//        ValueConvertingCache<String, String, byte[]> fileCache =
//            new ValueConvertingCache<String, String, byte []>(
//                    keyConvertingCache,
//                    new SerializingConverter<String>()
//                );
//
//        FileHashCache expringPersistDiskCache = new FileHashCache(expiringDataFolder);
//        KeyConvertingCache<String, byte[], byte[]> expiringKeyConvertingCache = new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//        Converter<Integer, String> converter = new ReverseConverter<Integer, String>(new StringSizeConverter());
//
//        Cache<String, String> cache =
//        		new FilePersistedMaxSizeCache<String>(
//                        dataFolder,
//                        new FilePersistedExpiringCache<String>(
//                        		fileCache,
//                                expiringKeyConvertingCache,
//                                (long)expireTimeout,
//                                (long)(expireTimeout * 2)
//                            ),
//                        converter,
//                        maxSize
//                    );
//
//        try {
//
//        	final String TEN_BYTES_STRING = new String(TEN_BYTES);
//
//			cache.put("1", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("2", TEN_BYTES_STRING);
//
//			assertNotNull(cache.get("1"));
//			assertNotNull(cache.get("2"));
//
//			cache.put("3", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("4", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("5", TEN_BYTES_STRING);
//
//			assertNull(cache.get("1"));
//			assertNull(cache.get("2"));
//			assertNull(cache.get("3"));
//			assertNotNull(cache.get("4"));
//			assertNotNull(cache.get("5"));
//
//			cache = null;
//			fileCache = null;
//			diskCache = null;
//
//			Thread.sleep(500);
//
//			assertNull(cache);
//
//			diskCache = new FileHashCache(dataFolder);
//            keyConvertingCache = new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//	        fileCache =
//	            new ValueConvertingCache<String, String, byte []>(
//                        keyConvertingCache,
//	                    new SerializingConverter<String>()
//	                );
//
//	        expringPersistDiskCache = new FileHashCache(expiringDataFolder);
//            expiringKeyConvertingCache = new KeyConvertingCache<String, byte[], byte[]>(expringPersistDiskCache, new ReverseConverter<>(new BytesStringConverter()));
//
//	        converter = new ReverseConverter<Integer, String>(new StringSizeConverter());
//
//	        cache =
//	            new FilePersistedExpiringCache<String>(
//	                new FilePersistedMaxSizeCache<String>(
//	                    dataFolder,
//	                    fileCache,
//	                    converter,
//	                    maxSize
//	                ),
//                    expiringKeyConvertingCache,
//	                (long)expireTimeout,
//	                (long)(expireTimeout * 2)
//	            );
//
//			assertTrue(cache.exists("4"));
//			assertNotNull(cache.get("5"));
//
//			cache.put("6", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("7", TEN_BYTES_STRING);
//
//			Thread.sleep(50);
//
//			cache.put("8", TEN_BYTES_STRING);
//
//		}
//		catch (InterruptedException e1) {
//			fail();
//		}
//		catch (ResourceException e) {
//		    fail();
//		}
//
//		try {
//
//			assertFalse(cache.exists("4"));
//			assertNull(cache.get("5"));
//			assertNull(cache.get("6"));
//			assertNotNull(cache.get("7"));
//			assertNotNull(cache.get("8"));
//
//			Thread.sleep(expireTimeout);
//
//			assertNull(cache.get("8"));
//
//			cache.clear();
//
//		}
//		catch (ResourceException e) {
//
//			fail("cant get here");
//			e.printStackTrace();
//
//		}
//		catch (InterruptedException e) {
//
//			fail("cant get here");
//			e.printStackTrace();
//
//		}
//
//		cache = null;
//
//		deleteRoot(cacheRoot);
//
//	}
//
//
//	void deleteRoot (File root) {
//        if (root.exists()) {
//
//            if(root.listFiles() != null) {
//                for (File cacheFile: root.listFiles()){
//                    cacheFile.delete();
//                }
//            }
//            root.delete();
//        }
//    }
	
}
