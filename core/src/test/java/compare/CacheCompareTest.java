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
import org.junit.jupiter.api.AfterEach;
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

    public static File tempDir = new File("./test-files");
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
        flag = false;
        deleteRoot(tempDir);

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

    @AfterEach
    public void tearDown() {
        deleteRoot(tempDir);
    }

    private static final int TEST_SIZE = 10;
    private static final int READ_LOOPS = 1000;

    void deleteRoot (File root) {
        if (root.exists()) {
            if (root.isDirectory()) {
                final File[] listed = root.listFiles();
                if (listed != null) {
                    for (File cacheFile : listed) {
                        deleteRoot(cacheFile);
                    }
                }
            }
            root.delete();
        }
    }

    private final int MULTI_WRITES = 10;
    private final int MULTI_READS = 50;
    private final int THREADS = 400;

    private boolean flag = false;

    @Test
    public void multiThreadedTest() throws IOException {
        TimingResult result = runStreamCacheBenchmark();
        assertFalse(flag);
    }

    @Test
    public void multiThreadedBytesTest() throws IOException {
        TimingResult result = runBytesCacheBenchmark();
        assertFalse(flag);
    }

    @Test
    public void multiThreadedLRUTest() throws IOException {
        TimingResult result = runDiskLruCacheBenchmark();
        assertFalse(flag);
    }

    @Test
    public void runAllAndSummarizeTest() throws Exception {

        logger.info("=== Cache Multi-Threaded Benchmark ===");
        logger.info("Threads: " + THREADS + ", Writes/thread: " + MULTI_WRITES + ", Reads/write: " + MULTI_READS);
        logger.info("");

        // StreamFileCache (InputStream values)
        setUp();
        flag = false;
        logger.info("Running StreamFileCache benchmark...");
        TimingResult streamResult = runStreamCacheBenchmark();
        boolean streamPassed = !flag;
        logger.info("  StreamFileCache done: " + streamResult.totalTimeMs + " ms");

        // BytesFileCache (byte[] values)
        setUp();
        flag = false;
        logger.info("Running BytesFileCache benchmark...");
        TimingResult bytesResult = runBytesCacheBenchmark();
        boolean bytesPassed = !flag;
        logger.info("  BytesFileCache done: " + bytesResult.totalTimeMs + " ms");

        // DiskLruCache (Jake Wharton)
        setUp();
        flag = false;
        logger.info("Running DiskLruCache benchmark...");
        TimingResult lruResult = runDiskLruCacheBenchmark();
        boolean lruPassed = !flag;
        logger.info("  DiskLruCache done: " + lruResult.totalTimeMs + " ms");

        long totalWriteOps = (long)(THREADS - 1) * MULTI_WRITES;
        long totalReadOps = totalWriteOps * MULTI_READS;

        logger.info("");
        logger.info("╔════════════════════════════════════════════════════════════════════════════════╗");
        logger.info("║                         CACHE BENCHMARK RESULTS                               ║");
        logger.info("╠════════════════════════════════════════════════════════════════════════════════╣");
        logger.info(String.format("║  %-22s %16s %16s %16s  ║", "", "StreamFile", "BytesFile", "DiskLruCache"));
        logger.info("╠════════════════════════════════════════════════════════════════════════════════╣");
        logger.info(String.format("║  %-22s %13d ms %13d ms %13d ms  ║", "Total time", streamResult.totalTimeMs, bytesResult.totalTimeMs, lruResult.totalTimeMs));
        logger.info(String.format("║  %-22s %16s %16s %16s  ║", "Passed", streamPassed, bytesPassed, lruPassed));
        logger.info(String.format("║  %-22s %16d %16d %16d  ║", "Write ops", totalWriteOps, totalWriteOps, totalWriteOps));
        logger.info(String.format("║  %-22s %16d %16d %16d  ║", "Read ops", totalReadOps, totalReadOps, totalReadOps));
        logger.info(String.format("║  %-22s %13.0f /s %13.0f /s %13.0f /s  ║", "Write throughput",
                streamResult.totalTimeMs > 0 ? totalWriteOps * 1000.0 / streamResult.totalTimeMs : 0,
                bytesResult.totalTimeMs > 0 ? totalWriteOps * 1000.0 / bytesResult.totalTimeMs : 0,
                lruResult.totalTimeMs > 0 ? totalWriteOps * 1000.0 / lruResult.totalTimeMs : 0));
        logger.info(String.format("║  %-22s %13.0f /s %13.0f /s %13.0f /s  ║", "Read throughput",
                streamResult.totalTimeMs > 0 ? totalReadOps * 1000.0 / streamResult.totalTimeMs : 0,
                bytesResult.totalTimeMs > 0 ? totalReadOps * 1000.0 / bytesResult.totalTimeMs : 0,
                lruResult.totalTimeMs > 0 ? totalReadOps * 1000.0 / lruResult.totalTimeMs : 0));
        logger.info(String.format("║  %-22s %13s ms %13d ms %13s ms  ║", "Max write latency",
                "n/a", bytesResult.maxWriteLatencyMs, "n/a"));
        logger.info(String.format("║  %-22s %13s ms %13d ms %13s ms  ║", "Max read latency",
                "n/a", bytesResult.maxReadLatencyMs, "n/a"));
        logger.info("╠════════════════════════════════════════════════════════════════════════════════╣");

        long fastest = Math.min(streamResult.totalTimeMs, Math.min(bytesResult.totalTimeMs, lruResult.totalTimeMs));
        String winner;
        if (fastest == bytesResult.totalTimeMs) winner = "BytesFileCache";
        else if (fastest == streamResult.totalTimeMs) winner = "StreamFileCache";
        else winner = "DiskLruCache";

        logger.info(String.format("║  Fastest: %-20s                                             ║", winner));
        logger.info("╚════════════════════════════════════════════════════════════════════════════════╝");

        assertTrue(streamPassed, "StreamFileCache test failed");
        assertTrue(bytesPassed, "BytesFileCache test failed");
        assertTrue(lruPassed, "DiskLruCache test failed");
    }

    private record TimingResult(long totalTimeMs, int maxWriteLatencyMs, int maxReadLatencyMs) {}

    private TimingResult runStreamCacheBenchmark() throws IOException {

        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        final File tempFolder = new File("./test-files/" + UUID.randomUUID() + "temp-data");
        final File dataFolder = new File("./test-files/" + UUID.randomUUID() + "data");

        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        tempFolder.mkdirs();
        dataFolder.mkdirs();

        final Cache<byte [], InputStream> fileCache = new StreamFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                new KeyConvertingCache<>(fileCache, new ReverseConverter<>(new BytesStringConverter()));

        final Cache<String, InputStream> cache = keyConvertingCache;

        flag = false;
        final long startTime = System.currentTimeMillis();

        for (int x = 0; x < THREADS -1; x++) {

            final int pre = x;
            pool.execute(() -> {
                try {
                    final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
                    final Random random = new Random();
                    final String key = "e" + String.valueOf(random.nextInt(999999));

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
            });
        }

        pool.shutdown();
        try { pool.awaitTermination(1000, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }

        return new TimingResult(System.currentTimeMillis() - startTime, 0, 0);
    }

    private TimingResult runBytesCacheBenchmark() throws IOException {

        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        final File tempFolder = new File("./test-files/" + UUID.randomUUID() + "temp-data");
        final File dataFolder = new File("./test-files/" + UUID.randomUUID() + "data");

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
        final long startTime = System.currentTimeMillis();

        for (int x = 0; x < THREADS -1; x++) {

            final int pre = x;
            pool.execute(() -> {
                try {
                    final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
                    final Random random = new Random();
                    final String key = "e" + String.valueOf(random.nextInt(999999));

                    final StopWatch stopWatch = new StopWatch();

                    for(int i = 0; i < MULTI_WRITES; i++) {

                        stopWatch.reset();
                        stopWatch.start();
                        cache.put(key, value.getBytes());
                        stopWatch.stop();

                        if(stopWatch.getTime() > writeLatency.get()) {
                            writeLatency.set((int) stopWatch.getTime());
                        }

                        int writes = countWrites.incrementAndGet();
                        if(writes % 10000 == 0) {
                            logger.info("writes " + writes);
                        }

                        for(int j = 0; j < MULTI_READS; j++) {

                            stopWatch.reset();
                            stopWatch.start();
                            final String returnValue = new String(cache.get(key));
                            stopWatch.stop();

                            if(stopWatch.getTime() > readLatency.get()) {
                                readLatency.set((int) stopWatch.getTime());
                            }

                            final int reads = countReads.incrementAndGet();
                            if(reads % 1000000 == 0) {
                                logger.info("reads " + reads);
                            }

                            assertEquals(value, returnValue);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    flag = true;
                }
            });
        }

        pool.shutdown();
        try { pool.awaitTermination(1000, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }

        return new TimingResult(System.currentTimeMillis() - startTime, writeLatency.get(), readLatency.get());
    }

    private TimingResult runDiskLruCacheBenchmark() throws IOException {

        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        final File tempFolder = new File("./test-files/" + UUID.randomUUID().toString() + "temp-data");
        final File dataFolder = new File("./test-files/" + UUID.randomUUID().toString() + "data");

        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        tempFolder.mkdirs();
        dataFolder.mkdirs();

        flag = false;
        final long startTime = System.currentTimeMillis();

        for (int x = 0; x < THREADS-1; x++) {

            final int pre = x;
            pool.execute(() -> {
                try {
                    final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
                    final Random random = new Random();
                    final String key = "e" + String.valueOf(random.nextInt(999999));

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
            });
        }

        pool.shutdown();
        try { pool.awaitTermination(1000, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }

        return new TimingResult(System.currentTimeMillis() - startTime, 0, 0);
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

        logger.info("data written " + (System.currentTimeMillis() - startTime));

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

        logger.info("test time: " + (System.currentTimeMillis() - startTime));

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

        logger.info("data written " + (System.currentTimeMillis() - startTime));

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

        logger.info("test time: " + (System.currentTimeMillis() - startTime));
    }
}
