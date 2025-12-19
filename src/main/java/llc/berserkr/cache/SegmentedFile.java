package llc.berserkr.cache;

import llc.berserkr.cache.exception.OutOfSpaceException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.SpaceFragementedException;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class SegmentedFile {

    private static final Logger logger = LoggerFactory.getLogger(SegmentedFile.class);

    //bytes at the beginning of each segment to construct into an int for the segment length
    private static final int SEGMENT_LENGTH_BYTES_COUNT = 4;

    //byte range -128 to 127
    public static final byte FREE_STATE = -128;
    public static final byte BOUND_STATE = -127;
    public static final byte TRANSITIONAL_STATE = -126;

    private final File root;
    private final SegmentReference reference = new SegmentReference();
    private final RandomAccessFile writeRandom;
    private final RandomAccessFile readRandom;
    private long lastKnownAddress = 0;

    /**
     * [x,x,x,x][x][x,x,x,x][x*n] segmentSize, type, fillSize, payload
     *
     * @param root
     */
    public SegmentedFile(
        final File root
    ) {

        this.root = root;

        if(!new File(root.getParent()).exists()) {
            new File(root.getParent()).mkdirs();
        }

        if(root.isDirectory()) {
            throw new IllegalArgumentException("file location must not be a directory " + root);
        }

        try {
            this.writeRandom = new RandomAccessFile(root, "rws");
            this.readRandom = new RandomAccessFile(root, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     *
     * @param address
     * @param segment
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public void write(long address, byte[] segment) throws ReadFailure, WriteFailure {

        try(final RandomAccessFile random = new RandomAccessFile(root, "rws")) {

            try {

                random.seek(address + SEGMENT_LENGTH_BYTES_COUNT); //seek to the state byte of the new segment that doesn't exist yet
                random.write(new byte[]{TRANSITIONAL_STATE});
                random.write(intToByteArray(segment.length));//write the fill size
                random.write(segment); //write the payload

                reference.setSegmentType(address, TRANSITIONAL_STATE);

            }
            catch (IOException e) {
                throw new WriteFailure("failed to write " + e.getMessage());
            }

        } catch (FileNotFoundException e) {
            throw new ReadFailure("file not found: " + root, e);
        } catch (IOException e) {
            throw new ReadFailure("file not opened " + root, e);
        }
    }

    public void writeState(long address, byte state) throws WriteFailure, ReadFailure {

        try {

            writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT);

            try {
                writeRandom.write(new byte[]{state});
                reference.setSegmentType(address, state);
            }
            catch (IOException e) {
                throw new WriteFailure("unknown write error " + e.getMessage(), e);
            }

        } catch (FileNotFoundException e) {
            throw new ReadFailure("file not found: " + root, e);
        } catch (IOException e) {
            throw new ReadFailure("unknown read error " + e.getMessage(), e);
        }
    }

    /**
     *
     * @param segment
     * @return
     * @throws WriteFailure
     * @throws ReadFailure
     */
    public long writeToEnd(final byte [] segment) throws WriteFailure, ReadFailure {

        long address = lastKnownAddress;

        while (true) {

            try {

                readRandom.seek(address);

                final byte[] segmentSize = new byte[SEGMENT_LENGTH_BYTES_COUNT];

                try {

                    readRandom.readFully(segmentSize, 0, segmentSize.length);

                    final int segmentLength = bytesToInt(segmentSize);

                    reference.setSegmentSize(address, segmentLength);

                    address += segmentLength + SEGMENT_LENGTH_BYTES_COUNT + 1 + SEGMENT_LENGTH_BYTES_COUNT ;

                }
                catch (EOFException e) {

                    try {
                        writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT); //seek to the state byte of the new segment that doesn't exist yet
                        writeRandom.write(new byte[]{TRANSITIONAL_STATE});
                        writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT + 1 + /*fill length*/SEGMENT_LENGTH_BYTES_COUNT);
                        writeRandom.write(segment);
                        writeRandom.seek(address);
                        writeRandom.write(intToByteArray(segment.length));
                        writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT + 1);
                        writeRandom.write(intToByteArray(segment.length)); //data fill is same size as the segment since its a new segment

                        reference.setSegmentType(address, TRANSITIONAL_STATE);
                        reference.setSegmentSize(address, segment.length);

                        this.lastKnownAddress = address;
                        return address; //return it in transitional state
                    }
                    catch (IOException we) {
                        throw new WriteFailure("failed to write " + e.getMessage());
                    }

                }

            } catch (FileNotFoundException e) {
                throw new ReadFailure("failed to read " + e.getMessage());
            } catch (IOException e) {
                logger.error("failed to read " + e.getMessage(), e);
                throw new ReadFailure("failed to read " + e.getMessage(), e);
            }
        }
    }

    public long getFreeSegment(final int lengthRequired) throws ReadFailure, OutOfSpaceException, SpaceFragementedException {

        final Long foundEarly = reference.getSuitableSegment(lengthRequired);

        if(foundEarly != null) {
            return foundEarly;
        }

        final List<Long> freeSegments = new ArrayList<>();
        int freeSegmentsTotalSize = 0;

        try {

            long address = 0;

            while (true) {

                final int segmentLength;
                Byte segmentState = reference.getSegmentType(address);

                {

                    switch(segmentState) {
                        case BOUND_STATE:
                        case TRANSITIONAL_STATE: {

                            segmentLength = 0;
                            break;
                        }
                        case FREE_STATE: {
                            segmentLength = reference.getSegmentSize(address);
                            break;
                        }
                        case null :
                        default: {
                            //go to the next address
                            readRandom.seek(address);

                            final byte[] segmentSize = new byte[SEGMENT_LENGTH_BYTES_COUNT + 1];

                            try {
                                //read in the size of this segment
                                readRandom.readFully(segmentSize, 0, segmentSize.length);
                            }
                            catch (EOFException e) {
                                throw new OutOfSpaceException("out of free or fractured segments");
                            }

                            segmentState = segmentSize[4];//random.readByte();
                            segmentLength = bytesToInt(new byte[] {segmentSize[0], segmentSize[1], segmentSize[2], segmentSize[3]});

                            reference.setSegmentSize(address, segmentLength);
                            reference.setSegmentType(address, segmentState);
                            break;
                        }

                    }

                }

                if(segmentState == FREE_STATE) {

                    if(segmentLength >= lengthRequired) {
                        //if this segment is big enough and free return the address
                        return address;
                    }
                    else {

                        //if the section is free add it to our record of segments we visited that are free
                        freeSegments.add(address);
                        freeSegmentsTotalSize += segmentLength;

                        final int accumulatedMetaSize =
                            ((freeSegments.size() - 1 /* first item we keep meta data*/) * (SEGMENT_LENGTH_BYTES_COUNT + 1 + SEGMENT_LENGTH_BYTES_COUNT));

                        if((freeSegmentsTotalSize + accumulatedMetaSize) >= lengthRequired) {

                            for(int i = 1; i < freeSegments.size(); i++) {

                                final int oldSize = reference.getSegmentSize(freeSegments.get(i));

                                reference.setSegmentType(freeSegments.get(i), null);
                                reference.setSegmentSize(freeSegments.get(i), 0, oldSize);

                            }

                            throw new SpaceFragementedException(freeSegments.get(0), freeSegmentsTotalSize + accumulatedMetaSize);
                        }
                    }

                }
                else {

                    //not a continuous section of free segments clear out the record we are keeping and start over
                    freeSegments.clear();
                    freeSegmentsTotalSize = 0;

                }

                address += segmentLength + SEGMENT_LENGTH_BYTES_COUNT + 1 + SEGMENT_LENGTH_BYTES_COUNT;

            }

        }
        catch (FileNotFoundException e) {
            throw new IllegalArgumentException("something is wrong with the file ", e);
        }
        catch (IOException e) {
            throw new ReadFailure("failed to read file" + e.getMessage(), e);
        }

    }

    /**
     * warning needs to be transactional
     *
     * @param address
     * @param segmentSize
     * @throws WriteFailure
     * @throws ReadFailure
     */
    public void setSegmentSize(long address, int segmentSize) throws WriteFailure, ReadFailure {

        try {

            writeRandom.seek(address); //seek to the state byte of the new segment that doesn't exist yet
            writeRandom.write(intToByteArray(segmentSize));

            reference.setSegmentSize(address, segmentSize);

        }
        catch (IOException e) {
            throw new WriteFailure("failed to write " + e.getMessage());
        }

    }

    public byte[] readSegment(long address) throws ReadFailure {

        try {

            readRandom.seek(address);

            final byte[] segmentSize = new byte[SEGMENT_LENGTH_BYTES_COUNT];
            final byte[] segmentFillSize = new byte[SEGMENT_LENGTH_BYTES_COUNT];

            readRandom.readFully(segmentSize, 0, segmentSize.length);

            final byte segmentState = readRandom.readByte();
            readRandom.readFully(segmentFillSize, 0, segmentFillSize.length);
            final int segmentLength = bytesToInt(segmentSize);
            final int segmentFillLength = bytesToInt(segmentFillSize);

            reference.setSegmentType(address, segmentState);
            reference.setSegmentSize(address, segmentLength);

            if(segmentState == BOUND_STATE) {

                final byte [] returnVal =  new byte[segmentFillLength];

                readRandom.readFully(returnVal, 0, segmentFillLength);

                return returnVal;
            }
            else {
                throw new ReadFailure("segment at " + address + " is not bound it is " + segmentState);
            }

        }
        catch (FileNotFoundException e) {
            throw new ReadFailure("file not found: " + root, e);
        }
        catch (IOException e) {
            throw new ReadFailure("unknown read error " + e.getMessage(), e);
        }

    }

    public static void delete(String filename) {

        // Create a File object to represent the filename
        final File f = new File(filename);
        f.setWritable(true);
        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) fail("Delete: no such file or directory: " + filename);
        if (!f.canWrite()) fail("Delete: write protected: " + filename);
        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
            String[] children = f.list();

            for (int i = 0; i < children.length; i++) {
                boolean success = delete(new File(f, children[i]));
                if (!success) {
                    fail("Delete: child not deleted: " + filename);
                }
            }
        }
        // If we passed all the tests, then attempt to delete it
        boolean success = f.delete();
        // And throw an exception if it didn't work for some (unknown) reason.
        // For example, because of a bug with Java 1.1.1 on Linux,
        // directory deletion always fails
        if (!success) {

            f.deleteOnExit();
            fail("Delete: deletion failed");

        }

    }

    protected static void fail(String msg) throws IllegalArgumentException {
        logger.error("ERROR DELETING " + msg);
//        throw new IllegalArgumentException(msg);
    }

    public static boolean delete(File dir) {

        if (dir != null) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    boolean success = delete(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            dir.setWritable(true);

            final boolean returnVal = dir.delete();

            if(!returnVal) {
                dir.deleteOnExit();
            }
            return returnVal;
        }else {
            return false;
        }
    }

    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public static int bytesToInt(byte [] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] intToByteArray(int value) {
        // Allocate a ByteBuffer with capacity for 4 bytes (an int)
        ByteBuffer buffer = ByteBuffer.allocate(4);

        // Set the byte order (e.g., BIG_ENDIAN or LITTLE_ENDIAN)
        // BIG_ENDIAN is common for network protocols and human readability
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Put the integer into the buffer
        buffer.putInt(value);

        // Return the byte array representation of the buffer's content
        return buffer.array();
    }

    public static byte[] longToByteArray(long value) {
        // Allocate a ByteBuffer with capacity for 4 bytes (an int)
        ByteBuffer buffer = ByteBuffer.allocate(8);

        // Set the byte order (e.g., BIG_ENDIAN or LITTLE_ENDIAN)
        // BIG_ENDIAN is common for network protocols and human readability
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Put the integer into the buffer
        buffer.putLong(value);

        // Return the byte array representation of the buffer's content
        return buffer.array();
    }


    public void clear() throws ReadFailure, WriteFailure {

        try(final RandomAccessFile random = new RandomAccessFile(root, "rws")) {
            random.setLength(0);
        } catch (FileNotFoundException e) {
            throw new ReadFailure("file doesn't exist", e);
        } catch (IOException e) {
            throw new WriteFailure("unknown write error " + e.getMessage(), e);
        }

    }


    private static class SegmentReference {

        private final Map<Long, Byte> segmentTypes = new HashMap<>();
        private final TreeMap<Integer, Set<Long>> segmentBySize = new TreeMap<>(); //could use a tree structure here maybe
        private final Map<Long, Integer> segmentSizes = new HashMap<>();

        public void setSegmentType(long address, Byte type) {
            this.segmentTypes.put(address, type);
        }

        private Long getSuitableSegment(int segmentLength) {

            Map.Entry<Integer, Set<Long>> entry;

            while(true) {

                entry = segmentBySize.ceilingEntry(segmentLength);

                if(entry != null) {

                    if(entry.getKey() < segmentLength) {
                        throw new IllegalStateException();
                    }

                    for(final long address : entry.getValue()) {
                        //if we found a free entry that fits return it
                        if(segmentTypes.get(address) == FREE_STATE) {
                            //TODO this is sucking down the cpu but I'm not sure how to deal with it without adding a lot of complication.
                            return  address;
                        }
                    }

                }
                else {
                    return null;
                }

                //go bigger and try again.
                segmentLength = entry.getKey() + 1;

            }

        }

        public void setSegmentSize(long address, int size) {

            segmentSizes.put(address, size);

            Set<Long> addresses = segmentBySize.get(size);
            if(addresses == null) {
                addresses = new HashSet<>();
                segmentBySize.put(size, addresses);
            }

            addresses.add(address);

        }

        public void setSegmentSize(long address, int newSize, int oldSize) {




            {

                Set<Long> addresses = segmentBySize.get(oldSize);

                if (addresses == null) {
                    addresses = new HashSet<>();
                    segmentBySize.put(newSize, addresses);
                }

                addresses.remove(address);

            }

            if(newSize > 0) {

                segmentSizes.put(address, newSize);

                Set<Long> addresses = segmentBySize.get(newSize);
                if (addresses == null) {
                    addresses = new HashSet<>();
                    segmentBySize.put(newSize, addresses);
                }

                addresses.add(address);

            }
            else {
                segmentSizes.remove(address);
            }

        }

        public Byte getSegmentType(long address) {
            return segmentTypes.get(address);
        }

        public int getSegmentSize(long address) {
            return segmentSizes.get(address);
        }
    }
}
