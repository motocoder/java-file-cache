package compare;

import com.jakewharton.disklrucache.DiskLruCache;
import llc.berserkr.cache.BytesFileCache;
import llc.berserkr.cache.Cache;
import llc.berserkr.cache.StreamFileCache;
import llc.berserkr.cache.KeyConvertingCache;
import llc.berserkr.cache.converter.BytesStringConverter;
import llc.berserkr.cache.converter.ReverseConverter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static llc.berserkr.cache.util.DataUtils.convertInputStreamToBytes;
import static org.junit.jupiter.api.Assertions.*;

public class CacheCompareTest {

    private static final Logger logger = LoggerFactory.getLogger(CacheCompareTest.class);

    private final int appVersion = 100;

    public static File tempDir = new File("./file-cache-temp");
    public static File segmentFile = new File(tempDir,"./segment");
    private File hashCacheDir;
    private File cacheDir;
    private File journalFile;
    private File journalBkpFile;
    private DiskLruCache cache;

    static final String JOURNAL_FILE = "journal";
    static final String JOURNAL_FILE_TEMP = "journal.tmp";
    static final String JOURNAL_FILE_BACKUP = "journal.bkp";

    @BeforeEach
    public void setUp() throws Exception {

        tempDir.mkdirs();
        hashCacheDir = new File(tempDir, "BerserkrCache");
        hashCacheDir.mkdirs();

        for (File file : hashCacheDir.listFiles()) {
            file.delete();
        }

        if(segmentFile.exists()) {
            segmentFile.delete();
        }

        if(segmentFile.exists()) {
            throw new RuntimeException("Segmented file already exists");
        }
        segmentFile.createNewFile();

        tempDir.mkdirs();
        cacheDir = new File(tempDir, "DiskLruCacheTest");
        cacheDir.mkdirs();


        journalFile = new File(cacheDir, JOURNAL_FILE);
        journalBkpFile = new File(cacheDir, JOURNAL_FILE_BACKUP);
        for (File file : cacheDir.listFiles()) {
            file.delete();
        }
        cache = DiskLruCache.open(cacheDir, appVersion, 2, Integer.MAX_VALUE);

    }

    private static final int TEST_SIZE = 10;
    private static final int READ_LOOPS = 1000;

    void deleteRoot (File root) {
        if (root.exists()) {

            final File[] listed = root.listFiles();

            if(listed != null) {
                for (File cacheFile: listed){
                    cacheFile.delete();
                }
            }

            root.delete();
        }
    }

    private final int MULTI_WRITES = 10;
    private final int MULTI_READS = 1000;
    private final int THREADS = 50;

    private boolean flag = false;

    @Test
    public void multiThreadedTest() throws IOException {

        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        final File tempFolder = new File("./target/test-files/" + UUID.randomUUID() + "temp-data");
        final File dataFolder = new File("./target/test-files/" + UUID.randomUUID() + "data");

        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        tempFolder.mkdirs();
        dataFolder.mkdirs();

        final Cache<byte [], InputStream> fileCache = new StreamFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                new KeyConvertingCache<>(fileCache, new ReverseConverter<>(new BytesStringConverter()));

        final Cache<String, InputStream> cache = keyConvertingCache;

        flag = false;
        for (int x = 0; x < THREADS -1; x++) {

            final int pre = x;
            pool.execute(

                    new Runnable() {

                        @Override
                        public void run() {

                            try {

                                // create varying length strings by concatenation
                                final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
                                final Random random = new Random();

                                final String key = "e" + String.valueOf(random.nextInt(999999));

                                logger.info("putting value: " + value + " with the key of " + key);

                                // TEST PUT, GET, REMOVE, and EXISTS

                                for(int i = 0; i < MULTI_WRITES; i++) {
                                    cache.put(key, new ByteArrayInputStream(value.getBytes()));

                                    for(int j = 0; j < MULTI_READS; j++) {
                                        final String returnValue = new String(convertInputStreamToBytes(cache.get(key)));

                                        assertEquals(value, returnValue);
                                    }
                                }

                            } catch (Throwable e) {
                                e.printStackTrace();
                                flag = true;
                            }

                        }

                    }

            );

        }

        pool.shutdown();

        try {
            pool.awaitTermination(1000, TimeUnit.SECONDS);
        }
        catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        assertFalse(flag);

    }

