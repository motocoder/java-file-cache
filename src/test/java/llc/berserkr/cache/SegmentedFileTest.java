package llc.berserkr.cache;

import llc.berserkr.cache.exception.OutOfSpaceException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.SpaceFragementedException;
import llc.berserkr.cache.exception.WriteFailure;
import llc.berserkr.cache.hash.SegmentedStreamingFile;
import llc.berserkr.cache.hash.SegmentedBytesDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static llc.berserkr.cache.hash.SegmentedTransactions.*;
import static llc.berserkr.cache.util.DataUtils.convertInputStreamToBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SegmentedFileTest {

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

    @Test
    public void testUtils() throws IOException {

        final int size = 4449929;

        final byte [] sizeBytes = SegmentedStreamingFile.intToByteArray(size);

        assertEquals(size, SegmentedStreamingFile.bytesToInt(sizeBytes));
    }

    @Test
    public void testSegmentFile() throws IOException, ReadFailure, WriteFailure {

        final long start = System.currentTimeMillis();

        final int JUNK_COUNT = 1;

        final SegmentedStreamingFile segmentedFile = new SegmentedStreamingFile(segmentFile);

        final Set<byte []> junkBytes = new HashSet<byte[]>();
        final Set<String> junkStrings = new HashSet<>();

        //create a bunch of junk data
        for(int i = 0; i < JUNK_COUNT; i++) {

            int junkItemsCount = (int) (Math.random() * 50F); //0 - < 49

            final StringBuilder builder = new StringBuilder();
            for(int j = 0; j < junkItemsCount; j++) {
                builder.append(UUID.randomUUID());
            }

            junkStrings.add(builder.toString());
            junkBytes.add(builder.toString().getBytes());

        }

        System.out.println("junkBytes: " + junkBytes.size() + " " + (System.currentTimeMillis() - start));

        final List<Long> addressesUsed = new ArrayList<>();

        //write the junk data into segments at the end
        for(final byte [] junk : junkBytes) {

            final long address = segmentedFile.writeToEnd(new ByteArrayInputStream(junk));

            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

            addressesUsed.add(address);

        }

        System.out.println("data written : " + addressesUsed.size() + " " + (System.currentTimeMillis() - start));

        final Set<byte []> wasWrote = new HashSet<>();

        //read in everything we wrote and store in wasWrote
        for(final long address : addressesUsed) {

            wasWrote.add(convertInputStreamToBytes(segmentedFile.readSegment(address)));
        }

        System.out.println("data read : " + wasWrote.size() + " " + (System.currentTimeMillis() - start));

        assertEquals(junkBytes.size(), wasWrote.size());

        for(final byte [] junk : junkBytes) {
            System.out.println("junk length " + junk.length);
            System.out.println("junk " + new String(junk));
        }

        for(final byte [] wrote : wasWrote) {
            System.out.println("wrote length " + wrote.length);
            System.out.println("wrote " + new String(wrote));
        }

        //check what we wrote vs what we read
        for(final byte [] wrote : wasWrote) {

            final String wroteString = new String(wrote, StandardCharsets.UTF_8);

            if(!junkStrings.contains(wroteString)) {
                fail();
            }
        }

        //grab half of the addresses we wrote, read the data, and remove them, then re-add them

        final Map<Long, byte []> halfAddresses = new HashMap<>();

        final int halfSize = addressesUsed.size() / 2;

        while(addressesUsed.size() > halfSize) {

            final Long addressToRemove = addressesUsed.remove((int) (Math.random() * addressesUsed.size()));

            halfAddresses.put(addressToRemove, convertInputStreamToBytes(segmentedFile.readSegment(addressToRemove)));

        }

        System.out.println("grabbed half the addresses " + halfAddresses.size());

        //deletes the data by marking it free
        for(final long address : halfAddresses.keySet()) {
            segmentedFile.writeState(address, SegmentedStreamingFile.FREE_STATE);
        }

        System.out.println("deleted " + halfAddresses.size());

        final Set<byte []> writeThese = new HashSet<>(halfAddresses.values());

        final Set<Long> wroteToFree = new HashSet<>();
        final Set<Long> wroteToEnd = new HashSet<>();
        int fragmentedCount = 0;

        //write the data back in
        for(final byte [] write : writeThese) {

            try {

                try {

                    //find a free address and write to it
                    final long freeAddress = segmentedFile.getFreeSegment(write.length);

                    segmentedFile.write(freeAddress, write);
                    segmentedFile.writeState(freeAddress, SegmentedStreamingFile.BOUND_STATE);

                    wroteToFree.add(freeAddress);

                }
                catch (final SpaceFragementedException e) {

                    segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());

                    fragmentedCount++;

                    segmentedFile.write(e.getAddress(), write);
                    segmentedFile.writeState(e.getAddress(), SegmentedStreamingFile.BOUND_STATE);

                    wroteToFree.add(e.getAddress());
                }

            } catch (OutOfSpaceException e) {

                //nothing for it to fit in, just allocate new space on the end.
                final long newAddress = segmentedFile.writeToEnd(new ByteArrayInputStream(write));

                wroteToEnd.add(newAddress);
            }

        }

        System.out.println("wrote to free " + wroteToFree.size() + " fragmentedCount " + fragmentedCount + " wroteToEnd " + wroteToEnd.size());
        System.out.println("end " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testTransactionReversal() throws ReadFailure, WriteFailure, OutOfSpaceException, IOException {

        final int JUNK_COUNT = 100;

        final SegmentedStreamingFile segmentedFile = new SegmentedStreamingFile(segmentFile);

        final Set<byte []> junkBytes = new HashSet<byte[]>();
        final Set<String> junkStrings = new HashSet<>();

        //create a bunch of junk data
        for(int i = 0; i < JUNK_COUNT; i++) {

            int junkItemsCount = (int) (Math.random() * 50F); //0 - < 49

            final StringBuilder builder = new StringBuilder();
            for(int j = 0; j < junkItemsCount; j++) {
                builder.append(UUID.randomUUID());
            }

            junkStrings.add(builder.toString());
            junkBytes.add(builder.toString().getBytes());

        }

        final List<Long> addressesUsed = new ArrayList<>();

        //write the junk data into segments at the end
        for(final byte [] junk : junkBytes) {

            final long address = segmentedFile.writeToEnd(new ByteArrayInputStream(junk));

            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

            addressesUsed.add(address);

        }

        final Set<byte []> wasWrote = new HashSet<>();

        for(final long address : addressesUsed) {

            wasWrote.add(convertInputStreamToBytes(segmentedFile.readSegment(address)));
        }

        assertEquals(junkBytes.size(), wasWrote.size());

        //check what we wrote vs what we read
        for(final byte [] wrote : wasWrote) {

            final String wroteString = new String(wrote, StandardCharsets.UTF_8);

            if(!junkStrings.contains(wroteString)) {
                fail();
            }
        }

        //grab half of the addresses we wrote, read the data, and remove them, then re-add them

        final Map<Long, byte []> halfAddresses = new HashMap<>();

        final int halfSize = addressesUsed.size() / 2;

        while(addressesUsed.size() > halfSize) {

            final Long addressToRemove = addressesUsed.remove((int) (Math.random() * addressesUsed.size()));

            halfAddresses.put(addressToRemove, convertInputStreamToBytes(segmentedFile.readSegment(addressToRemove)));

        }

        //deletes the data by marking it free
        for(final long address : halfAddresses.keySet()) {
            segmentedFile.writeState(address, SegmentedStreamingFile.FREE_STATE);
        }

        //ok we have the segmented file into a state of being half empty, this can be used now for our tests

        {
            //test reversing a write
            final long address = segmentedFile.getFreeSegment(50);

            startWritingTransaction(segmentedFile, address);

            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

            byte segmentState = segmentedFile.readSegmentState(address);

            assertEquals(SegmentedStreamingFile.BOUND_STATE, segmentState);

            segmentedFile.validateData();

            System.out.println("reading " + address);

            segmentState = segmentedFile.readSegmentState(address);

            assertEquals(SegmentedStreamingFile.FREE_STATE, segmentState);

        }

        {
            final long address = segmentedFile.findEnd();

            startAddTransaction(segmentedFile, 50);

            segmentedFile.validateData();

            byte segmentState = segmentedFile.readSegmentState(address);

            assertEquals(SegmentedStreamingFile.FREE_STATE, segmentState); //state didn't exist before

        }

        {

            final long address = segmentedFile.findEnd();

            startMergeTransaction(segmentedFile, address, 50);

            segmentedFile.validateData();

            byte segmentState = segmentedFile.readSegmentState(address);

            assertEquals(SegmentedStreamingFile.FREE_STATE, segmentState); //state didn't exist before

        }

    }

}
