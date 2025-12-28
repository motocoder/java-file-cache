package llc.berserkr.cache.hash;

import llc.berserkr.cache.data.FIFOByteFileBuffer;
import llc.berserkr.cache.data.RandomAccessFileWriter;
import llc.berserkr.cache.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.*;
import static llc.berserkr.cache.hash.SegmentedTransactions.*;
import static llc.berserkr.cache.util.DataUtils.copyAndCount;

/**
 * This class manages putting hashed items into the segmented file.
 *
 * It also maintains the transaction lifecycle of reads/and writes.
 *
 */
public class SegmentedStreamingDataManager implements SingleValueHashDataManager<byte [], InputStream> {

    private static final Logger logger = LoggerFactory.getLogger(SegmentedStreamingDataManager.class);

    private final SegmentedFile segmentedFile;
    private final ThreadLocal<FIFOByteFileBuffer> fifo;

    public SegmentedStreamingDataManager(File segmentFile, File tempDirectory) {

        tempDirectory.mkdirs();
        final File tempFile = new File(tempDirectory, UUID.randomUUID().toString());

        if(!tempDirectory.isDirectory() || !tempDirectory.exists()) {
            throw new IllegalArgumentException("temp directory is bad");
        }

        this.fifo = ThreadLocal.withInitial(() -> {
            try { //TODO make this memory size configurable
                return new FIFOByteFileBuffer(50_000, new RandomAccessFileWriter(tempFile));
            } catch (LinearStreamException e) {
                throw new IllegalArgumentException("temp directory is bad2");
            }
        });

        this.segmentedFile = new SegmentedFile(segmentFile);

    }

    @Override
    public InputStream getBlobsAt(long blobIndex) throws ReadFailure {
        return segmentedFile.readSegment(blobIndex);
    }

    @Override
    public long setBlobs(long blobIndex, InputStream blobs) throws WriteFailure, ReadFailure {

        //We have to know the size of the data written in order to assign it to a segmented item
        //to do this we write it to temporary file then read it back and write it into the segment.
        //I don't think there's any other way to do this, this ultimately is a big limitation of the
        //streaming cache vs one doing it all in byte arrays. That's why we have the option for both.
        //
        //this could be overcome by adding a segment linking functionality to the SegmentedStreamingFile
        //It would require a link to the next segment if this inputstreams write causes it to
        //overflow a segment it's written to. This is the only way to know the segment length after
        //writting it from a stream.
        final int length;

        try {

            final OutputStream os = fifo.get().getOutputStream();

            length = copyAndCount(blobs, os);

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

            final long free;

            if(blobIndex >= 0) {

                int currentLength = segmentedFile.getSegmentLength(blobIndex);

                if(length <= currentLength) {

                    //we can re-use the segment we were already in
                    free = blobIndex;

                }
                else {

                    long transArress = startWritingTransaction(segmentedFile, blobIndex);

                    //delete the previous item
                    segmentedFile.writeState(blobIndex, SegmentedFile.FREE_STATE);

                    endTransactions(segmentedFile, transArress);

                    //find a free segment and write the data into it.
                    free = segmentedFile.getFreeSegment(length);
                }

            }
            else {
                //find a free segment and write the data into it.
                free = segmentedFile.getFreeSegment(length);
            }

            final long transAddress = startWritingTransaction(segmentedFile, free);

            segmentedFile.write(free, fifo.get().getInputStream());

            segmentedFile.writeState(free, SegmentedFile.BOUND_STATE);

            endTransactions(segmentedFile, transAddress);

            return free;
        }
        catch (final NeedsSplitException e) {

            final int split1 = e.getSegmentSize() / 2;
            final int split2 = e.getSegmentSize() - split1;

            if(split1 + split2 != e.getSegmentSize()) {
                throw new RuntimeException("remove when this is proven");
            }

            //splitting requires we take a large segment (address) and turn it into two segments
            //the split segment is created first, if the crash occurs we just back out and retain one
            //large segment
            //after it is complete the last operation is to set the split segment to the new size and
            //mark it bound to the new data
            final long address = e.getAddress();
            final long splitAddress = address + SegmentedFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedFile.SEGMENT_LENGTH_BYTES_COUNT + split1;

            final long transAddress = startWritingTransaction(segmentedFile, address);

            segmentedFile.setSegmentSize(splitAddress, split2 - (SegmentedFile.SEGMENT_LENGTH_BYTES_COUNT + 1 + SegmentedFile.SEGMENT_LENGTH_BYTES_COUNT));
            segmentedFile.writeState(splitAddress, SegmentedFile.FREE_STATE);

            segmentedFile.setSegmentSize(address, split1);

            segmentedFile.write(address, fifo.get().getInputStream());

            segmentedFile.writeState(address, SegmentedFile.BOUND_STATE);

            endTransactions(segmentedFile, transAddress);

            return e.getAddress();
        }
        catch (final SpaceFragementedException e) {

            final long transAddress = startMergeTransaction(segmentedFile, e.getAddress(), e.getSegmentSize());

            segmentedFile.setSegmentSize(e.getAddress(), e.getSegmentSize());

            segmentedFile.write(e.getAddress(), fifo.get().getInputStream());

            segmentedFile.writeState(e.getAddress(), SegmentedFile.BOUND_STATE);

            endTransactions(segmentedFile, transAddress);

            return e.getAddress();
        }
        catch (OutOfSpaceException e) {

            final long transAddress = startAddTransaction(segmentedFile, length);

            try {

                //no free segments, add to the end of the segment file
                final long address = segmentedFile.writeToEnd(fifo.get().getInputStream());

                segmentedFile.writeState(address, SegmentedFile.BOUND_STATE);

                return address;
            }
            finally {
                endTransactions(segmentedFile, transAddress);
            }

        }

    }

    @Override
    public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {

        final long transAddress = startWritingTransaction(segmentedFile, blobIndex);

        //delete the previous item
        segmentedFile.writeState(blobIndex, SegmentedFile.FREE_STATE);

        endTransactions(segmentedFile, transAddress);

    }

    @Override
    public void clear() throws WriteFailure, ReadFailure {
        segmentedFile.clear();
    }

}
