package llc.berserkr.cache;

import llc.berserkr.cache.exception.OutOfSpaceException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.SpaceFragementedException;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SegmentedHashDataManager implements HashDataManager<byte [], byte []> {

    private static final Logger logger = LoggerFactory.getLogger(SegmentedHashDataManager.class);

    private final SegmentedFile segmentedFile;

    public SegmentedHashDataManager(File segmentFile) {
        this.segmentedFile = new SegmentedFile(segmentFile);
    }

    @Override
    public Set<Pair<byte[], byte[]>> getBlobsAt(long blobIndex) throws ReadFailure {

        final byte[] segment = segmentedFile.readSegment(blobIndex);

        return getSegmentPairs(segment);
    }

    @Override
    public long setBlobs(long blobIndex, Set<Pair<byte[], byte[]>> blobs) throws WriteFailure, ReadFailure {

        if(blobIndex >= 0) {
            //delete the previous item
            segmentedFile.writeState(blobIndex, SegmentedFile.FREE_STATE);
        }

        final byte [] pairData = getPairData(blobs);

        try {
            //find a free segment and write the data into it.
            long free = segmentedFile.getFreeSegment(pairData.length);

            segmentedFile.write(free, pairData);
            segmentedFile.writeState(free, SegmentedFile.BOUND_STATE);

            return free;
        }
        catch (final SpaceFragementedException e) {

            segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());
            segmentedFile.write(e.getAddress(), pairData);
            segmentedFile.writeState(e.getAddress(), SegmentedFile.BOUND_STATE);

            return e.getAddress();
        }
        catch (OutOfSpaceException e) {

            //no free segments, add to the end of the segment file
            final long address = segmentedFile.writeToEnd(pairData);

            segmentedFile.writeState(address, SegmentedFile.BOUND_STATE);
            return address;
        }

    }

    @Override
    public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {
        segmentedFile.writeState(blobIndex, SegmentedFile.FREE_STATE);
    }

    @Override
    public void clear() throws WriteFailure, ReadFailure {
        segmentedFile.clear();
    }

    public static byte [] getPairData(Set<Pair<byte [], byte []>> pairsIn) {

        final List<Pair<byte [], byte []>> pairs = new ArrayList<>(pairsIn);//ordered

        char count = (char) pairs.size();

        try {

            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write(charToBytes(count));

            for(int i = 0; i < count; i++) {

                final Pair<byte [], byte []> pair = pairs.get(i);

                out.write(SegmentedFile.intToByteArray(pair.getOne().length + pair.getTwo().length));
                out.write(SegmentedFile.intToByteArray(pair.getOne().length));
                out.write(pair.getOne());
                out.write(pair.getTwo());

            }

            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("this can't happen", e);
        }

    }

    public static Set<Pair<byte [], byte []>> getSegmentPairs(byte [] data) {

        final Set<Pair<byte [], byte []>> pairs = new HashSet<>();

        char count = bytesToChar(new byte [] {data[0], data[1]});

        int dataBase = 2;

        for(int i = 0; i < count; i++) {

            final int pairLength = SegmentedFile.bytesToInt(new byte[] {data[dataBase], data[dataBase + 1], data[dataBase + 2], data[dataBase + 3]});
            final int keyLength = SegmentedFile.bytesToInt(new byte[] {data[dataBase + 4], data[dataBase + 5], data[dataBase + 6], data[dataBase + 7]});

            final byte [] keyData = new byte[keyLength];
            final byte [] payloadData = new byte[pairLength - keyLength];

            System.arraycopy(data, dataBase + 8, keyData, 0, keyLength);
            System.arraycopy(data, dataBase + 8 + keyLength, payloadData, 0, pairLength - keyLength);

            dataBase += pairLength + 8;

            pairs.add(new Pair<>(keyData, payloadData));
        }

        return pairs;

    }

    public static byte[] charToBytes(char ch) {

        // Extract the most significant byte (MSB)
        byte msb = (byte) ((ch >> 8) & 0xFF);

        // Extract the least significant byte (LSB)
        byte lsb = (byte) (ch & 0xFF);

        return new byte[] {msb, lsb};

    }

    public static char bytesToChar(byte[] bytes) {

        if(bytes.length != 2) { throw new IllegalArgumentException("bytes must be 2 lenght " + bytes.length); }

        final byte byte1 = bytes[0]; // Example: 'A' (most significant byte)
        final byte byte2 = bytes[1]; // Example: (least significant byte for 'A' in little-endian UTF-16)

        // Combine the two bytes into a short, then cast to char
        // Assuming byte1 is the most significant byte and byte2 is the least significant byte
        // This order is typical for big-endian systems, or if you're constructing a specific UTF-16 value.
        return (char) (((byte1 & 0xFF) << 8) | (byte2 & 0xFF));

    }

}
