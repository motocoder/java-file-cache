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
 */
public class BlobsSegmentedStreamingHashDataManager implements HashDataManager<byte [], Long> {

    private static final Logger logger = LoggerFactory.getLogger(BlobsSegmentedStreamingHashDataManager.class);

    private final SegmentedStreamingFile segmentedFile;

    public BlobsSegmentedStreamingHashDataManager(File segmentFile) {
        this.segmentedFile = new SegmentedStreamingFile(segmentFile);
    }

    @Override
    public Set<Pair<byte[], Long>> getBlobsAt(long blobIndex) throws ReadFailure {

        final byte[] segment;
        try {
            segment = convertInputStreamToBytes(segmentedFile.readSegment(blobIndex));
        } catch (IOException e) {
            throw new ReadFailure("failed", e);
        }

        return getSegmentPairs(segment);
    }

    @Override
    public long setBlobs(long blobIndex, Set<Pair<byte[], Long>> blobs) throws WriteFailure, ReadFailure {

        final byte [] pairData = getPairData(blobs);

        final long free;

        try {

            if(blobIndex >= 0) {

                int currentLength = segmentedFile.getSegmentLength(blobIndex);

                if(pairData.length <= currentLength) {

                    //we can re-use the segment we were already in
                    free = blobIndex;

                }
                else {

                    final long transAddress = startWritingTransaction(segmentedFile, blobIndex);

                    //delete the previous item
                    segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

                    endTransactions(segmentedFile, transAddress);


                    //find a free segment and write the data into it.
                    free = segmentedFile.getFreeSegment(pairData.length);

                }

            }
            else {

                //find a free segment and write the data into it.
                free = segmentedFile.getFreeSegment(pairData.length);

            }

            final long transAddress = startWritingTransaction(segmentedFile, free);

            segmentedFile.write(free, pairData);
            segmentedFile.writeState(free, SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile, transAddress);

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

            final long transAddress = startWritingTransaction(segmentedFile, address);

            segmentedFile.setSegmentSize(splitAddress, split2 - (SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT));
            segmentedFile.writeState(splitAddress, SegmentedStreamingFile.FREE_STATE);

            segmentedFile.setSegmentSize(address, split1);
            segmentedFile.write(address, pairData);
            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile, transAddress);

            return e.getAddress();
        }
        catch (final SpaceFragementedException e) {

            final long transAddress = startMergeTransaction(segmentedFile, e.getAddress(), e.getSegmentSize());

            segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());
            segmentedFile.write(e.getAddress(), pairData);
            segmentedFile.writeState(e.getAddress(), SegmentedStreamingFile.BOUND_STATE);

            endTransactions(segmentedFile, transAddress);

            return e.getAddress();
        }
        catch (OutOfSpaceException e) {

            final long transAddress = startAddTransaction(segmentedFile, pairData.length);

            try(final InputStream is = new ByteArrayInputStream(pairData)) {
                //no free segments, add to the end of the segment file
                final long address = segmentedFile.writeToEnd(is);

                segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);

                endTransactions(segmentedFile, transAddress);


                return address;
            } catch (IOException ex) {
                throw new WriteFailure("failed to write ", ex);
            }
        }

