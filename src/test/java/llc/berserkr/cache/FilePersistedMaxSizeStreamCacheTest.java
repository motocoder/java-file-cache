package llc.berserkr.cache;

import llc.berserkr.cache.util.StringUtilities;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class FilePersistedMaxSizeStreamCacheTest {

//	private static final Logger logger = LoggerFactory.getLogger(FilePersistedMaxSizeStreamCacheTest.class);
//
//	private static final byte[] TEN_BYTES = new byte[10];
//
//	static {
//		BasicConfigurator.configure();
//	}
//
//	@Test
//	public void testFilePersistingStreamCacheTest() {
//
//		try {
//
//			Random random = new Random();
//			String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//			File root = new File("./target/test-files/temp" + appendix + "/");
//
//			deleteRoot(root);
//
//			final File dataFolder = new File(root, "data");
//
//			final File persistingFolderData = new File(dataFolder, "data/sizePersisted/data/data");
//
//			final int maxSize = 4000;
//			final int expires = 10000;
//
//			final Cache<String, InputStream> cache = CacheFactory.getMaxSizeExpiringFileCache(
//					dataFolder, maxSize, expires);
//
//			final String key = "dfslkjasdfkljsadfa";
//			final String value = StringUtilities.repeat("dfsaoiuwekljfsdfsadlkaioklalkdsf", 100);
//
//			final int KEYS = 100;
//
//			for (int x = 0; x < KEYS; x++) {
//
//				final String keyRepeated = String.valueOf(x);
//
//				cache.put(keyRepeated, getInputStreamFromString((value)));
//
//				if ((x % 100) == 0) {
//					logger.debug("Size of data " + persistingFolderData.length());
//				}
//
//			}
//
//			Thread.sleep(expires + 100);
//
//			for (int x = 0; x < KEYS; x++) {
//
//				final String keyRepeated = String.valueOf(x);
//
//				assertEquals(false, cache.exists(keyRepeated));
//
//			}
//
//			final String key2 = StringUtilities.repeat(key, 1);
//
//			assertEquals(false, cache.exists(key2));
//
//			deleteRoot(root);
//
//		}
//		catch (ResourceException e) {
//			e.printStackTrace();
//			fail();
//		}
//		catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	@Test
//	public void testFileMultithreadedTest() {
//
//		final Executor exec = Executors.newFixedThreadPool(10);
//
//		try {
//
//			Random random = new Random();
//			String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//			File root = new File("./target/test-files/temp" + appendix + "/");
//
//			deleteRoot(root);
//
//			final File dataFolder = new File(root, "data");
//
//			final int maxCount = 50;
//			final int expires = 5000;
//
//			final Cache<String, InputStream> cache = CacheFactory.getMaxSizeExpiringFileCache(
//					dataFolder, maxCount, expires);
//
//			final String key = "dfslkjasdfkljsadfa";
//			final String value = "dfsaoiuwekljfsdfsadlkaioklalkdsf";
//
//			for (int x = 0; x < 10; x++) {
//
//				final int xfinal = x;
//
//				exec.execute(new Runnable() {
//
//					@Override
//					public void run() {
//
//						final String keyRepeated = StringUtilities.repeat(key, xfinal);
//						final String valueRepeated = StringUtilities.repeat(value, xfinal);
//
//						try {
//							cache.put(keyRepeated, getInputStreamFromString(valueRepeated));
//						}
//						catch (ResourceException e) {
//							e.printStackTrace();
//						}
//
//					}
//
//				});
//
//			}
//
//			Thread.sleep(3000);
//
//			for (int x = 0; x < 10; x++) {
//
//				final int xfinal = x;
//
//				exec.execute(new Runnable() {
//
//					@Override
//					public void run() {
//
//						final String keyRepeated = StringUtilities.repeat(key, xfinal);
//						final String valueRepeated = StringUtilities.repeat(value, xfinal);
//
//						try {
//							assertEquals(true, cache.exists(keyRepeated));
//							assertEquals(valueRepeated,
//									getStringFromInputStream(cache.get(keyRepeated)));
//						}
//						catch (ResourceException e) {
//							e.printStackTrace();
//						}
//
//					}
//
//				});
//
//			}
//
//			for (int x = 500; x < 560; x++) {
//
//				final int xfinal = x;
//
//				exec.execute(new Runnable() {
//
//					@Override
//					public void run() {
//
//						final String keyRepeated = StringUtilities.repeat(key, xfinal);
//
//						try {
//							cache.put(keyRepeated, getInputStreamFromString(value));
//						}
//						catch (ResourceException e) {
//							e.printStackTrace();
//						}
//
//						if ((xfinal % 100) == 0) {
//							logger.debug("Size of data " + dataFolder.getTotalSpace());
//						}
//
//					}
//
//				});
//
//			}
//
//			Thread.sleep(10000);
//
//			// test exists
//			for (int x = 0; x < 10; x++) {
//
//				final String keyRepeated = StringUtilities.repeat(key, x);
//				assertEquals(false, cache.exists(keyRepeated));
//
//			}
//
//			Thread.sleep(expires + 100);
//
//			final String key2 = StringUtilities.repeat(key, 1);
//			assertEquals(false, cache.exists(key2));
//
//			deleteRoot(root);
//
//		}
//		catch (ResourceException e) {
//			e.printStackTrace();
//			fail();
//		}
//		catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	@Test
//	public void universalTest() {
//
//		try {
//
//			Random random = new Random();
//			String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//			File root = new File("./target/test-files/temp" + appendix + "/");
//
//			deleteRoot(root);
//
//			final File dataFolder = new File(root, "data");
//
//			final int maxCount = 50;
//			final int expires = 5000;
//
//			final Cache<String, InputStream> cache = CacheFactory.getMaxSizeExpiringFileCache(
//					dataFolder, maxCount, expires);
//
//			final String key = "dfsa";
//			final String value = "dfsadsf";
//			final String key2 = "fgdd";
//			final String value2 = "dfgsds";
//
//			// TEST PUT, GET, REMOVE, and EXISTS
//
//			cache.put(key, getInputStreamFromString(value));
//
//			String retVal = getStringFromInputStream(cache.get(key));
//
//			assertEquals(value, retVal);
//			assertEquals(cache.exists(key), true);
//
//			cache.remove(key);
//
//			assertEquals(cache.exists(key), false);
//
//			// TEST CLEAR, GETALL, and RETEST EXISTS
//
//			List<String> keyList = new ArrayList<String>();
//
//			keyList.add(key);
//			keyList.add(key2);
//
//			cache.put(key, getInputStreamFromString(value));
//			cache.put(key2, getInputStreamFromString(value2));
//
//			assertEquals(cache.exists(key), true);
//			assertEquals(cache.exists(key2), true);
//
//			List<InputStream> streamList = cache.getAll(keyList);
//
//			for (int y = 0; y < 1; y++) {
//
//				String ret = getStringFromInputStream(streamList.get(y));
//
//				assertEquals(value, ret);
//				assertEquals(cache.exists(key), true);
//
//			}
//
//			cache.clear();
//
//			assertEquals(cache.exists(key), false);
//			assertEquals(cache.exists(key2), false);
//
//		}
//		catch (ResourceException e) {
//			e.printStackTrace();
//		}
//
//	}
//
//	@Test
//	public void testPersistedPart() {
//
//		Random random = new Random();
//		String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//		File root2 = new File("./target/test-files/temp" + appendix + "/");
//
//		deleteRoot(root2);
//
//		final File dataFolder = new File(root2, "data");
//		final File tempFolder = new File(root2, "temp");
//		final File temp2Folder = new File(root2, "temp2");
//		final File persistingFolder = new File(root2, "persisting");
//
//		FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//		Cache<String, InputStream> fileCache = new SynchronizedCache<String, InputStream>(diskCache);
//
//		Cache<String, InputStream> cache = new FilePersistedMaxSizeStreamCache(persistingFolder,
//				fileCache, 20);
//
//		try {
//
//			cache.clear();
//
//			final String TEN_BYTES_STRING = new String(TEN_BYTES);
//
//			cache.put("1", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(100);
//
//			cache.put("2", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			System.out.println(getStringFromInputStream(cache.get("1")));
//
//			assertNotNull(getStringFromInputStream(cache.get("1")));
//			assertNotNull(getStringFromInputStream(cache.get("2")));
//
//			cache.put("3", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("4", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("5", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			assertNull(getStringFromInputStream(cache.get("1")));
//			assertNull(getStringFromInputStream(cache.get("2")));
//			assertNull(getStringFromInputStream(cache.get("3")));
//			assertNotNull(getStringFromInputStream(cache.get("4")));
//			assertNotNull(getStringFromInputStream(cache.get("5")));
//
//			cache = null;
//			fileCache = null;
//			diskCache = null;
//
//			Thread.sleep(500);
//
//			assertNull(cache);
//
//			final FileHashCache diskCache2 = new FileHashCache(dataFolder, temp2Folder);
//
//			final Cache<String, InputStream> fileCache2 = new SynchronizedCache<String, InputStream>(
//					diskCache2);
//
//			cache = new FilePersistedMaxSizeStreamCache(persistingFolder, fileCache2, 20);
//
//			assertTrue(cache.exists("4"));
//			assertNotNull(getStringFromInputStream(cache.get("5")));
//
//			cache.put("6", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("7", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("8", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//		}
//		catch (InterruptedException e1) {
//			fail();
//		}
//		catch (ResourceException e) {
//			fail();
//		}
//
//		try {
//
//			assertFalse(cache.exists("4"));
//			assertNull(getStringFromInputStream(cache.get("5")));
//			assertNull(getStringFromInputStream(cache.get("6")));
//			assertNotNull(getStringFromInputStream(cache.get("7")));
//			assertNotNull(getStringFromInputStream(cache.get("8")));
//
//		}
//		catch (ResourceException e) {
//
//			fail("cant get here");
//			e.printStackTrace();
//
//		}
//
//		deleteRoot(root2);
//
//	}
//
//	@Test
//	public void testMaxSizePart() {
//
//		Random random = new Random();
//		String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//		File root2 = new File("./target/test-files/temp" + appendix + "/");
//
//		deleteRoot(root2);
//
//		final File dataFolder = new File(root2, "data");
//		final File tempFolder = new File(root2, "temp");
//		final File persistingFolder = new File(root2, "persisting");
//
//		FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//		Cache<String, InputStream> fileCache = new SynchronizedCache<String, InputStream>(diskCache);
//
//		Cache<String, InputStream> cache = new FilePersistedMaxSizeStreamCache(persistingFolder,
//				fileCache, 20);
//
//		try {
//
//			cache.clear();
//
//			final String TEN_BYTES_STRING = new String(TEN_BYTES);
//
//			cache.put("1", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(100);
//
//			cache.put("2", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("3", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("4", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//			Thread.sleep(50);
//
//			cache.put("5", new ByteArrayInputStream(TEN_BYTES_STRING.getBytes()));
//
//		}
//		catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
//		catch (ResourceException e) {
//			e.printStackTrace();
//		}
//
//		try {
//
//			assertFalse(cache.exists("1"));
//			assertNull(cache.get("2"));
//			assertNull(cache.get("3"));
//			assertNotNull(cache.get("4"));
//			assertNotNull(cache.get("5"));
//
//		}
//		catch (ResourceException e) {
//
//			fail("cant get here");
//			e.printStackTrace();
//
//		}
//
//		deleteRoot(root2);
//
//	}
//
//	@Test
//	public void testFilePersistedExpiringCacheTest() {
//
//		try {
//
//			Random random = new Random();
//			String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//			File root = new File("./target/test-files/temp" + appendix + "/");
//
//			deleteRoot(root);
//
//			final File dataFolder = new File(root, "data");
//			final File tempFolder = new File(root, "temp");
//			final File persistingFolder = new File(root, "persisting");
//
//			FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//			Cache<String, InputStream> fileCache = new SynchronizedCache<String, InputStream>(
//					diskCache);
//
//			Cache<String, InputStream> cache = new FilePersistedMaxSizeStreamCache(
//					persistingFolder, fileCache, 150);
//
//			final String key = "dfsa";
//			final String value = "dfsadsf";
//			final String key2 = "fgdd";
//			final String value2 = "dfgsds";
//			InputStream returnValue;
//
//			// TEST PUT, GET, REMOVE, and EXISTS
//
//			cache.put(key, new ByteArrayInputStream(value.getBytes()));
//
//			returnValue = cache.get(key);
//
//			assertEquals(value, getStringFromInputStream(returnValue));
//			assertEquals(cache.exists(key), true);
//
//			cache.remove(key);
//
//			assertEquals(cache.exists(key), false);
//
//			// TEST CLEAR, GETALL, and RETEST EXISTS AND GET
//
//			List<String> keyList = new ArrayList<String>();
//
//			keyList.add(key);
//			keyList.add(key2);
//
//			cache.put(key, new ByteArrayInputStream(value.getBytes()));
//			cache.put(key2, new ByteArrayInputStream(value2.getBytes()));
//			cache.put(key2, new ByteArrayInputStream(value2.getBytes())); //repeat to test the automatic remove() when duplicate
//
//			assertEquals(cache.exists(key), true);
//			assertEquals(cache.exists(key2), true);
//
//			List<InputStream> stringList = cache.getAll(keyList);
//
//			for (int y = 0; y < 1; y++) {
//
//				returnValue = stringList.get(y);
//
//				assertEquals(value, getStringFromInputStream(returnValue));
//				assertEquals(cache.exists(key), true);
//
//			}
//
//			cache.clear();
//
//			assertEquals(cache.exists(key), false);
//			assertEquals(cache.get(key), null);
//
//			deleteRoot(root);
//
//		}
//		catch (ResourceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	@Test
//	public void negativeSizeTest() {
//
//		try {
//
//			Random random = new Random();
//			String appendix = String.valueOf(Math.abs(random.nextInt()));
//
//			File root = new File("./target/test-files/temp" + appendix + "/");
//
//			deleteRoot(root);
//
//			final Converter<Integer, String> converter = new ReverseConverter<Integer, String>(
//					new StringSizeConverter());
//
//			final File dataFolder = new File(root, "data");
//			final File tempFolder = new File(root, "temp");
//			final File persistingFolder = new File(root, "persisting");
//
//			FileHashCache diskCache = new FileHashCache(dataFolder, tempFolder);
//
//			Cache<String, InputStream> fileCache = new SynchronizedCache<String, InputStream>(
//					diskCache);
//
//			Cache<String, InputStream> cache = new FilePersistedMaxSizeStreamCache(
//					persistingFolder, fileCache, 150);
//
//			String key = "dfsa";
//			final String key1 = "dfewfawfsdsfsdfadsadsfvsa";
//			final String key2 = "dfsewr34fara";
//			final String value2 = "dsfaskdfaskfjhasjkdfhaskfjldhaskfjhaskjfhkashdfkasjhdfkahsdfkljhs";
//			final String key4 = "9un98q5n3miodfsa";
//			final String value = "qv54v3ckljhdsfoyh43ods";
//			InputStream returnValue;
//
//			cache.put(key, new ByteArrayInputStream(value.getBytes()));
//			cache.put(key1, new ByteArrayInputStream(value2.getBytes()));
//			cache.put(key2, new ByteArrayInputStream(value2.getBytes()));
//			cache.put(key4, new ByteArrayInputStream(value.getBytes()));
//
//			returnValue = cache.get(key4);
//
//			assertEquals(value, getStringFromInputStream(returnValue));
//			assertEquals(cache.exists(key4), true);
//
//			cache.remove(key4);
//
//			assertEquals(cache.exists(key4), false);
//
//			deleteRoot(root);
//
//		}
//		catch (ResourceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}
//
//	void deleteRoot(File root) {
//
//		if (root.exists()) {
//
//			final File[] fileList = root.listFiles();
//
//			if (fileList != null) {
//
//				for (File cacheFile : fileList) {
//					cacheFile.delete();
//				}
//
//			}
//
//			root.delete();
//
//		}
//	}
//
//	private static ByteArrayInputStream getInputStreamFromString(String str) {
//		return new ByteArrayInputStream(str.getBytes());
//	}
//
//	// convert InputStream to String
//	private static String getStringFromInputStream(InputStream is) {
//
//		if (is == null) {
//			return null;
//			//throw new NullPointerException("InputStream is null");
//		}
//
//		BufferedReader br = null;
//		StringBuilder sb = new StringBuilder();
//
//		String line;
//		try {
//
//			br = new BufferedReader(new InputStreamReader(is));
//			while ((line = br.readLine()) != null) {
//				sb.append(line);
//			}
//
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//		}
//		finally {
//			if (br != null) {
//				try {
//					br.close();
//				}
//				catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		String retString = sb.toString();
//
//		if (retString == "") {
//			return null;
//		}
//		else {
//			return retString;
//		}
//
//	}//todo stuff all broken i dun wanna fix it

}
