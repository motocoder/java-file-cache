package llc.berserkr.cache;

import llc.berserkr.cache.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class SegmentedStreamingFile {

    private static final Logger logger = LoggerFactory.getLogger(SegmentedStreamingFile.class);

    //bytes at the beginning of each segment to construct into an int for the segment length
    public static final int SEGMENT_LENGTH_BYTES_COUNT = 4;
    private static final int START_OFFSET = 1024; //leave 1024 bytes for use of transactions

    //byte range -128 to 127
    public static final byte FREE_STATE = -128;
    public static final byte BOUND_STATE = -127;
    public static final byte TRANSITIONAL_STATE = -126;

    public static final byte WRITING_TRANSACTION = -125;
    public static final byte MERGE_TRANSACTION = -124;
    public static final byte ADD_END_TRANSACTION = -123;

    private final File root;
    private final SegmentReference reference = new SegmentReference();


    private long lastKnownAddress = START_OFFSET;
    private static final int WRITE_BUFFER_SIZE = 8192;

    /**
     *
     * File format for a forward linked list of segments that can vary in size and fill.
     *
     * first kilobyte is reserved for some meta data used to make sure write operations don't
     * break the structure on catastrophic failure.
     *
     * File format is such
     * [x, ... 1024][[x,x,x,x][x][x,x,x,x][x*n] * count of entries] 1024 transaction bytes (reserved), segmentSize(4bytes), type(1byte), fillSize(4bytes), payload(segmentSize bytes)
     *
     * TODO seperate reads/write so they can run on different threads at the same time.
     * TODO memory cache has an O^n issue that affect performance some when a lot of segments exist
     * TODO write the memory cache of addresses and state in C, would probably double the performance of this sytem because that's the bottle neck.
     *
     * @param root
     */
    public SegmentedStreamingFile(
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

            if(!root.exists()) {
                root.createNewFile();
            }

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("there's an issue with file for segments", e);
        } catch (IOException e) {
            throw new IllegalStateException("there's an issue with file for segments", e);
        }

        validateData();

    }

    /**
     * This method will reverse any open transaction
     */
    public void validateData() {

        try {

            final byte [] transactionBytes = readTransactionalBytes();

            switch (transactionBytes[0]) {
                case WRITING_TRANSACTION:
                {

                    long address = bytesToLong(
                        new byte[] {
                            transactionBytes[1],
                            transactionBytes[2],
                            transactionBytes[3],
                            transactionBytes[4],
                            transactionBytes[5],
                            transactionBytes[6],
                            transactionBytes[7],
                            transactionBytes[8]
                        }
                    );

                    writeState(address, FREE_STATE);

                    writeTransactionalBytes(new byte[] {});
                    break;
                }
                case MERGE_TRANSACTION:
                {

                    long address = bytesToLong(
                        new byte[] {
                            transactionBytes[1],
                            transactionBytes[2],
                            transactionBytes[3],
                            transactionBytes[4],
                            transactionBytes[5],
                            transactionBytes[6],
                            transactionBytes[7],
                            transactionBytes[8],
                        }
                    );

                    int length = bytesToInt(
                        new byte[] {
                            transactionBytes[9],
                            transactionBytes[10],
                            transactionBytes[11],
                            transactionBytes[12]
                        }
                    );

                    writeState(address, FREE_STATE);
                    write(address, new byte[length]);
                    setSegmentSize(address, length);
                    writeState(address, FREE_STATE);

                    writeTransactionalBytes(new byte[] {});
                    break;
                }
                case ADD_END_TRANSACTION:
                {
                    int length = bytesToInt(
                        new byte[] {
                            transactionBytes[1],
                            transactionBytes[2],
                            transactionBytes[3],
                            transactionBytes[4]
                        }
                    );

                    long endAddress = this.findEnd();

                    writeState(endAddress, FREE_STATE);
                    write(endAddress, new byte[length]);
                    writeState(endAddress, FREE_STATE);

                    writeTransactionalBytes(new byte[] {});

                }

            }

        }
        catch (ReadFailure e) {
            throw new IllegalStateException("couldn't validate data cache file is corrupt maybe", e);
        } catch (WriteFailure e) {
            throw new IllegalStateException("couldn't revert bad state data cache file is corrupt maybe", e);
        }

    }

    /**
     *
     * This method writes the bytes to the segrment at the address
     *
     * @param address - beginning address of segment
     * @param segment - data to be filled into segment
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public void write(long address, byte[] segment) throws ReadFailure, WriteFailure {

        final RandomAccessFile writeRandom = getWriter();

        try {

            writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT); //seek to the state byte of the new segment that doesn't exist yet
            writeRandom.write(new byte[]{TRANSITIONAL_STATE}); //note the caller needs to finalize the state
            writeRandom.write(intToByteArray(segment.length));//write the fill size
            writeRandom.write(segment); //write the payload

            reference.setSegmentType(address, TRANSITIONAL_STATE);

        }
        catch (IOException e) {
            throw new WriteFailure("failed to write " + e.getMessage());
        }
        finally {
            giveWriter(writeRandom);
        }
    }


    /**
     *
     * This method writes the bytes to the segrment at the address
     *
     * @param address - beginning address of segment
     * @param segment - data to be filled into segment
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public void write(long address, InputStream segment) throws ReadFailure, WriteFailure {


        final RandomAccessFile writeRandom = getWriter();

        try {

            writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT); //seek to the state byte of the new segment that doesn't exist yet
            writeRandom.write(new byte[]{TRANSITIONAL_STATE}); //note the caller needs to finalize the state
            writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT + 1 + SEGMENT_LENGTH_BYTES_COUNT);

            final byte [] buffer = new byte[WRITE_BUFFER_SIZE];
            int totalRead = 0;

            while(true) {

                final int read = segment.read(buffer);

                if(read > 0) {
                    totalRead += read;
                    writeRandom.write(buffer, 0, read);
                }
                else {
                    break;
                }

            }

            writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT + 1);

            writeRandom.write(intToByteArray(totalRead));//write the fill size

            reference.setSegmentType(address, TRANSITIONAL_STATE);



        }
        catch (IOException e) {
            throw new WriteFailure("failed to write " + e.getMessage());
        }
        finally {
            giveWriter(writeRandom);
        }

    }

    /**
     * Writes the state of the segment to the segment
     *
     * @param address
     * @param state
     * @throws WriteFailure
     * @throws ReadFailure
     */
    public void writeState(long address, byte state) throws WriteFailure, ReadFailure {

        final RandomAccessFile writeRandom = getWriter();

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
        finally {
            giveWriter(writeRandom);
        }
    }

    public long findEnd() throws ReadFailure {

        long address = lastKnownAddress; //shortcut to the last address if we already know it

        final RandomAccessFile readRandom = getReader();

        try {

            while (true) {

                //this only ever really runs through once the first time, then we know the last address
                //it's a forward linked list though so there's no way to go from the back to the front
                readRandom.seek(address);

                final byte[] segmentSize = new byte[SEGMENT_LENGTH_BYTES_COUNT + 1];

                try {

                    readRandom.readFully(segmentSize, 0, segmentSize.length); //read in the segment size, this blows an EOF if we are at the end

                    //didn't blow up so lets process the info for this segment and cache it.
                    final int segmentLength = bytesToInt(new byte [] {segmentSize[0],  segmentSize[1], segmentSize[2], segmentSize[3]});

                    final byte type = segmentSize[4]; //reading the state just to cache it

                    reference.setSegmentType(address, type);
                    reference.setSegmentSize(address, segmentLength);

                    address += segmentLength + SEGMENT_LENGTH_BYTES_COUNT + 1 + SEGMENT_LENGTH_BYTES_COUNT ;

                }
                catch (EOFException e) {
                    return address;
                }

            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("failed to read because some deleted the file " + e.getMessage());
        } catch (IOException e) {
            logger.error("failed to read " + e.getMessage(), e);
            throw new ReadFailure("failed to read " + e.getMessage(), e);
        }
        finally {
            giveReader(readRandom);
        }
    }

    /**
     * This method finds the last segment address which isn't allocated yet,
     * assigns it a size and state and puts the data provided into it.
     *
     * @param segment
     * @return
     * @throws WriteFailure
     * @throws ReadFailure
     */
    public long writeToEnd(final InputStream segment) throws WriteFailure, ReadFailure {

        long address = lastKnownAddress; //shortcut to the last address if we already know it

        final RandomAccessFile readRandom = getReader();
        final RandomAccessFile writeRandom = getWriter();

        try {
            while (true) {

                //this only ever really runs through once the first time, then we know the last address
                //it's a forward linked list though so there's no way to go from the back to the front
                readRandom.seek(address);

                final byte[] segmentSize = new byte[SEGMENT_LENGTH_BYTES_COUNT + 1];

                try {

                    readRandom.readFully(segmentSize, 0, segmentSize.length); //read in the segment size, this blows an EOF if we are at the end

                    //didn't blow up so lets process the info for this segment and cache it.
                    final int segmentLength = bytesToInt(new byte [] {segmentSize[0],  segmentSize[1], segmentSize[2], segmentSize[3]});

                    final byte type = segmentSize[4]; //reading the state just to cache it

                    reference.setSegmentType(address, type);
                    reference.setSegmentSize(address, segmentLength);

                    address += segmentLength + SEGMENT_LENGTH_BYTES_COUNT + 1 + SEGMENT_LENGTH_BYTES_COUNT ;

                }
                catch (EOFException e) {

                    //once we reach the end of the file we add a segment to it.
                    try {
                        writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT); //seek to the state byte of the new segment that doesn't exist yet
                        writeRandom.write(new byte[]{TRANSITIONAL_STATE}); //write the state first
                        writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT + 1 + /*fill length*/SEGMENT_LENGTH_BYTES_COUNT);

                        final byte [] buffer = new byte[WRITE_BUFFER_SIZE];
                        int totalRead = 0;

                        while(true) {

                            final int read = segment.read(buffer);

                            if(read > 0) {
                                totalRead += read;

                                writeRandom.write(buffer, 0, read);
                            }
                            else {
                                break;
                            }

                        }

//                        writeRandom.write(segment);
                        writeRandom.seek(address);
                        writeRandom.write(intToByteArray(totalRead));
                        writeRandom.seek(address + SEGMENT_LENGTH_BYTES_COUNT + 1);
                        writeRandom.write(intToByteArray(totalRead)); //data fill is same size as the segment since its a new segment

                        reference.setSegmentType(address, TRANSITIONAL_STATE);
                        reference.setSegmentSize(address, totalRead);

                        this.lastKnownAddress = address;
                        return address; //return it in transitional state
                    }
                    catch (IOException we) { //seperating the try/catch to send a write failure here
                        throw new WriteFailure("failed to write " + e.getMessage());
                    }

                }

            }
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("failed to read because some deleted the file " + e.getMessage());
        } catch (IOException e) {
            logger.error("failed to read " + e.getMessage(), e);
            throw new ReadFailure("failed to read " + e.getMessage(), e);
        }
        finally {
            giveReader(readRandom);
            giveWriter(writeRandom);
        }
    }

    /**
     * Searches the segmented file for a free segment to use.
     *
     * @param lengthRequired - size in bytes we need the segment to be
     * @return
     * @throws ReadFailure
     * @throws OutOfSpaceException - thrown if we reach the last item and still can't find anything
     * @throws NeedsSplitException - thrown if we reach the last item and still can't find anything
     * @throws SpaceFragementedException - thrown if we reach a framented group of segments that can be used by merging them
     */
    public long getFreeSegment(final int lengthRequired) throws ReadFailure, OutOfSpaceException, NeedsSplitException, SpaceFragementedException {

        //check cache for one we already know of
        final Long foundEarly = reference.getSuitableSegment(lengthRequired);

        //return it if we know about it
        if(foundEarly != null) {
            return foundEarly;
        }

        //used to keep track of continuous free segments so we can merge them
        final List<Long> freeSegments = new ArrayList<>();
        int freeSegmentsTotalSize = 0;

        try {

            //start looking from beginning
            long address = START_OFFSET;

            while (true) { //keep looking till we find a free segment

                final int segmentLength;
                Byte segmentState = reference.getSegmentType(address);

                {
                    //if we already know the segment state check it conditionally
                    switch(segmentState) {
                        case BOUND_STATE:
                        case TRANSITIONAL_STATE: {
                            //segment isnt free so it can't be used
                            segmentLength = 0;
                            break;
                        }
                        case FREE_STATE: {
                            //segment is free and cached so check the length
                            segmentLength = reference.getSegmentSize(address);
                            break;
                        }
                        case null :
                        default: {

                            final RandomAccessFile readRandom = getReader();

                            try {
                                //go to the next address since we didn't have it cached
                                readRandom.seek(address);

                                //read in segment length and type
                                final byte[] segmentSize = new byte[SEGMENT_LENGTH_BYTES_COUNT + 1];

                                try {
                                    //read in the size of this segment
                                    readRandom.readFully(segmentSize, 0, segmentSize.length);
                                } catch (EOFException e) {
                                    //throw out of space so we can add to the end in another call
                                    throw new OutOfSpaceException("out of free or fractured segments");
                                }

                                segmentState = segmentSize[4];//random.readByte();
                                segmentLength = bytesToInt(new byte[]{segmentSize[0], segmentSize[1], segmentSize[2], segmentSize[3]});

                                //cache the size and state
                                reference.setSegmentSize(address, segmentLength);
                                reference.setSegmentType(address, segmentState);
                                break;
                            }
                            finally {
                                giveReader(readRandom);
                            }
                        }

                    }

                }

                //free segment lets see if it fits
                if(segmentState == FREE_STATE) {

                    if(segmentLength > lengthRequired * 2) {
                        throw new NeedsSplitException(address, segmentLength);
                    }
                    else if(segmentLength >= lengthRequired) {
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

                            //remove the items we are merging from the cache, if the calling body cancels the merge the cache can be rebuilt
                            for(int i = 1; i < freeSegments.size(); i++) {

                                final int oldSize = reference.getSegmentSize(freeSegments.get(i));

                                reference.setSegmentType(freeSegments.get(i), null);

                                //if the user cancels merging i think this will make the segment invisible until the app restarts
                                //i dont really care though because that probably never happens and it will eventually recover the segments
                                reference.setSegmentSize(freeSegments.get(i), 0, oldSize);

                            }

                            //throw exception with the info on the fragmented segments for merge.
                            throw new SpaceFragementedException(freeSegments.get(0), freeSegmentsTotalSize + accumulatedMetaSize);
                        }
                    }

                }
                else {

                    //not a continuous section of free segments clear out the record we are keeping and start over
                    freeSegments.clear();
                    freeSegmentsTotalSize = 0;

                }

                //this segment isnt eligible go to the next one
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
     * Assigns the segments size
     *
     * @param address
     * @param segmentSize
     * @throws WriteFailure
     * @throws ReadFailure
     */
    public void setSegmentSize(long address, int segmentSize) throws WriteFailure, ReadFailure {

        final RandomAccessFile writeRandom = getWriter();

        try {

            writeRandom.seek(address); //seek to the state byte of the new segment that doesn't exist yet
            writeRandom.write(intToByteArray(segmentSize));

            reference.setSegmentSize(address, segmentSize);

        }
        catch (IOException e) {
            throw new WriteFailure("failed to write " + e.getMessage());
        }
        finally {
            giveWriter(writeRandom);
        }

    }

    private final List<RandomAccessFile> readers = new LinkedList<>();
    private final List<RandomAccessFile> writers = new LinkedList<>();

    private void giveReader(RandomAccessFile reader) {
        synchronized (readers) {
            readers.add(reader);
        }
    }
    private RandomAccessFile getReader() {
        synchronized (readers) {
            if(readers.size() == 0) {
                try {
                    return new RandomAccessFile(root, "r");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("hash file isn't working", e);
                }
            }
            else {
                return readers.removeFirst();
            }
        }
    }

    private void giveWriter(RandomAccessFile reader) {
        synchronized (writers) {
            writers.add(reader);
        }
    }
    private RandomAccessFile getWriter() {
        synchronized (writers) {
            if(writers.size() == 0) {
                try {
                    return new RandomAccessFile(root, "rws");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("hash file isn't working", e);
                }
            }
            else {
                return writers.removeFirst();
            }
        }
    }

    /**
     * Reads the data in bytes from the segment
     *
     * @param address
     * @return
     * @throws ReadFailure
     */
    public InputStream readSegment(long address) throws ReadFailure {

        try {

            final RandomAccessFile readRandom = getReader();

            readRandom.seek(address);

            //read in the size, fill size and type
            final byte [] toRead =  new byte[SEGMENT_LENGTH_BYTES_COUNT + 1 +  SEGMENT_LENGTH_BYTES_COUNT];

            readRandom.readFully(toRead, 0, toRead.length);

            final byte segmentState = toRead[4];

            final int segmentLength = bytesToInt(new byte[] {toRead[0], toRead[1], toRead[2], toRead[3]});//segmentSize);
            final int segmentFillLength = bytesToInt(new byte[] {toRead[5], toRead[6], toRead[7], toRead[8]});//bytesToInt(segmentFillSize);

            reference.setSegmentType(address, segmentState);
            reference.setSegmentSize(address, segmentLength);

            //check to make sure the segment is bound otherwise there's nothing to read
            if(segmentState == BOUND_STATE) {

                return new InputStream() {

                    int readFromAvailable = 0;

                    @Override
                    public int available() {
                        return segmentFillLength - readFromAvailable;
                    }

                    RandomAccessFile access = readRandom;

                    @Override
                    public int read() throws IOException {

                        if(available() > 0) {

                            readFromAvailable++;

                            return access.read();
                        }
                        else {
                            return -1;
                        }
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {

                        final int available = available();

                        if (available <= 0) {
                            return -1;
                        }

                        if (available < len) {
                            len = available;
                        }

                        final int readThisTime = access.read(b, off, len);
                        readFromAvailable += readThisTime;

                        return readThisTime;

                    }

                    @Override
                    public void close() throws IOException {
                        super.close();

                        this.access = null;

                        giveReader(readRandom);

                    }
                };

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

    public void writeTransactionalBytes(byte [] toWrite) throws WriteFailure, ReadFailure {

        if(toWrite.length > START_OFFSET) {
            throw new IllegalArgumentException("toWrite must be less than 1024 bytes");
        }

        if(toWrite.length < START_OFFSET) {

            final byte[] temp = new byte[START_OFFSET];

            Arrays.fill(temp, (byte) 0); //fill it with zeros

            System.arraycopy(toWrite, 0, temp, 0, toWrite.length);

            toWrite = temp;

        }

        final RandomAccessFile writeRandom = getWriter();

        try {

            writeRandom.seek(0); //seek to the state byte of the new segment that doesn't exist yet
            writeRandom.write(toWrite, 0, toWrite.length);

        }
        catch (IOException e) {
            logger.error("Failed ", e);
            throw new WriteFailure("failed to write " + e.getMessage());
        }
        finally {
            giveWriter(writeRandom);
        }

    }

    public byte[] readTransactionalBytes() throws ReadFailure {

        final RandomAccessFile readRandom = getReader();

        try {

            if(readRandom.length() < START_OFFSET) {

                final byte[] returnVal = new byte[START_OFFSET];
                Arrays.fill(returnVal, (byte) 0);

                return returnVal;

            }

            readRandom.seek(0);

            //read in the size, fill size and type
            final byte [] toRead =  new byte[START_OFFSET];

            readRandom.readFully(toRead, 0, toRead.length);

            return toRead;

        }
        catch (FileNotFoundException e) {
            throw new ReadFailure("file not found: " + root, e);
        }
        catch (IOException e) {
            throw new ReadFailure("unknown read error " + e.getMessage(), e);
        }
        finally {
            giveReader(readRandom);
        }

    }

    /**
     *
     * @param address
     * @return
     * @throws ReadFailure
     */
    public byte readSegmentState(long address) throws ReadFailure {

        final RandomAccessFile readRandom = getReader();

        try {

            readRandom.seek(address);

            //read in the size, fill size and type
            final byte [] toRead =  new byte[SEGMENT_LENGTH_BYTES_COUNT + 1 +  SEGMENT_LENGTH_BYTES_COUNT];

            readRandom.readFully(toRead, 0, toRead.length);

            final byte segmentState = toRead[4];

            final int segmentLength = bytesToInt(new byte[] {toRead[0], toRead[1], toRead[2], toRead[3]});//segmentSize);

            reference.setSegmentType(address, segmentState);
            reference.setSegmentSize(address, segmentLength);

            return segmentState;

        }
        catch (FileNotFoundException e) {
            throw new ReadFailure("file not found: " + root, e);
        }
        catch (IOException e) {
            throw new ReadFailure("unknown read error " + e.getMessage(), e);
        }
        finally {
            giveReader(readRandom);
        }

    }

    /**
     * Class that holds memory cache to all the segment info
     */
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

    /************************ UTILITY METHODS ***************************/

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

}
