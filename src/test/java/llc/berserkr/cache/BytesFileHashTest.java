package llc.berserkr.cache;
import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
import llc.berserkr.cache.hash.FileHash;
import llc.berserkr.cache.hash.SegmentedBytesDataManager;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BytesFileHashTest {
    
    static {
        BasicConfigurator.configure();
    }
    
    private static final Logger logger = LoggerFactory.getLogger(BytesFileHashTest.class);

    public static File tempDir = new File("./file-cache-temp");
    public static File segmentFile = new File(tempDir,"./segment");
    public static File hashFile = new File(tempDir,"./hash");
    private File cacheDir;

    @BeforeEach
    public void setUp() throws Exception {

        tempDir.mkdirs();
        cacheDir = new File(tempDir, "BerserkrCache");
        cacheDir.mkdirs();

        for (File file : cacheDir.listFiles()) {
            file.delete();
        }

        if(segmentFile.exists()) {
            segmentFile.delete();
        }

        if(segmentFile.exists()) {
            throw new RuntimeException("Segmented file already exists");
        }
        segmentFile.createNewFile();

        if(hashFile.exists()) {
            hashFile.delete();
        }

        if(hashFile.exists()) {
            throw new RuntimeException("Segmented file already exists");
        }
        hashFile.createNewFile();

    }

    @Test
    public void testByteStuff() {

        final byte [] bytes1 = new byte [] {1,2,3,4,5};
        final byte [] bytes2 = new byte [] {1,2,3,4,5};

        assertTrue(Arrays.equals(bytes1, bytes1));
        assertEquals(Arrays.hashCode(bytes1), Arrays.hashCode(bytes2));

    }
    
    @Test
    public void testRealHashing() throws ReadFailure, WriteFailure, IOException {

        final File root = new File(cacheDir, "./temp-hash/");
        final File tempFolder = new File(cacheDir, "./temp-data");
        final File dataFolder = new File(cacheDir, "./segmentData");

        deleteRoot(root);
        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        dataFolder.createNewFile();

        final FileHash hash = new FileHash(root, dataFolder, 1000);

        checkFileEmpty(root);

        final int TEST_COUNT = 100;

        for(int i = 0; i < TEST_COUNT; i++) {

            hash.put(String.valueOf(i).getBytes(StandardCharsets.UTF_8), String.valueOf(i).getBytes(StandardCharsets.UTF_8));

            final byte [] segInput = hash.get(String.valueOf(i).getBytes(StandardCharsets.UTF_8));

            final String seg = new String(segInput);

            assertNotNull("i not null " + i, seg);

            assertEquals(String.valueOf(i), seg);

        }

        assertNull(hash.get(String.valueOf(TEST_COUNT + 1).getBytes(StandardCharsets.UTF_8)));

        for(int i = 0; i < TEST_COUNT; i++) {
            hash.remove(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
        }

        for(int i = 0; i < TEST_COUNT; i++) {

            final byte [] segInput = hash.get(String.valueOf(i).getBytes(StandardCharsets.UTF_8));

            assertNull(segInput);

        }

        checkFileEmpty(root);

        deleteRoot(root);
        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

    }
//
    @Test
    public void testRealHashingClear() throws ReadFailure, WriteFailure, IOException {

        final File root = new File(cacheDir, "./temp-hash/");
        final File tempFolder = new File(cacheDir, "./temp-data");
        final File dataFolder = new File(cacheDir, "./segmentData");

        deleteRoot(root);
        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

        dataFolder.createNewFile();

        final FileHash hash = new FileHash(root, dataFolder, 1000) {
            @Override
            public int hashCode(byte[] bytes) {
                return Arrays.hashCode(bytes);
            }

            @Override
            public boolean equals(byte[] key1, byte[] key2) {
                return Arrays.equals(key1, key2);
            }
        };

        checkFileEmpty(root);

        final int TEST_COUNT = 100;

        for(int i = 0; i < TEST_COUNT; i++) {

            hash.put(String.valueOf(i).getBytes(StandardCharsets.UTF_8), String.valueOf(i).getBytes(StandardCharsets.UTF_8));

            final byte [] segInput = hash.get(String.valueOf(i).getBytes(StandardCharsets.UTF_8));


            final String seg = new String(segInput, StandardCharsets.UTF_8);

            assertNotNull("i not null " + i, seg);

            assertEquals(String.valueOf(i), seg);

        }

        assertNull(hash.get(String.valueOf(TEST_COUNT + 1).getBytes(StandardCharsets.UTF_8)));

        hash.clear();

        for(int i = 0; i < TEST_COUNT; i++) {

            final byte [] segInput = hash.get(String.valueOf(i).getBytes(StandardCharsets.UTF_8));

            assertNull(segInput);

        }

        checkFileEmpty(root);

        deleteRoot(root);
        deleteRoot(tempFolder);
        deleteRoot(dataFolder);

    }

    @Test
    public void testManagerSegCreation() {

        final Set<Pair<String, String>> pairs = new HashSet<>();

        for(int i = 0; i < 10000; i++) {

            final String key =  String.valueOf(i);
            final String data = UUID.randomUUID().toString();

            pairs.add(new Pair<>(key, data));
        }

        final Set<Pair<byte [], byte []>> pairsBytes = new HashSet<>();

        for(Pair<String, String> pair : pairs) {
            pairsBytes.add(new Pair<>(pair.getOne().getBytes(StandardCharsets.UTF_8), pair.getTwo().getBytes(StandardCharsets.UTF_8)));
        }

        final byte [] pairData = SegmentedBytesDataManager.getPairData(pairsBytes);

        final Set<Pair<byte [], byte []>> pairsBytesRestored = SegmentedBytesDataManager.getSegmentPairs(pairData);
        final Set<Pair<String, String>> pairsStringRestored = new HashSet<>();

        for(Pair<byte [], byte []> restored : pairsBytesRestored) {

            pairsStringRestored.add(new Pair<>(new String(restored.getOne(), StandardCharsets.UTF_8), new String(restored.getTwo(), StandardCharsets.UTF_8)));
        }

        for(Pair<String, String> pair : pairsStringRestored) {
            assertTrue(pairs.contains(pair));
        }


    }

//    private static class LoggingManager implements HashDataManager<byte [], InputStream> {
//
//        private final HashDataManager<String, InputStream> internal;
//
//        public LoggingManager(HashDataManager<String, InputStream> internal) {
//            this.internal = internal;
//        }
//
//        @Override
//        public Set<Pair<String, InputStream>> getBlobsAt(long blobIndex) throws ReadFailure {
//            return internal.getBlobsAt(blobIndex);
//        }
//
//        @Override
//        public long setBlobs(long blobIndex, Set<Pair<String, InputStream>> blobs) throws ReadFailure, WriteFailure {
//
//            logger.debug("setting blobs " + blobIndex);
//
//            final long returnVal = internal.setBlobs(blobIndex, blobs);
//
//            logger.debug("set blobs at index " + blobIndex + " new Index " + returnVal);
//
//            return returnVal;
//
//        }
//
//        @Override
//        public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {
//
//            internal.eraseBlobs(blobIndex);
//
//        }
//
//        @Override
//        public void clear() throws WriteFailure, ReadFailure {
//            internal.clear();
//        }
//    }
    
    private void checkFileEmpty(final File root) {
        
        try {
            
            final InputStream in = new FileInputStream(root);
            
            try {
                
                int totalRead = 0;
                
                final byte [] buffer = new byte[1000];
                
                while(true) {
                    
                    final int read = in.read(buffer);
                    
                    if(read > 0) {
                        
                        for(int i = 0; i < read; i++) {
                            
                            if(buffer[i] != -1) {
                                
                                logger.debug("hash not empty " + totalRead + " " + buffer[i]);
                                fail();
                                
                            }
                            
                            totalRead++;
                            
                        }
                        
                    }
                    else {
                        break;
                    }
                    
                }
                
            }
            finally {
                in.close();
            }
        } 
        catch (FileNotFoundException e) {
            fail();
        }
        catch (IOException e) {
            fail();
        }
        
    }

    void deleteRoot (File root) {
        if (root.exists()) {
            
            if(root.listFiles() != null) {
                for (File cacheFile: root.listFiles()){
                    cacheFile.delete();
                }
            }
            root.delete();
        }
    }

    public static void copyTo(
            final InputStream in,
            final Set<OutputStream> outs,
            final int bufferSize
    ) throws IOException {

        if(in == null) {
            throw new NullPointerException("<StreamUtil><1>" + "In cannot be null");
        }

        if(outs == null) {
            throw new NullPointerException("<StreamUtil><2>" + "Out cannot be null");
        }

        if(outs.contains(null)) {
            throw new NullPointerException("<StreamUtil><3>" + "outs cannot contain null");
        }

        final byte [] buffer = new byte[bufferSize];

        while(true) {

            final int read = in.read(buffer);

            if(read <= 0) {
                break;
            }

            for(final OutputStream out : outs) {
                out.write(buffer, 0, read);
            }

        }

        for(final OutputStream out : outs) {
            out.flush();
        }

    }

    /**
     * flushes doesn't close
     *
     * @param in
     * @param out
     * @param bufferSize
     * @throws IOException
     */
    public static void copyTo(
            final InputStream in,
            final OutputStream out,
            final int bufferSize
    ) throws IOException {

        final Set<OutputStream> outs = new HashSet<OutputStream>();
        outs.add(out);

        copyTo(in, outs, bufferSize);

    }

    /**
     *
     * flushes doesn't close
     *
     * @param is
     * @param out
     * @throws IOException
     */
    public static void copyTo(InputStream is, OutputStream out) throws IOException {
        copyTo(is, out, 1024);
    }

    public static byte [] digest(InputStream in) throws NoSuchAlgorithmException, IOException {

        final MessageDigest md = MessageDigest.getInstance("MD5");

        final DigestInputStream is = new DigestInputStream(in, md);

        try {

            final byte [] buffer = new byte[1024];

            // read stream to EOF as normal...
            while(is.read(buffer) >= 0) {

            }

        }
        finally {
            is.close();
        }

        return is.getMessageDigest().digest();

    }


}
