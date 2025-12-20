package llc.berserkr.cache;


import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
import llc.berserkr.cache.util.WrappingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * 
 * this is a file backed hashing mechanism. 
 *
 * TODO make read/write seperated for threading reasons (should be able to read from stuff you aren't writting to while writting to other items)
 *
 * Some of this class was derived from: https://code.google.com/p/jdbm2/ 
 *
 * **/
public class StreamingFileHash {

    private static final Logger logger = LoggerFactory.getLogger(StreamingFileHash.class);

    private final static int BUCKET_SIZE = 8;

    private final int hashSize;
    private final File file;

    private final Map<Long, Object> hashLocks = new ConcurrentHashMap<>();

    private final BlobsSegmentedStreamingHashDataManager blobManager;
//    private final RandomAccessFile randomRead;
//    private final RandomAccessFile randomWrite;
    private final StreamsSegmentedStreamingHashDataManager dataManager;
    private final File tempDirectory;

    public StreamingFileHash(
        final File file,
        final File blobFile,
        final File dataFile,
        final File tempDirectory,
        final int hashSize
    ) {
        
        this.hashSize = hashSize;
        
        if(file.isDirectory()) {
            throw new RuntimeException("hash file location must not be a directory");
        }

        for(long i = 0; i < hashSize; i++) {
            hashLocks.put(i * (BUCKET_SIZE), new Object());
        }

        this.tempDirectory = tempDirectory;
        this.blobManager = new BlobsSegmentedStreamingHashDataManager(blobFile);
        this.dataManager = new StreamsSegmentedStreamingHashDataManager(dataFile, tempDirectory);
        this.file = file;

        //if the file doesn't exist, initialize an empty hash of the desired size
        if(!file.exists()) {
            initFile();
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
                    return new RandomAccessFile(file, "r");
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
                    return new RandomAccessFile(file, "rws");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("hash file isn't working", e);
                }
            }
            else {
                return writers.removeFirst();
            }
        }
    }


    private void initFile() {

        try {

            final OutputStream output = new FileOutputStream(file);

            try {

                final byte [] bytes = new byte [1024];

                Arrays.fill(bytes, (byte)-1);

                final int max = hashSize * BUCKET_SIZE;

                for(int i = 0; i < max; i += 1024) {
                    output.write(bytes);
                }

                output.flush();

            }
            finally {
                output.close();
            }

        }
        catch (FileNotFoundException e) {

            logger.error("Failed to establish file hash", e);
            throw new RuntimeException("failed to establish file hash", e);

        }
        catch (IOException e) {

            logger.error("Failed to establish file hash 2", e);
            throw new RuntimeException("failed to establish file hash 2", e);

        }

    }

    public void giveLock(long key, Object lock) {

//        synchronized (hashLocks) {
//            hashLocks.put(key, lock);
//            hashLocks.notifyAll();
//        }
    }

    private Object getLock(long key) {

        return new Object();

//        synchronized (hashLocks) {
//
//            while(true) {
//                if (hashLocks.containsKey(key)) {
//                    return hashLocks.remove(key);
//                } else {
//                    try {
//                        hashLocks.wait();
//                    } catch (InterruptedException e) {
//                        logger.warn("Interrupted while waiting for lock", e);
//                    }
//                }
//            }
//
//        }

    }
    /**
     * 
     * @param key
     * @param blob
     */
    public void put(
      final byte [] key,
      final InputStream blob
    ) throws ReadFailure, WriteFailure {
        
        final long limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash size to our hash
      
        long hashedIndex = limitedHash * (BUCKET_SIZE); //multiply by bucket size so we know index.
             
        try {

           final RandomAccessFile randomRead = getReader();
           final RandomAccessFile randomWrite = getWriter();

           final Object lock = getLock(hashedIndex);

           try {

               final byte[] currentKeyIn = new byte[BUCKET_SIZE];
               randomRead.seek(hashedIndex);

               //read in key at this hash location.
               final int read = randomRead.read(currentKeyIn); //read in the current value

               if (read <= 0) { //file was too short, empty values weren't filled in
                   throw new RuntimeException("hash was not initialized properly");
               }

               final long blobIndex = SegmentedStreamingFile.bytesToLong(currentKeyIn);

               final Set<Pair<byte[], Long>> toWrite = new HashSet<>();

               if (blobIndex >= 0) {

                   //if there is already something hashed here, retrieve the hash bucket and add to it
                   final Set<Pair<byte[], Long>> blobs = blobManager.getBlobsAt(blobIndex);

                   if (blobs != null) {
                       toWrite.addAll(blobs);
                   }

               }

               //if this key was already in the bucket remove it.
               Pair<byte[], Long> remove = null;

               for (final Pair<byte[], Long> entry : toWrite) {

                   if (equals(entry.getOne(), key)) {
                       remove = entry;
                       break;
                   }

               }

               toWrite.remove(remove);

               final long oldAddress;

               if (remove != null) {
                   oldAddress = remove.getTwo();
               } else {
                   oldAddress = -1;
               }
               final long newAddress = dataManager.setBlobs(oldAddress, blob);

               //then add it to the bucket again
               toWrite.add(
                   new Pair<byte[], Long>(key, newAddress)
               );

               //save the new values, if a new index is allocated, store it in the hash
               final long blobIndexAfterSet = blobManager.setBlobs(blobIndex, toWrite);

               if (blobIndexAfterSet != blobIndex) {

                   final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(blobIndexAfterSet);

                   randomWrite.seek(hashedIndex);
                   randomWrite.write(bytesIndex);

               }
           }
           finally {

               giveReader(randomRead);
               giveWriter(randomWrite);
               giveLock(hashedIndex, lock);
           }

            
        }
        catch (FileNotFoundException e) {
          
            logger.error("file not found in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        } 
        catch (IOException e) {
          
            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
          
        }
            
    }
    
    public InputStream get(
      final byte [] key
    ) throws ReadFailure, WriteFailure {
      
        final int limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash to our hash size
    
        int hashedIndex = limitedHash * (BUCKET_SIZE); //determine byte index

        final RandomAccessFile randomRead = getReader();

        final Object lock = getLock(hashedIndex);
        try {

            final byte[] currentKeyIn = new byte[BUCKET_SIZE];
            randomRead.seek(hashedIndex);

            //read in key at this hash location.
            final int read = randomRead.read(currentKeyIn);

            if (read <= 0) {
                giveLock(hashedIndex, lock);
                //file should have been initialized to hash size
                throw new RuntimeException("hash was not initialized properly");
            }

            //convert the values read into an index
            long blobIndex = SegmentedStreamingFile.bytesToLong(currentKeyIn);

            Long returnVal = null;

            if (blobIndex >= 0) {

                //if there is values on this hash
                final Set<Pair<byte[], Long>> blobs = blobManager.getBlobsAt(blobIndex);

                if (blobs == null) {
                    giveLock(hashedIndex, lock);
                    //data corrupt lets remove our reference.
                    throw new ReadFailure("there should have been blobs at blobIndex");
                } else {

                    for (Pair<byte[], Long> blob : blobs) {

                        if (equals(blob.getOne(), key)) {

                            //return the value mapped to this key
                            returnVal = blob.getTwo();
                            break;

                        }

                    }

                }

            }

            if (returnVal == null) {
                giveLock(hashedIndex, lock);
                return null;
            }

            return new WrappingInputStream(dataManager.getBlobsAt(returnVal)) {

                private Object myLock = lock;

                private synchronized void giveLockOnce() {

                    if(myLock == null) {
                        return;
                    }

                    giveLock(hashedIndex, myLock);

                    myLock = null;

                }
                @Override
                public int available() throws IOException {

                    int returnAvail = super.available();

                    if(returnAvail == 0) {
                        giveLockOnce();
                    }

                    return returnAvail;

                }

                @Override
                public void close() throws IOException {
                    super.close();
                    giveLockOnce();
                }
            };

        } catch (FileNotFoundException e) {
            throw new ReadFailure("read value != written value");
        } catch (IOException e) {

            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);

        }
        finally {
            giveReader(randomRead);
        }
        
    }

    public void remove(byte [] key) throws ReadFailure, WriteFailure {
                
        final long limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash size
        
        long hashedIndex = limitedHash * (BUCKET_SIZE);

        final Object lock = getLock(hashedIndex);

        final RandomAccessFile randomRead = getReader();
        final RandomAccessFile randomWrite = getWriter();

        try {

            final byte[] currentKeyIn = new byte[BUCKET_SIZE];
            randomRead.seek(hashedIndex);

            //read in key at this hash location.
            final int read = randomRead.read(currentKeyIn);

            if (read <= 0) {
                throw new RuntimeException("hash was not initialized properly");
            }

            //convert to an index
            long blobIndex = SegmentedStreamingFile.bytesToLong(currentKeyIn);

            //if there is a value on this hash, retrieve its value
            if (blobIndex >= 0) {

                Pair<byte[], Long> removing = null;

                final Set<Pair<byte[], Long>> blobs = blobManager.getBlobsAt(blobIndex);

                if (blobs == null) { //data corrupt lets remove our reference.

                    final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(-1L);

                    randomWrite.seek(hashedIndex);
                    randomWrite.write(bytesIndex);

                } else {

                    for (Pair<byte[], Long> blob : blobs) {

                        if (equals(blob.getOne(), key)) {

                            //once we find a key that matches removed the value
                            removing = blob;
                            break;

                        }

                    }

                }

                if (removing != null) {

                    //save the blobs after removing the value mapped to our key
                    blobs.remove(removing);

                    if (blobs.size() == 0) {

                        blobManager.eraseBlobs(blobIndex);

                        //if blobs is empty, remove the hash as well.

                        final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(-1L);

                        randomWrite.seek(hashedIndex);
                        randomWrite.write(bytesIndex);

                    } else {

                        final long newAddress = blobManager.setBlobs(blobIndex, blobs);

                        final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(newAddress);

                        randomWrite.seek(hashedIndex);
                        randomWrite.write(bytesIndex);

                    }

                }

            }

        } catch (FileNotFoundException e) {

            logger.error("file not found in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        } catch (IOException e) {

            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);

        }
        finally {
            giveReader(randomRead);
            giveWriter(randomWrite);
            giveLock(hashedIndex, lock);
        }
                
    }
    
    private void delete(int hashedIndex) throws ReadFailure, WriteFailure {

        final RandomAccessFile randomRead = getReader();
        final RandomAccessFile randomWrite = getWriter();

        final Object lock = getLock(hashedIndex);

        try {

            final byte[] currentKeyIn = new byte[BUCKET_SIZE];
            randomRead.seek(hashedIndex);

            //read in key at this hash location.
            final int read = randomRead.read(currentKeyIn);

            if (read <= 0) {
                throw new RuntimeException("hash was not initialized properly");
            }

            //convert to an index
            long blobIndex = SegmentedStreamingFile.bytesToLong(currentKeyIn);

            //if there is a value on this hash, retrieve its value
            if (blobIndex >= 0) {

                blobManager.eraseBlobs(blobIndex);

                //if blobs is empty, remove the hash as well.

                final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(-1L);

                randomWrite.seek(hashedIndex);
                randomWrite.write(bytesIndex);

            }

        } catch (FileNotFoundException e) {

            logger.error("file not found in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        } catch (IOException e) {

            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);

        }
        finally {
            giveReader(randomRead);
            giveWriter(randomWrite);
            giveLock(hashedIndex, lock);
        }


    }

    public void clear() throws ReadFailure, WriteFailure {
        
        //TODO make bulk delete
        try {
            for (int i = 0; i < hashSize; i++) {
                delete(i * (BUCKET_SIZE));
            }
        }
        catch (Exception e) {

            SegmentedStreamingFile.delete(file.getAbsolutePath());

            try {
                initFile();
            }
            catch (Exception e2) {
                logger.error("Failed to init file", e2);
            }

        }
        
        blobManager.clear();
        
    }

    public int hashCode(byte[] bytes) {
        return Arrays.hashCode(bytes);
    }

    public boolean equals(byte[] key1, byte[] key2) {
        return Arrays.equals(key1, key2);
    }
}