//        if(blobIndex >= 0) {
//
//            final long transAddress = startWritingTransaction(segmentedFile, blobIndex);
//
//            //delete the previous item
//            segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);
//
//            endTransactions(segmentedFile, transAddress);
//
//        }
//
//        final byte [] pairData = getPairData(blobs);
//
//        try {
//
//            //find a free segment and write the data into it.
//            long free = segmentedFile.getFreeSegment(pairData.length);
//
//            final long transAddress = startWritingTransaction(segmentedFile, free);
//
//            segmentedFile.write(free, pairData);
//            segmentedFile.writeState(free, SegmentedStreamingFile.BOUND_STATE);
//
//            endTransactions(segmentedFile, transAddress);
//
//            return free;
//        }
//        catch (final NeedsSplitException e) {
//
//            final int split1 = e.getSegmentSize() / 2;
//            final int split2 = e.getSegmentSize() - split1;
//
//            if(split1 + split2 != e.getSegmentSize()) {
//                throw new RuntimeException("remove when this is proven");
//            }
//
//            final long address = e.getAddress();
//            final long splitAddress = address + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + split1;
//
//            final long transAddress = startWritingTransaction(segmentedFile, address);
//
//            segmentedFile.setSegmentSize(splitAddress, split2 - (SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedStreamingFile.SEGMENT_LENGTH_BYTES_COUNT));
//            segmentedFile.writeState(splitAddress, SegmentedStreamingFile.FREE_STATE);
//
//            segmentedFile.setSegmentSize(address, split1);
//            segmentedFile.write(address, pairData);
//            segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);
//
//            endTransactions(segmentedFile, transAddress);
//
//            return e.getAddress();
//        }
//        catch (final SpaceFragementedException e) {
//
//            final long transAddress = startMergeTransaction(segmentedFile, e.getAddress(), e.getSegmentSize());
//
//            segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());
//            segmentedFile.write(e.getAddress(), pairData);
//            segmentedFile.writeState(e.getAddress(), SegmentedStreamingFile.BOUND_STATE);
//
//            endTransactions(segmentedFile, transAddress);
//
//            return e.getAddress();
//        }
//        catch (OutOfSpaceException e) {
//
//            final long transAddress = startAddTransaction(segmentedFile, pairData.length);
//
//            try(final InputStream is = new ByteArrayInputStream(pairData)) {
//                //no free segments, add to the end of the segment file
//                final long address = segmentedFile.writeToEnd(is);
//
//                segmentedFile.writeState(address, SegmentedStreamingFile.BOUND_STATE);
//
//                endTransactions(segmentedFile, transAddress);
//
//                return address;
//            } catch (IOException ex) {
//                throw new WriteFailure("failed to write ", ex);
//            }
//        }

    }

    @Override
    public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {
        final long transAddress = startWritingTransaction(segmentedFile, blobIndex);

        //delete the previous item
        segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

        endTransactions(segmentedFile, transAddress);
    }

    @Override
    public void clear() throws WriteFailure, ReadFailure {
        segmentedFile.clear();
    }

    public static byte [] getPairData(Set<Pair<byte [], Long>> pairsIn) {

        final List<Pair<byte [], Long>> pairs = new ArrayList<>(pairsIn);//ordered

        char count = (char) pairs.size();

        try {

            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write(charToBytes(count));

            for(int i = 0; i < count; i++) {

                final Pair<byte [], Long> pair = pairs.get(i);

                out.write(intToByteArray(pair.getOne().length + 8));
                out.write(intToByteArray(pair.getOne().length));
                out.write(pair.getOne());
                out.write(longToByteArray(pair.getTwo()));

            }

            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("this can't happen", e);
        }

    }

    public static Set<Pair<byte [], Long>> getSegmentPairs(byte [] data) {

        final Set<Pair<byte [], Long>> pairs = new HashSet<>();

        if(data == null || data.length == 0) { //this shouldn't happen unless data got corrupted and blown away
            return pairs;
        }

        char count = bytesToChar(new byte [] {data[0], data[1]});

        int dataBase = 2;

        for(int i = 0; i < count; i++) {

            final int pairLength = bytesToInt(new byte[] {data[dataBase], data[dataBase + 1], data[dataBase + 2], data[dataBase + 3]});
            final int keyLength = bytesToInt(new byte[] {data[dataBase + 4], data[dataBase + 5], data[dataBase + 6], data[dataBase + 7]});

            final byte [] keyData = new byte[keyLength];
            final byte [] payloadData = new byte[pairLength - keyLength];

            System.arraycopy(data, dataBase + 8, keyData, 0, keyLength);
            System.arraycopy(data, dataBase + 8 + keyLength, payloadData, 0, pairLength - keyLength);

            dataBase += pairLength + 8;

            pairs.add(new Pair<>(keyData, bytesToLong(payloadData)));
        }

        return pairs;

    }

}
