package llc.berserkr.cache;

import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.*;
import llc.berserkr.cache.util.StreamUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * This class manages putting hashed items into the segmented file.
 *
 * It also maintains the transaction lifecycle of reads/and writes.
 *
 * TODO modify reading/writing to allow multiple reads async to writes
 * TODO modify the value to be streamable instead of bytes to handle large values (potentially infinite instead of limited by heap size)
 */
public class StreamsSegmentedStreamingHashDataManager {

    private static final Logger logger = LoggerFactory.getLogger(StreamsSegmentedStreamingHashDataManager.class);

    private final SegmentedStreamingFile segmentedFile;
    private final File tempDirectory;

    public StreamsSegmentedStreamingHashDataManager(File segmentFile, File tempDirectory) {

        tempDirectory.mkdirs();

        if(!tempDirectory.isDirectory() || !tempDirectory.exists()) {
            throw new IllegalArgumentException("temp directory is bad");
        }
        this.segmentedFile = new SegmentedStreamingFile(segmentFile);
        this.tempDirectory = tempDirectory;
    }

    public InputStream getBlobsAt(long blobIndex) throws ReadFailure {

        return segmentedFile.readSegment(blobIndex);

    }

    public long setBlobs(long blobIndex, InputStream blobs) throws WriteFailure, ReadFailure {

        if(blobIndex >= 0) {

            startWritingTransaction(segmentedFile, blobIndex);

            //delete the previous item
            segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

            endTransactions(segmentedFile);

        }

        final int length;

        final File tempFile = new File(tempDirectory, UUID.randomUUID().toString());

        try(final FileOutputStream fos = new FileOutputStream(tempFile)) {
            length = copyAndCount(blobs, fos);
        } catch (IOException e) {
            throw new WriteFailure("failed", e);
        } finally {
            try {
                blobs.close();
            } catch (IOException e) {
                throw new WriteFailure("failed to close inputstream");
            }
        }

        try {

            //find a free segment and write the data into it.
            long free = segmentedFile.getFreeSegment(length);

            startWritingTransaction(segmentedFile, free);

            try(final FileInputStream fis = new FileInputStream(tempFile)) {
                segmentedFile.write(free, fis);
            }
            catch (IOException e) {
                throw new WriteFailure("failed to write temp file", e);
            }
            finally {
                tempFile.delete();
            }

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

            try(final FileInputStream fis = new FileInputStream(tempFile)) {
                segmentedFile.write(address, fis);
            }
            catch (IOException e2) {
                throw new WriteFailure("failed to write temp file", e2);
            }
            finally {
                tempFile.delete();
            }

            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile);

            return e.getAddress();
        }
        catch (final SpaceFragementedException e) {

            startMergeTransaction(segmentedFile, e.getAddress(), e.getSegmentSize());

            segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());

            try(final FileInputStream fis = new FileInputStream(tempFile)) {
                segmentedFile.write(e.getAddress(), fis);
            }
            catch (IOException e2) {
                throw new WriteFailure("failed to write temp file", e2);
            }
            finally {
                tempFile.delete();
            }

            segmentedFile.writeState(e.getAddress(), SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile);

            return e.getAddress();
        }
        catch (OutOfSpaceException e) {

            startAddTransaction(segmentedFile, length);

            try(final FileInputStream fis = new FileInputStream(tempFile)) {

                //no free segments, add to the end of the segment file
                final long address = segmentedFile.writeToEnd(fis);

                segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

                return address;
            }
            catch (IOException e2) {
                throw new WriteFailure("failed to write temp file", e2);
            }
            finally {
                endTransactions(segmentedFile);
                tempFile.delete();
            }

        }

    }

    public static void startAddTransaction(SegmentedStreamingFile segmentedFile, int length) throws ReadFailure, WriteFailure {

        final byte[] lengthBytes = SegmentedStreamingFile.intToByteArray(length);

        final byte [] writeTransaction = new byte[] {
            SegmentedStreamingFile.ADD_END_TRANSACTION, //if merge fails we will finish the merge but leave it empty
            lengthBytes[0],
            lengthBytes[1],
            lengthBytes[2],
            lengthBytes[3]
        };

        segmentedFile.writeTransactionalBytes(
            writeTransaction
        );

    }

    public static void startMergeTransaction(SegmentedStreamingFile segmentedFile, long address, int segmentSize) throws ReadFailure, WriteFailure {

        final byte[] addressBytes = SegmentedStreamingFile.longToByteArray(address);
        final byte[] lengthBytes = SegmentedStreamingFile.intToByteArray(segmentSize);

        final byte [] writeTransaction = new byte[] {
                SegmentedStreamingFile.MERGE_TRANSACTION, //if merge fails we will finish the merge but leave it empty
                addressBytes[0],
                addressBytes[1],
                addressBytes[2],
                addressBytes[3],
                addressBytes[4],
                addressBytes[5],
                addressBytes[6],
                addressBytes[7],
                lengthBytes[0],
                lengthBytes[1],
                lengthBytes[2],
                lengthBytes[3]
        };

        segmentedFile.writeTransactionalBytes(
                writeTransaction
        );

    }

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

    public static void endTransactions(SegmentedStreamingFile segmentedFile) throws ReadFailure, WriteFailure {
        segmentedFile.writeTransactionalBytes(new byte[] {});
    }

    public static void startWritingTransaction(SegmentedStreamingFile segmentedFile, long address) throws ReadFailure, WriteFailure {

        final byte[] addressBytes = SegmentedStreamingFile.longToByteArray(address);
        final byte [] writeTransaction = new byte[] {
            SegmentedStreamingFile.WRITING_TRANSACTION, //reversal of a write just deletes it anyway
            addressBytes[0],
            addressBytes[1],
            addressBytes[2],
            addressBytes[3],
            addressBytes[4],
            addressBytes[5],
            addressBytes[6],
            addressBytes[7]
        };

        segmentedFile.writeTransactionalBytes(
                writeTransaction
        );

    }

    public static int copyAndCount(InputStream inputStream, OutputStream outputStream) throws IOException {

        byte[] buffer = new byte[8192]; // Use a sensible buffer size (e.g., 1KB, 4KB, 8KB)
        int bytesRead;

        int totalRead = 0;

        // Read from the input stream into the buffer until the end of the stream is reached (read() returns -1)
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            // Write the read bytes to the output stream
            outputStream.write(buffer, 0, bytesRead);

            totalRead += bytesRead;
        }

        // It's good practice to flush the output stream to ensure all buffered bytes are written to the destination
        outputStream.flush();

        return totalRead;

    }

}
