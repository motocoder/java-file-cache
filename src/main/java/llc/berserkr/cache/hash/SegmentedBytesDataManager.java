package llc.berserkr.cache.hash;

import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static llc.berserkr.cache.hash.SegmentedTransactions.*;
import static llc.berserkr.cache.util.DataUtils.*;

/**
 * This class manages putting hashed items into the segmented file.
 *
 * It also maintains the transaction lifecycle of reads/and writes.
 *
 * TODO modify reading/writing to allow multiple reads async to writes
 * TODO modify the value to be streamable instead of bytes to handle large values (potentially infinite instead of limited by heap size)
 */
public class SegmentedBytesDataManager implements HashDataManager<byte [], byte []> {

    private static final Logger logger = LoggerFactory.getLogger(SegmentedBytesDataManager.class);

    private final SegmentedStreamingFile segmentedFile;

    public SegmentedBytesDataManager(File segmentFile) {
        this.segmentedFile = new SegmentedStreamingFile(segmentFile);
    }

    public Set<Pair<byte[], byte[]>> getBlobsAt(long blobIndex) throws ReadFailure {

        final byte[] segment;
        try {
            segment = convertInputStreamToBytes(segmentedFile.readSegment(blobIndex));
        } catch (IOException e) {
            throw new ReadFailure("failed", e);
        }

        return getSegmentPairs(segment);
    }

    public long setBlobs(long blobIndex, Set<Pair<byte[], byte[]>> blobs) throws WriteFailure, ReadFailure {

        if(blobIndex >= 0) {

            startWritingTransaction(segmentedFile, blobIndex);

            //delete the previous item
            segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

            endTransactions(segmentedFile);

        }

        final byte [] pairData = getPairData(blobs);

        try {

            //find a free segment and write the data into it.
            long free = segmentedFile.getFreeSegment(pairData.length);

            startWritingTransaction(segmentedFile, free);

            segmentedFile.write(free, pairData);
            segmentedFile.writeState(free, SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile);

            return free;
        }
        catch (final NeedsSplitException e) {

            final int split1 = e.getSegmentSize() / 2;
            final int split2 = e.getSegmentSize() - split1;

            if(split1 + split2 != e.getSegmentSize()) {
                throw new RuntimeException("remove when this is proven");
            }

            final long address = e.getAddress();
            final long splitAddress = address + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + split1;

            startWritingTransaction(segmentedFile, address);

            segmentedFile.setSegmentSize(splitAddress, split2 - (SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT));
            segmentedFile.writeState(splitAddress, SegmentedStreamingFile.FREE_STATE);

            segmentedFile.setSegmentSize(address, split1);
            segmentedFile.write(address, pairData);
            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile);

            return e.getAddress();
        }
        catch (final SpaceFragementedException e) {

            startMergeTransaction(segmentedFile, e.getAddress(), e.getSegmentSize());

            segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());
            segmentedFile.write(e.getAddress(), pairData);
            segmentedFile.writeState(e.getAddress(), SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile);

            return e.getAddress();
        }
        catch (OutOfSpaceException e) {

            startAddTransaction(segmentedFile, pairData.length);

            try(final InputStream is = new ByteArrayInputStream(pairData)) {
                //no free segments, add to the end of the segment file
                final long address = segmentedFile.writeToEnd(is);

                segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

                endTransactions(segmentedFile);

                return address;
            } catch (IOException ex) {
                throw new WriteFailure("failed to write ", ex);
            }
        }

    }

