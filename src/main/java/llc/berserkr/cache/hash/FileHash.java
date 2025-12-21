package llc.berserkr.cache.hash;


import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
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
 * Some of this class was derived from: https://code.google.com/p/jdbm2/ 
 *
 * **/
public class FileHash {
    
    private static final Logger logger = LoggerFactory.getLogger(FileHash.class);

    private final static int BUCKET_SIZE = 8;
    
    private final int hashSize;
    private final File file;

    private final Map<Long, HashLocks> hashLocks = new ConcurrentHashMap<>();

    private final SegmentedBytesDataManager blobManager;
    private final LocalRandomAccess localAccess;

    public FileHash(
        final File file,
        final File dataFile,
        final int hashSize
    ) {

        this.blobManager = new SegmentedBytesDataManager(dataFile);
        
        this.hashSize = hashSize;
        
        if(file.isDirectory()) {
            throw new RuntimeException("hash file location must not be a directory");
        }

        this.file = file;
        this.localAccess = new LocalRandomAccess(file);

        //if the file doesn't exist, initialize an empty hash of the desired size
        if(!file.exists()) {
            initFile();
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

    private final HashLocks.SharedWriteLocks sharedLocks = new HashLocks.SharedWriteLocks();

    private synchronized HashLocks getLock(long key) {

        HashLocks returnVal = hashLocks.get(key);

        if(returnVal == null) {
            returnVal = new HashLocks(sharedLocks);
            hashLocks.put(key, returnVal);
        }

        return returnVal;

    }

    /**
     * 
     * @param key
     * @param blob
     */
    public void put(
      final byte [] key,
      final byte [] blob
    ) throws ReadFailure, WriteFailure {
        
        final long limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash size to our hash
      
        long hashedIndex = limitedHash * (BUCKET_SIZE); //multiply by bucket size so we know index.
             
        try {

            final RandomAccessFile randomRead = localAccess.getReader();
            final RandomAccessFile randomWrite = localAccess.getWriter();

            final HashLocks lock = getLock(hashedIndex);

            try {

                lock.getLock(HashLocks.LockType.WRITER);

               final byte [] currentKeyIn = new byte[BUCKET_SIZE];
               randomRead.seek(hashedIndex);

               //read in key at this hash location.
               final int read = randomRead.read(currentKeyIn); //read in the current value

               if(read <= 0) { //file was too short, empty values weren't filled in
                   throw new RuntimeException("hash was not initialized properly");
               }

               final long blobIndex = SegmentedStreamingFile.bytesToLong(currentKeyIn);

               final Set<Pair<byte [], byte []>> toWrite = new HashSet<>();

               if(blobIndex >= 0) {

                   //if there is already something hashed here, retrieve the hash bucket and add to it
                   final Set<Pair<byte [], byte []>> blobs = blobManager.getBlobsAt(blobIndex);

                   if(blobs != null) {
                       toWrite.addAll(blobs);
                   }

               }

               //if this key was already in the bucket remove it.
               Pair<byte [], byte []> remove = null;

               for(final Pair<byte [], byte []> entry : toWrite) {

                   if(equals(entry.getOne(), key)) {
                       remove = entry;
                       break;
                   }

               }

               toWrite.remove(remove);

               //then add it to the bucket again
               toWrite.add(
                   new Pair<>(key, blob)
               );

               //save the new values, if a new index is allocated, store it in the hash
               final long blobIndexAfterSet = blobManager.setBlobs(blobIndex, toWrite);

               if(blobIndexAfterSet != blobIndex) {

                   final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(blobIndexAfterSet);

                   randomWrite.seek(hashedIndex);
                   randomWrite.write(bytesIndex);

               }

            } catch (InterruptedException e) {
                throw new WriteFailure("failed to write interrupted", e);
            } finally {

                lock.releaseLock(HashLocks.LockType.WRITER);
                localAccess.giveReader(randomRead);
                localAccess.giveWriter(randomWrite);

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
    
    public byte [] get(
      final byte [] key
    ) throws ReadFailure {
      
        final int limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash to our hash size
    
        int hashedIndex = limitedHash * (BUCKET_SIZE); //determine byte index

        final RandomAccessFile randomRead = localAccess.getReader();

        final HashLocks lock = getLock(hashedIndex);

        try {

            lock.getLock(HashLocks.LockType.READER);
            final byte [] currentKeyIn = new byte[BUCKET_SIZE];
            randomRead.seek(hashedIndex);

            //read in key at this hash location.
            final int read = randomRead.read(currentKeyIn);

            if(read <= 0) { //file should have been initialized to hash size
                throw new RuntimeException("hash was not initialized properly");
            }

            //convert the values read into an index
            long blobIndex = SegmentedStreamingFile.bytesToLong(currentKeyIn);

            byte [] returnVal = null;

            if(blobIndex >= 0) {

                //if there is values on this hash
                final Set<Pair<byte [], byte []>> blobs = blobManager.getBlobsAt(blobIndex);

                if(blobs == null) { //data corrupt lets remove our reference.
                    throw new ReadFailure("there should have been blobs at blobIndex");
                }
                else {

                    for(Pair<byte [], byte []> blob : blobs) {

                        if(equals(blob.getOne(), key)) {

                            //return the value mapped to this key
                            returnVal = blob.getTwo();
                            break;

                        }

                    }

                }

            }
            //else the returnval will be null

            return returnVal;
          
        }
        catch (FileNotFoundException e) {
            throw new ReadFailure("read value != written value");
        } 
        catch (IOException e) {
        
            logger.error("io exception in fileHash putHash", e);
            throw new ReadFailure("failed hash blob", e);
        
        } catch (InterruptedException e) {
            throw new ReadFailure("failed to read interrupted", e);
        } finally {
            lock.releaseLock(HashLocks.LockType.READER);
            localAccess.giveReader(randomRead);
        }
        
    }

    public void remove(byte [] key) throws ReadFailure, WriteFailure {
                
        final long limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash size
        
        long hashedIndex = limitedHash * (BUCKET_SIZE);

        final RandomAccessFile randomRead = localAccess.getReader();
        final RandomAccessFile randomWrite = localAccess.getWriter();

        final HashLocks lock = getLock(hashedIndex);

        try {
            lock.getLock(HashLocks.LockType.WRITER);

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

                Pair<byte [], byte []> removing = null;

                final Set<Pair<byte [], byte []>> blobs = blobManager.getBlobsAt(blobIndex);

                if (blobs == null) { //data corrupt lets remove our reference.

                    final byte[] bytesIndex = SegmentedStreamingFile.longToByteArray(-1L);

                    randomWrite.seek(hashedIndex);
                    randomWrite.write(bytesIndex);

                } else {

                    for (Pair<byte [], byte []> blob : blobs) {

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

        }
        catch (FileNotFoundException e) {
        
            logger.error("file not found in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        } 
        catch (IOException e) {
        
            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        
        } catch (InterruptedException e) {
            throw new WriteFailure("failed to write iteerrupted", e);
        } finally {

            lock.releaseLock(HashLocks.LockType.WRITER);

            localAccess.giveReader(randomRead);
            localAccess.giveWriter(randomWrite);

        }
                
    }
    
    private void delete(int hashedIndex) throws ReadFailure, WriteFailure {

        final RandomAccessFile randomRead = localAccess.getReader();
        final RandomAccessFile randomWrite = localAccess.getWriter();

        final HashLocks lock = getLock(hashedIndex);

        try {

            lock.getLock(HashLocks.LockType.WRITER);

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
          
        }
        catch (FileNotFoundException e) {
        
            logger.error("file not found in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        } 
        catch (IOException e) {
        
            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        
        } catch (InterruptedException e) {
            throw new WriteFailure("failed to write interrupted", e);
        } finally {
            lock.releaseLock(HashLocks.LockType.WRITER);
            localAccess.giveReader(randomRead);
            localAccess.giveWriter(randomWrite);
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
