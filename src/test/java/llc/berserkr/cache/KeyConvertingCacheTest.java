package llc.berserkr.cache;

import llc.berserkr.cache.converter.Converter;
import llc.berserkr.cache.converter.InputStreamConverter;
import llc.berserkr.cache.converter.ReverseConverter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.hash.SegmentedBytesDataManager;
import llc.berserkr.cache.loader.DefaultResourceLoader;
import llc.berserkr.cache.loader.ResourceLoader;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static llc.berserkr.cache.hash.SegmentedStreamingFile.bytesToLong;
import static llc.berserkr.cache.hash.SegmentedStreamingFile.longToByteArray;
import static org.junit.jupiter.api.Assertions.*;

public class KeyConvertingCacheTest {

	private static ResourceLoader<String, byte[]> resourceLoader;
	private static KeyConvertingCache<Long, String, byte[]> testCacheWithResourceLoader;

	private static File rootDir = new File("./target/test-files/test1/");
    private static File segments = new File(rootDir, "segments");
	private static KeyConvertingCache<Long, byte [], byte[]> testCacheWithFileCache;

	private final static String TEST_VAL1 = "Test Value 1";
	private final static String TEST_VAL2 = "Test Value 2";
    private static BytesFileCache fileCache;

    @BeforeAll
	public static void setUpBeforeClass() throws Exception {

		LongStringConverter converter = new LongStringConverter();

		resourceLoader = new DefaultResourceLoader<String, byte[]>() {
			@Override
			public byte[] get(String key) throws ResourceException {

				if(key.equals("1")) {
					String retStr = TEST_VAL1;
					return retStr.getBytes();
				} else if(key.equals("2")) {
					String retStr = TEST_VAL2;
					return retStr.getBytes();
				} else {
					return null;
				}
			}

			@Override
			public boolean exists(String key) throws ResourceException {

				if(key.equals("1")) {
					return true;
				} else if(key.equals("2")) {
					return true;
				} else {
					return false;
				}
			}
		};
		testCacheWithResourceLoader = new KeyConvertingCache<Long, String, byte[]> (
				resourceLoader,
				converter
				);

        fileCache = new BytesFileCache(segments); // 2000 is to keep a rogue test case from eating disk, -1 so no worries about expiration.

        testCacheWithFileCache = new KeyConvertingCache<Long, byte [], byte[]> (
                fileCache,
                new Converter<Long, byte[]>() {

                    @Override
                    public byte[] convert(Long aLong) throws ResourceException {
                        return longToByteArray(aLong);
                    }

                    @Override
                    public Long restore(byte[] newVal) throws ResourceException {
                        return bytesToLong(newVal);
                    }
                }
        );

	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void testCacheFlavor() throws ResourceException {

		deleteRoot(rootDir);

		// Try a put and make sure a file is created.
		testCacheWithFileCache.put(1L, TEST_VAL1.getBytes());
		assertNotNull(testCacheWithFileCache.get(1L));

		// Put another an make sure they both have files created.
		testCacheWithFileCache.put(2L, TEST_VAL2.getBytes());

        assertNotNull(testCacheWithFileCache.get(2L));
		
		// Test exists().
		try {
			assertTrue(testCacheWithFileCache.exists(1L));
		} catch (ResourceException e) {
			System.out.println(e);
			fail();
		}
		// Make sure it's not just nodding its head...
		try {
			assertFalse(testCacheWithFileCache.exists(3L));
		} catch (ResourceException e) {
			System.out.println(e);
			fail();
		}

		// Now test a positive get().
		try {
			byte[] result = testCacheWithFileCache.get(1L);
			assertNotNull(result);
			String resultStr = new String(result);
			assertTrue(resultStr.equals(TEST_VAL1));
		} catch (Exception e){
			System.out.println(e);
			fail();
		}
		// and a negative get().
		try {
			byte[] result = testCacheWithFileCache.get(3L);
			assertNull(result);
		} catch (Exception e){
			System.out.println(e);
			fail();
		}

		// Try a getAll() with present and missing items, tests negative get() as well.
		List<Long> keys = new ArrayList<Long>();
		keys.add(-1L);
		keys.add(1L);
		keys.add(2L);
		keys.add(3L);
		
		// Now test remove()
		testCacheWithFileCache.remove(1L);
        assertNull(testCacheWithFileCache.get(1L));
		
		// and clear()
		try {
            testCacheWithFileCache.clear();
        } catch (ResourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertNull(testCacheWithFileCache.get(2L));
		
	}

	@Test
	public void testResourceLoaderFlavor() {

		// Try a put.
		testCacheWithResourceLoader.put(1L, TEST_VAL1.getBytes());

		// Test exists().
		try {
			assertTrue(testCacheWithResourceLoader.exists(1L));
		} catch (ResourceException e) {
			System.out.println(e);
			fail();
		}
		// Make sure it's not just nodding its head...
		try {
			assertFalse(testCacheWithResourceLoader.exists(3L));
		} catch (ResourceException e) {
			System.out.println(e);
			fail();
		}

		// Now test a positive get().
		try {
			byte[] result = testCacheWithResourceLoader.get(1L);
			assertNotNull(result);
			String resultStr = new String(result);
			assertTrue(resultStr.equals(TEST_VAL1));
		} catch (Exception e){
			System.out.println(e);
			fail();
		}
		// and a negative get().
		try {
			byte[] result = testCacheWithResourceLoader.get(3L);
			assertNull(result);
		} catch (Exception e){
			System.out.println(e);
			fail();
		}

		// Try a getAll() with present and missing items, tests negative get() as well.
		List<Long> keys = new ArrayList<Long>();
		keys.add(-1L);
		keys.add(1L);
		keys.add(2L);
		keys.add(3L);

		try {
			List<byte[]> results = testCacheWithResourceLoader.getAll(keys);
			assertNotNull(results);
			assertTrue(results.size() == 4);
			logResults(results);
			assertTrue(results.get(0) == null);
			assertTrue(new String(results.get(1)).equals(TEST_VAL1));
			assertTrue(new String(results.get(2)).equals(TEST_VAL2));
			assertTrue(results.get(3) == null);
		} catch (ResourceException e) {
			System.out.println(e);
			fail();
		}
	}

	void deleteRoot (File root) {
		if (root.exists()) {
			for (File cacheFile: root.listFiles()){
				cacheFile.delete();
			}
			root.delete();
		}
	}

	void logResults(List<byte[]> results) {
		for (byte[] result: results) {
			if (result == null){
				System.out.println("null");
			} else {
				System.out.println(new String(result));
			}
		}
	}
}
