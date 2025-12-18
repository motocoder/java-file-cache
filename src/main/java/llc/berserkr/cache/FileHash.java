package llc.berserkr.cache;


import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;


/**
 * 
 * 
 * this is a file backed hashing mechanism. 
 * 
 * Some of this class was derived from: https://code.google.com/p/jdbm2/ 
 *
 * **/
public abstract class FileHash<Key, Value> {
    
    private static final Logger logger = LoggerFactory.getLogger(FileHash.class);

    private final static int BUCKET_SIZE = 8;
    
    private final int hashSize;
    private final File file;

    private final HashDataManager<Key, Value> blobManager;
    
    
    public FileHash(
        final File file,
        final HashDataManager<Key, Value> blobManager,
        final int hashSize
    ) {
        
        this.hashSize = hashSize;
        
        if(file.isDirectory()) {
            throw new RuntimeException("hash file location must not be a directory");
        }
        
        this.blobManager = blobManager;        
        this.file = file;

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
    
    /**
     * 
     * @param key
     * @param blob
     */
    public void put(
      final Key key,
      final Value blob
    ) throws ReadFailure, WriteFailure {
        
        final long limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash size to our hash
      
        long hashedIndex = limitedHash * (BUCKET_SIZE); //multiply by bucket size so we know index.
             
        try {
          
            final RandomAccessFile random = new RandomAccessFile(file, "rws");
            
            try {
              
               final byte [] currentKeyIn = new byte[BUCKET_SIZE];
               random.seek(hashedIndex);
               
               //read in key at this hash location.
               final int read = random.read(currentKeyIn); //read in the current value
               
               if(read <= 0) { //file was too short, empty values weren't filled in
                   throw new RuntimeException("hash was not initialized properly");
               }
               
               final long blobIndex = SegmentedFile.bytesToLong(currentKeyIn);
               
               final Set<Pair<Key, Value>> toWrite = new HashSet<>();
               
               if(blobIndex >= 0) {
                   
                   //if there is already something hashed here, retrieve the hash bucket and add to it
                   final Set<Pair<Key, Value>> blobs = blobManager.getBlobsAt(blobIndex);
                   
                   if(blobs != null) {
                       toWrite.addAll(blobs);
                   }
                   
               }
                              
               //if this key was already in the bucket remove it.
               Pair<Key, Value> remove = null;
               
               for(final Pair<Key, Value> entry : toWrite) {
                   
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

                   final byte[] bytesIndex = SegmentedFile.longToByteArray(blobIndexAfterSet);
                   
                   random.seek(hashedIndex);
                   random.write(bytesIndex);
                   
               }
               
            }
            finally {
                random.close();
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
    
    public Value get(
      final Key key
    ) throws ReadFailure {
      
        final int limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash to our hash size
    
        int hashedIndex = limitedHash * (BUCKET_SIZE); //determine byte index
                
        try {
        
            final RandomAccessFile random = new RandomAccessFile(file, "rws");
          
            try {
                
               
                final byte [] currentKeyIn = new byte[BUCKET_SIZE];
                random.seek(hashedIndex);
             
                //read in key at this hash location.
                final int read = random.read(currentKeyIn);
                             
                if(read <= 0) { //file should have been initialized to hash size
                    throw new RuntimeException("hash was not initialized properly");
                }
             
                //convert the values read into an index
                long blobIndex = SegmentedFile.bytesToLong(currentKeyIn);
             
                Value returnVal = null;
               
                if(blobIndex >= 0) {
                 
                    //if there is values on this hash
                    final Set<Pair<Key, Value>> blobs = blobManager.getBlobsAt(blobIndex);
                                        
                    if(blobs == null) { //data corrupt lets remove our reference.
                        throw new ReadFailure("there should have been blobs at blobIndex");
                    }
                    else {
                    	                 
                        for(Pair<Key, Value> blob : blobs) {
                           
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
            finally {
                random.close();
            }
          
        }
        catch (FileNotFoundException e) {
            throw new ReadFailure("read value != written value");
        } 
        catch (IOException e) {
        
            logger.error("io exception in fileHash putHash", e);
            throw new RuntimeException("failed hash blob", e);
        
        }
        
    }

    public void remove(Key key) throws ReadFailure, WriteFailure {
                
        final long limitedHash = Math.abs(hashCode(key)) % hashSize; //limit the hash size
        
        long hashedIndex = limitedHash * (BUCKET_SIZE);
        
        try {
        
            final RandomAccessFile random = new RandomAccessFile(file, "rws");
          
            try {
            
               final byte [] currentKeyIn = new byte[BUCKET_SIZE];
               random.seek(hashedIndex);
             
               //read in key at this hash location.
               final int read = random.read(currentKeyIn);
             
               if(read <= 0) {
                   throw new RuntimeException("hash was not initialized properly");
               }
             
               //convert to an index
               long blobIndex = SegmentedFile.bytesToLong(currentKeyIn);
               
               //if there is a value on this hash, retrieve its value
               if(blobIndex >= 0) {
                 
                   Pair<Key, Value> removing = null;
                                      
                   final Set<Pair<Key, Value>> blobs = blobManager.getBlobsAt(blobIndex);
                   
                   if(blobs == null) { //data corrupt lets remove our reference.
                       
                       final byte[] bytesIndex = SegmentedFile.longToByteArray(-1L);
                       
                       random.seek(hashedIndex);
                       random.write(bytesIndex); 
                       
                   }
                   else {
                 
                       for(Pair<Key, Value> blob : blobs) {
                                                      
                           if(equals(blob.getOne(), key)) {
                               
                               //once we find a key that matches removed the value
                               removing = blob;
                               break;
                               
                           }
                           
                       }
                       
                   }
                   
                   if(removing != null) {
                       
                       //save the blobs after removing the value mapped to our key
                       blobs.remove(removing);
                       
                       if(blobs.size() == 0) {
                           
                           blobManager.eraseBlobs(blobIndex);
                           
                           //if blobs is empty, remove the hash as well.
                                                      
                           final byte[] bytesIndex = SegmentedFile.longToByteArray(-1L);
                         
                           random.seek(hashedIndex);
                           random.write(bytesIndex); 
                                                      
                       }
                       else {

                           final long newAddress = blobManager.setBlobs(blobIndex, blobs);

                           final byte[] bytesIndex = SegmentedFile.longToByteArray(newAddress);

                           random.seek(hashedIndex);
                           random.write(bytesIndex);

                       }
                       
                   }
                 
               }
             
            } finally {
                random.close();
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
    
    private void delete(int hashedIndex) throws ReadFailure, WriteFailure {
        
        try {
        
            final RandomAccessFile random = new RandomAccessFile(file, "rws");
          
            try {
            
               final byte [] currentKeyIn = new byte[BUCKET_SIZE];
               random.seek(hashedIndex);
             
               //read in key at this hash location.
               final int read = random.read(currentKeyIn);
             
               if(read <= 0) {
                   throw new RuntimeException("hash was not initialized properly");
               }
             
               //convert to an index
               long blobIndex = SegmentedFile.bytesToLong(currentKeyIn);
               
               //if there is a value on this hash, retrieve its value
               if(blobIndex >= 0) {
                           
                   blobManager.eraseBlobs(blobIndex);
                   
                   //if blobs is empty, remove the hash as well.
                                      
                   final byte[] bytesIndex = SegmentedFile.longToByteArray(-1L);
                 
                   random.seek(hashedIndex);
                   random.write(bytesIndex); 
                   
               }
             
            }
            finally {
                random.close();
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

    public void clear() throws ReadFailure, WriteFailure {
        
        //TODO make bulk delete
        try {
            for (int i = 0; i < hashSize; i++) {
                delete(i * (BUCKET_SIZE));
            }
        }
        catch (Exception e) {

            SegmentedFile.delete(file.getAbsolutePath());

            try {
                initFile();
            }
            catch (Exception e2) {
                logger.error("Failed to init file", e2);
            }

        }
        
        blobManager.clear();
        
    }

    public abstract int hashCode(Key key);
    public abstract boolean equals(Key key1, Key key2);
    
}