//    public static void startAddTransaction(SegmentedStreamingFile segmentedFile, int length) throws ReadFailure, WriteFailure {
//
//        final byte[] lengthBytes = SegmentedStreamingFile.intToByteArray(length);
//
//        final byte [] writeTransaction = new byte[] {
//            SegmentedStreamingFile.ADD_END_TRANSACTION, //if merge fails we will finish the merge but leave it empty
//            lengthBytes[0],
//            lengthBytes[1],
//            lengthBytes[2],
//            lengthBytes[3]
//        };
//
//        segmentedFile.writeTransactionalBytes(
//            writeTransaction
//        );
//
//    }
//
//    public static void startMergeTransaction(SegmentedStreamingFile segmentedFile, long address, int segmentSize) throws ReadFailure, WriteFailure {
//
//        final byte[] addressBytes = SegmentedStreamingFile.longToByteArray(address);
//        final byte[] lengthBytes = SegmentedStreamingFile.intToByteArray(segmentSize);
//
//        final byte [] writeTransaction = new byte[] {
//                SegmentedStreamingFile.MERGE_TRANSACTION, //if merge fails we will finish the merge but leave it empty
//                addressBytes[0],
//                addressBytes[1],
//                addressBytes[2],
//                addressBytes[3],
//                addressBytes[4],
//                addressBytes[5],
//                addressBytes[6],
//                addressBytes[7],
//                lengthBytes[0],
//                lengthBytes[1],
//                lengthBytes[2],
//                lengthBytes[3]
//        };
//
//        segmentedFile.writeTransactionalBytes(
//                writeTransaction
//        );
//
//    }

    public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {
        startWritingTransaction(segmentedFile, blobIndex);

        //delete the previous item
        segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

        endTransactions(segmentedFile);
    }

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

                out.write(SegmentedStreamingFile.intToByteArray(pair.getOne().length + pair.getTwo().length));
                out.write(SegmentedStreamingFile.intToByteArray(pair.getOne().length));
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

        if(data == null || data.length == 0) { //this shouldn't happen unless data got corrupted and blown away
            return pairs;
        }

        char count = bytesToChar(new byte [] {data[0], data[1]});

        int dataBase = 2;

        for(int i = 0; i < count; i++) {

            final int pairLength = SegmentedStreamingFile.bytesToInt(new byte[] {data[dataBase], data[dataBase + 1], data[dataBase + 2], data[dataBase + 3]});
            final int keyLength = SegmentedStreamingFile.bytesToInt(new byte[] {data[dataBase + 4], data[dataBase + 5], data[dataBase + 6], data[dataBase + 7]});

            final byte [] keyData = new byte[keyLength];
            final byte [] payloadData = new byte[pairLength - keyLength];

            System.arraycopy(data, dataBase + 8, keyData, 0, keyLength);
            System.arraycopy(data, dataBase + 8 + keyLength, payloadData, 0, pairLength - keyLength);

            dataBase += pairLength + 8;

            pairs.add(new Pair<>(keyData, payloadData));
        }

        return pairs;

    }

//    public static byte[] charToBytes(char ch) {
//
//        // Extract the most significant byte (MSB)
//        byte msb = (byte) ((ch >> 8) & 0xFF);
//
//        // Extract the least significant byte (LSB)
//        byte lsb = (byte) (ch & 0xFF);
//
//        return new byte[] {msb, lsb};
//
//    }
//
//    public static char bytesToChar(byte[] bytes) {
//
//        if(bytes.length != 2) { throw new IllegalArgumentException("bytes must be 2 lenght " + bytes.length); }
//
//        final byte byte1 = bytes[0]; // Example: 'A' (most significant byte)
//        final byte byte2 = bytes[1]; // Example: (least significant byte for 'A' in little-endian UTF-16)
//
//        // Combine the two bytes into a short, then cast to char
//        // Assuming byte1 is the most significant byte and byte2 is the least significant byte
//        // This order is typical for big-endian systems, or if you're constructing a specific UTF-16 value.
//        return (char) (((byte1 & 0xFF) << 8) | (byte2 & 0xFF));
//
//    }

//    public static void endTransactions(SegmentedStreamingFile segmentedFile) throws ReadFailure, WriteFailure {
//        segmentedFile.writeTransactionalBytes(new byte[] {});
//    }
//
//    public static void startWritingTransaction(SegmentedStreamingFile segmentedFile, long address) throws ReadFailure, WriteFailure {
//
//        final byte[] addressBytes = SegmentedStreamingFile.longToByteArray(address);
//        final byte [] writeTransaction = new byte[] {
//            SegmentedStreamingFile.WRITING_TRANSACTION, //reversal of a write just deletes it anyway
//            addressBytes[0],
//            addressBytes[1],
//            addressBytes[2],
//            addressBytes[3],
//            addressBytes[4],
//            addressBytes[5],
//            addressBytes[6],
//            addressBytes[7]
//        };
//
//        segmentedFile.writeTransactionalBytes(
//                writeTransaction
//        );
//
//    }



}
