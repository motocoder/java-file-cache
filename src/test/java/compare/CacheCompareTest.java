package compare;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCacheTest;
import llc.berserkr.cache.FileHashCache;
import llc.berserkr.cache.exception.CacheException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CacheCompareTest {

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

    private static final int TEST_SIZE = 1000;
    private static final int READ_LOOPS = 1000;

    @Test
    public void test() throws CacheException, IOException {

        final FileHashCache hashCache = new FileHashCache(hashCacheDir, TEST_SIZE * 2);

        final Map<String, String> data = new HashMap<>();

        for(int i = 0; i < TEST_SIZE; i++) {
            data.put("key" + i, "value" + i);
        }

        final long startTime = System.currentTimeMillis();

        for(final Map.Entry<String, String> entry : data.entrySet()) {
            hashCache.put(entry.getKey().getBytes(StandardCharsets.UTF_8), entry.getValue().getBytes(StandardCharsets.UTF_8));
        }

        System.out.println("data written " + (System.currentTimeMillis() - startTime));

        final Map<String, String> readData = new HashMap<>();

        for(int i = 0; i < READ_LOOPS; i++) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {

                final String keyString = entry.getKey();
                final String value = new String(hashCache.get(keyString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

                readData.put(keyString, value);

            }
        }

        assertEquals(readData.size(), data.size());
        assertEquals(readData, data);

        System.out.println("test time: " + (System.currentTimeMillis() - startTime));

    }

    @Test
    public void testLRU() throws CacheException, IOException {
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
