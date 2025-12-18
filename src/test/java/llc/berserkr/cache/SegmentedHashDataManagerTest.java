package llc.berserkr.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SegmentedHashDataManagerTest {

    public static File tempDir = new File("./file-cache-temp");
    public static File segmentFile = new File(tempDir,"./segment");
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

    }

//    @Test
//    public void testByteStuff2() throws  Exception {
//
//        final SegmentedHashDataManager hashManager = new SegmentedHashDataManager(segmentFile);
//
//        final Set<Pair<String, String>> pairs = new HashSet<>();
//
//        for(int i = 0; i < 10000; i++) {
//
//            final String key =  String.valueOf(i);
//            final String data = UUID.randomUUID().toString();
//
//            pairs.add(new Pair<>(key, data));
//        }
//
//        final Set<Pair<byte [], byte []>> pairsBytes = new HashSet<>();
//
//        for(Pair<String, String> pair : pairs) {
//            pairsBytes.add(new Pair<>(pair.getOne().getBytes(StandardCharsets.UTF_8), pair.getTwo().getBytes(StandardCharsets.UTF_8)));
//        }
//
////        hashManager.setBlobs(-1L,)
//
//    }
}