    @Test
    public void multiThreadedBytesTest() throws IOException {

        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        final File tempFolder = new File("./target/test-files/" + UUID.randomUUID() + "temp-data");
        final File dataFolder = new File("./target/test-files/" + UUID.randomUUID() + "data");

        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        tempFolder.mkdirs();
        dataFolder.mkdirs();

        final Cache<byte [], byte []> fileCache = new BytesFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConvertingCache =
                new KeyConvertingCache<>(fileCache, new ReverseConverter<>(new BytesStringConverter()));

        final Cache<String, byte []> cache = keyConvertingCache;

        final AtomicInteger countWrites = new AtomicInteger(0);
        final AtomicInteger countReads = new AtomicInteger(0);
        final AtomicInteger readLatency = new AtomicInteger(0);
        final AtomicInteger writeLatency = new AtomicInteger(0);


        flag = false;
        for (int x = 0; x < THREADS -1; x++) {

            final int pre = x;
            pool.execute(

                    new Runnable() {

                        @Override
                        public void run() {

                            try {

                                // create varying length strings by concatenation
                                final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
                                final Random random = new Random();

                                final String key = "e" + String.valueOf(random.nextInt(999999));

                                // TEST PUT, GET, REMOVE, and EXISTS

                                final StopWatch stopWatch = new StopWatch();

                                for(int i = 0; i < MULTI_WRITES; i++) {

                                    stopWatch.reset();
                                    stopWatch.start();

                                    cache.put(key, value.getBytes());

                                    stopWatch.stop();
                                    if(stopWatch.getTime() > writeLatency.get()) {
                                        writeLatency.set((int) stopWatch.getTime());
//                                        System.out.println("write latency " + stopWatch.getTime());
                                    }

                                    int writes = countWrites.incrementAndGet();

                                    if(writes % 100 == 0) {
//                                        System.out.println("writes " + writes);
                                    }

                                    for(int j = 0; j < MULTI_READS; j++) {

                                        stopWatch.reset();
                                        stopWatch.start();

                                        final String returnValue = new String(cache.get(key));
                                        stopWatch.stop();
                                        if(stopWatch.getTime() > readLatency.get()) {
                                            readLatency.set((int) stopWatch.getTime());
//                                            System.out.println("read latency " + stopWatch.getTime());
                                        }

                                        final int reads = countReads.incrementAndGet();

                                        if(reads % 10000 == 0) {
//                                            System.out.println("reads " + reads);
                                        }

                                        assertEquals(value, returnValue);

                                    }
                                }

                            } catch (Throwable e) {
                                e.printStackTrace();
                                flag = true;
                            }

                        }

                    }

            );

        }

        pool.shutdown();

        try {
            pool.awaitTermination(1000, TimeUnit.SECONDS);
        }
        catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        assertFalse(flag);

    }

    @Test
    public void multiThreadedLRUTest() throws IOException {

        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        final File tempFolder = new File("./target/test-files/" + UUID.randomUUID().toString() + "temp-data");
        final File dataFolder = new File("./target/test-files/" + UUID.randomUUID().toString() + "data");

        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        tempFolder.mkdirs();
        dataFolder.mkdirs();

        flag = false;

        for (int x = 0; x < THREADS-1; x++) {

            final int pre = x;
            pool.execute(

                    new Runnable() {

                        @Override
                        public void run() {

                            try {

                                // create varying length strings by concatenation
                                final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
                                final Random random = new Random();

                                final String key = "e" + String.valueOf(random.nextInt(999999));

                                logger.info("putting value: " + value + " with the key of " + key);

                                // TEST PUT, GET, REMOVE, and EXISTS

                                for(int i = 0; i < MULTI_WRITES; i++) {
                                    DiskLruCache.Editor creator = cache.edit(key);
                                    creator.set(0, value);
                                    creator.set(1, "");
                                    creator.commit();

                                    for(int j = 0; j < MULTI_READS; j++) {

                                        DiskLruCache.Snapshot snapshot = cache.get(key);

                                        final String valueSnap = snapshot.getString(0);
                                        final String value2 = snapshot.getString(1);

                                        assertEquals(value, valueSnap);
                                    }
                                }

                            } catch (Throwable e) {
                                flag = true;
                                e.printStackTrace();
                            }

                        }

                    }

            );

        }

        pool.shutdown();

        try {
            pool.awaitTermination(1000, TimeUnit.SECONDS);
        }
        catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        assertFalse(flag);

    }

    @Test
    public void test() throws IOException, ResourceException, InterruptedException {

        final StreamFileCache hashCache = new StreamFileCache(hashCacheDir, TEST_SIZE * 2);

        final Map<String, String> data = new HashMap<>();

        for(int i = 0; i < TEST_SIZE; i++) {
            data.put("key" + i, "value" + i);
        }

        final long startTime = System.currentTimeMillis();

        for(final Map.Entry<String, String> entry : data.entrySet()) {
            hashCache.put(entry.getKey().getBytes(StandardCharsets.UTF_8), new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }

        System.out.println("data written " + (System.currentTimeMillis() - startTime));

        final Map<String, String> readData = new HashMap<>();

        for(int i = 0; i < READ_LOOPS; i++) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {

                final String keyString = entry.getKey();
                final String value = new String(convertInputStreamToBytes(hashCache.get(keyString.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);

                readData.put(keyString, value);

            }
        }

        assertEquals(readData.size(), data.size());
        assertEquals(readData, data);

        System.out.println("test time: " + (System.currentTimeMillis() - startTime));

    }

    @Test
    public void testLRU() throws IOException {
        final Map<String, String> data = new HashMap<>();

        for(int i = 0; i < TEST_SIZE; i++) {
            data.put("key" + i, "value" + i);
        }

        final long startTime = System.currentTimeMillis();

        for(final Map.Entry<String, String> entry : data.entrySet()) {

            DiskLruCache.Editor creator = cache.edit(entry.getKey());
            creator.set(0, entry.getValue());
            creator.set(1, entry.getValue());
            creator.commit();

        }

        System.out.println("data written " + (System.currentTimeMillis() - startTime));

        final Map<String, String> readData = new HashMap<>();

        for(int i = 0; i < READ_LOOPS; i++) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {

                final String keyString = entry.getKey();

                DiskLruCache.Snapshot snapshot = cache.get(keyString);

                final String value = snapshot.getString(0);
                final String value2 = snapshot.getString(1);

                readData.put(keyString, value);

            }
        }

        assertEquals(readData.size(), data.size());
        assertEquals(readData, data);

        System.out.println("test time: " + (System.currentTimeMillis() - startTime));
    }
}
