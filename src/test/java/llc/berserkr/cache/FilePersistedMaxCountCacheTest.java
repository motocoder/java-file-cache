package llc.berserkr.cache;

import llc.berserkr.cache.converter.BytesStringConverter;
import llc.berserkr.cache.converter.InputStreamConverter;
import llc.berserkr.cache.converter.ReverseConverter;
import llc.berserkr.cache.converter.SerializingConverter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StringUtilities;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class FilePersistedMaxCountCacheTest {

	private static final Logger logger = LoggerFactory.getLogger(FilePersistedMaxCountCacheTest.class);

	private static final byte[] TEN_BYTES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

	static {
		BasicConfigurator.configure();
	}

	boolean removed = false;

	public void removed() {
		removed = true;
	}

	@Test
	public void testFilePersistingCountCacheTest() throws IOException {

		try {

			Random random = new Random();
			String appendix = String.valueOf(Math.abs(random.nextInt()));

			File root = new File("./target/test-files/temp" + appendix + "/");

			deleteRoot(root);

			final File dataFolder = new File(root, "data");

			final File countPersisted = new File(dataFolder, "data/countPersisted/data/data");

			final int maxCount = 50;

			final Consumer<String> call = new Consumer<String>() {

				@Override
				public void accept(String value) {

					removed();
				}

			};

			final Cache<String, String> cache = CacheFactory.getSerializingMaxCountFileCache(
					maxCount, dataFolder, call);

			final String key = "dfslkjasdfkljsadfa";
			final String value = "dfsaoiuwekljfsdfsadlkaioklalkdsf";

			for (int x = 1; x < 10; x++) {

				final String keyRepeated = StringUtilities.repeat(key, x);
				final String valueRepeated = StringUtilities.repeat(value, x);

				cache.put(keyRepeated, valueRepeated);

				Thread.sleep(5);

			}

			// test exists
			for (int x = 1; x < 10; x++) {

				final String keyRepeated = StringUtilities.repeat(key, x);
				final String valueRepeated = StringUtilities.repeat(value, x);

				assertEquals(true, cache.exists(keyRepeated));
				assertEquals(valueRepeated, cache.get(keyRepeated));

			}

			for (int x = 1; x < 1000; x++) {

				final String keyRepeated = String.valueOf(x);

				cache.put(keyRepeated, value);

				if ((x % 100) == 0) {
                    logger.debug("Size of data " + countPersisted.length());
                }

			}

			// test exists
			for (int x = 1; x < 10; x++) {

				final String keyRepeated = StringUtilities.repeat(key, x);
				assertEquals(false, cache.exists(keyRepeated));

			}

			assertEquals(true, removed);

			final String key2 = StringUtilities.repeat(key, 1);

			assertEquals(false, cache.exists(key2));

			deleteRoot(root);

		}
		catch (ResourceException e) {
			e.printStackTrace();
			fail();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testFilePersistingCountCacheMultithreadedTest() throws IOException {

		final Executor exec = Executors.newFixedThreadPool(10);

		try {

			Random random = new Random();
			String appendix = String.valueOf(Math.abs(random.nextInt()));

			File root = new File("./target/test-files/temp" + appendix + "/");

			deleteRoot(root);

			final File dataFolder = new File(root, "data");

			final int maxCount = 50;

			final Consumer<String> call = new Consumer<String>() {

				@Override
				public void accept(String value) {

					removed();
				}

			};

			final Cache<String, String> cache = CacheFactory.getSerializingMaxCountFileCache(maxCount, dataFolder, (removed) -> {});

			final String key = "dfslkjasdfkljsadfa";
			final String value = "dfsaoiuwekljfsdfsadlkaioklalkdsf";

			for (int x = 1; x < 10; x++) {

				final int xfinal = x;

				exec.execute(new Runnable() {

					@Override
					public void run() {

						final String keyRepeated = StringUtilities.repeat(key, xfinal);
						final String valueRepeated = StringUtilities.repeat(value, xfinal);

						try {
							cache.put(keyRepeated, valueRepeated);
						}
						catch (ResourceException e) {
							e.printStackTrace();
						}

					}

				});

			}

			Thread.sleep(3000);

			for (int x = 1; x < 10; x++) {

				final int xfinal = x;

				exec.execute(new Runnable() {

					@Override
					public void run() {

						final String keyRepeated = StringUtilities.repeat(key, xfinal);
						final String valueRepeated = StringUtilities.repeat(value, xfinal);

						try {
							assertEquals(true, cache.exists(keyRepeated));
							assertEquals(valueRepeated, cache.get(keyRepeated));
						}
						catch (ResourceException e) {
							e.printStackTrace();
						}

					}

				});

			}

			for (int x = 500; x < 560; x++) {

				final int xfinal = x;

				exec.execute(new Runnable() {

					@Override
					public void run() {

						final String keyRepeated = StringUtilities.repeat(key, xfinal);

						try {
							cache.put(keyRepeated, value);
						}
						catch (ResourceException e) {
							e.printStackTrace();
						}

					}

				});

			}

			Thread.sleep(10000);

			// test exists
			for (int x = 1; x < 10; x++) {

				final String keyRepeated = StringUtilities.repeat(key, x);
				assertEquals(false, cache.exists(keyRepeated));

			}

			assertEquals(true, removed);

			final String key2 = StringUtilities.repeat(key, 1);

			assertEquals(false, cache.exists(key2));

			deleteRoot(root);

		}
		catch (ResourceException e) {
			e.printStackTrace();
			fail();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void universalTest() throws IOException {

		try {

			Random random = new Random();
			String appendix = String.valueOf(Math.abs(random.nextInt()));

			File root = new File("./target/test-files/temp" + appendix + "/");

			deleteRoot(root);

			final File dataFolder = new File(root, "data");

			final int maxCount = 50;

			final Consumer<String> call = new Consumer<String>() {

				@Override
				public void accept(String value) {

					removed();
				}

			};

			final Cache<String, String> cache = CacheFactory.getSerializingMaxCountFileCache(
					maxCount, dataFolder, call);

			final String key = "dfsa";
			final String value = "dfsadsf";
			final String key2 = "fgdd";
			final String value2 = "dfgsds";
			String returnValue;

			// TEST PUT, GET, REMOVE, and EXISTS

			cache.put(key, value);

			String retVal = cache.get(key);

			assertEquals(value, retVal);
			assertEquals(cache.exists(key), true);

			cache.remove(key);

			assertEquals(cache.exists(key), false);

			// TEST CLEAR, GETALL, and RETEST EXISTS

			List<String> keyList = new ArrayList<String>();

			keyList.add(key);
			keyList.add(key2);

			cache.put(key, value);
			cache.put(key2, value2);

			assertEquals(cache.exists(key), true);
			assertEquals(cache.exists(key2), true);

//			List<String> streamList = cache.getAll(keyList);
//
//			for (int y = 0; y < 1; y++) {
//
//				String ret = streamList.get(y);
//
//				assertEquals(value, ret);
//				assertEquals(cache.exists(key), true);
//
//			}

			cache.clear();

			assertEquals(cache.exists(key), false);
			assertEquals(cache.exists(key2), false);

		}
		catch (ResourceException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testPersistedPart() throws IOException {

		Random random = new Random();
		String appendix = String.valueOf(Math.abs(random.nextInt()));

		File root2 = new File("./target/test-files/temp" + appendix + "/");

		deleteRoot(root2);

		final File dataFolder = new File(root2, "data");
		final File tempFolder = new File(root2, "temp");
		final File persistingFolder = new File(root2, "persisting");

		BytesFileCache diskCache = new BytesFileCache(dataFolder);

        KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));


        Cache<String, String> fileCache = new ValueConvertingCache<String, String, byte[]>(
                keyConvertingCache,
				new SerializingConverter<String>());

		Cache<String, String> cache = new FilePersistedMaxCountCache<String>(persistingFolder,
				fileCache, 2, new Consumer<String>() {

					@Override
					public void accept(String value) {
					}
				});

		try {

			cache.clear();

			final String TEN_BYTES_STRING = new String(TEN_BYTES);

			cache.put("1", TEN_BYTES_STRING);

			Thread.sleep(100);

			cache.put("2", TEN_BYTES_STRING);

			assertNotNull(cache.get("1"));
			assertNotNull(cache.get("2"));

			cache.put("3", TEN_BYTES_STRING);

			Thread.sleep(50);

			cache.put("4", TEN_BYTES_STRING);

			Thread.sleep(50);

			cache.put("5", TEN_BYTES_STRING);

			assertNull(cache.get("1"));
			assertNull(cache.get("2"));
			assertNull(cache.get("3"));
			assertNotNull(cache.get("4"));
			assertNotNull(cache.get("5"));

			cache = null;
			fileCache = null;
			diskCache = null;

			Thread.sleep(500);

			assertNull(cache);

			diskCache = new BytesFileCache(dataFolder);

            keyConvertingCache =
                    new KeyConvertingCache<String, byte[], byte[]>(diskCache, new ReverseConverter<>(new BytesStringConverter()));



            fileCache = new ValueConvertingCache<String, String, byte[]>(
                    keyConvertingCache,
					new SerializingConverter<String>());

			cache = new FilePersistedMaxCountCache<String>(persistingFolder, fileCache, 2,
					new Consumer<String>() {

						@Override
						public void accept(String value) {
						}
					});

			assertTrue(cache.exists("4"));
			assertNotNull(cache.get("5"));

			cache.put("6", TEN_BYTES_STRING);

			Thread.sleep(50);

			cache.put("7", TEN_BYTES_STRING);

			Thread.sleep(50);

			cache.put("8", TEN_BYTES_STRING);

		}
		catch (InterruptedException e1) {
			fail();
		}
		catch (ResourceException e) {
			fail();
		}

		try {

			assertFalse(cache.exists("4"));
			assertNull(cache.get("5"));
			assertNull(cache.get("6"));
			assertNotNull(cache.get("7"));
			assertNotNull(cache.get("8"));

		}
		catch (ResourceException e) {

			fail("cant get here");
			e.printStackTrace();

		}

		deleteRoot(root2);

	}

	void deleteRoot(File root) {

		if (root.exists()) {

			final File[] fileList = root.listFiles();

			if (fileList != null) {

				for (File cacheFile : fileList) {
					cacheFile.delete();
				}

			}

			root.delete();

		}
	}

}
