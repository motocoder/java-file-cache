package llc.berserkr.cache.hash;

import llc.berserkr.cache.Cache;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FileHashCache implements Cache<byte [], byte []> {

    private static final Logger logger = LoggerFactory.getLogger(FileHashCache.class);

    private final FileHash hash;
//    private boolean blockAll;

    public FileHashCache(
        final File dataFolder
    ) throws IOException {
        this(dataFolder, 10000);

    }

    public FileHashCache(
        final File dataFolder,
        final int hashSize
    ) throws IOException {

        dataFolder.mkdirs();

        if(!dataFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid data folder");
        }

        final File hashFile = new File(dataFolder, "hash");
        final File blobFile = new File(dataFolder, "blob");
        final File dataFile = new File(dataFolder, "data");
        final File tempFolder = new File(dataFolder, "temp");

        tempFolder.mkdirs();

        if(!tempFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid temp folder");
        }

        hash = new FileHash(hashFile, dataFile, hashSize);

    }
    @Override
    public boolean exists(byte [] key) throws ResourceException {

        getLock(false,false);

        try {

            final byte [] is = hash.get(key);

            if(is != null) {
                return true;
            }

            return false;

        } catch (ReadFailure e) {
            throw new ResourceException("failure", e);
        }
        finally {
            giveLock(false);
        }

    }

    @Override
    public byte [] get(byte [] key) throws ResourceException {

        getLock(false,false);
        
        try {
            return hash.get(key);
        } catch (ReadFailure e) {
            giveLock(false);
            throw new ResourceException("failure", e);
        }
        finally {
            giveLock(false);
        }

    }

    @Override
    public List<byte []> getAll(List<byte[]> bytes) throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clear() throws ResourceException {

        getLock(true, true);
        try {
            hash.clear();
        } catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
        finally {
            giveLock(true);
        }

    }

    @Override
    public void remove(byte [] key) throws ResourceException {

        getLock(true, true);
        try {
            hash.remove(key);
        } catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
        finally {
            giveLock(true);
        }

    }

    @Override
    public void put(byte [] key, byte [] value) throws ResourceException {

        getLock(true, true);
        try {
            
            hash.put(key, value);
            
        }
        catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
        finally {
            giveLock(true);
        }
        
    }

    private final Object lock = new Object();
    private int lockCount = 0;

    public void giveLock(boolean unBlockAll) {

//        synchronized (lock) {
//            lockCount--;
//
//            if(unBlockAll) {
//                blockAll = false;
//                lock.notifyAll();
//            }
//
//            if(lockCount == 0) {
//                lock.notifyAll();
//            }
//
//        }
    }

    private void getLock(boolean blockAll, boolean block) {

//        synchronized (lock) {
//
//            //if we are blocking to lock it, or everyone is blocked
//            if(block || this.blockAll) {
//
//                //while we are blocking and locks are out or everyone is blocked
//                while((block && lockCount != 0)|| this.blockAll) {
//                    try {
//                        lock.wait();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//
//            this.blockAll = blockAll;
//
//            lockCount++;
//
//        }

    }

}
