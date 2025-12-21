package llc.berserkr.cache.hash;

import llc.berserkr.cache.data.Pair;
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
public class StreamsSegmentedStreamingHashDataManager implements SingleValueHashDataManager<byte [], InputStream> {

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

    @Override
    public InputStream getBlobsAt(long blobIndex) throws ReadFailure {

        return segmentedFile.readSegment(blobIndex);

    }

    @Override
    public long setBlobs(long blobIndex, InputStream blobs) throws WriteFailure, ReadFailure {

        if(blobIndex >= 0) {

            startWritingTransaction(segmentedFile, blobIndex);

            //delete the previous item
            segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

            endTransactions(segmentedFile);

        }

        //We have to know the size of the data written in order to assign it to a segmented item
        //to do this we write it to temporary file then read it back and write it into the segment.
        //I don't think there's any other way to do this, this ultimately is a big limitation of the
        //streaming cache vs one doing it all in byte arrays. That's why we have the option for both.
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

            //splitting requires we take a large segment (address) and turn it into two segments
            //the split segment is created first, if the crash occurs we just back out and retain one
            //large segment
            //after it is complete the last operation is to set the split segment to the new size and
            //mark it bound to the new data
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

    @Override
    public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {
        startWritingTransaction(segmentedFile, blobIndex);

        //delete the previous item
        segmentedFile.writeState(blobIndex, SegmentedStreamingFile.FREE_STATE);

        endTransactions(segmentedFile);
    }

    @Override
    public void clear() throws WriteFailure, ReadFailure {
        segmentedFile.clear();
    }



}
