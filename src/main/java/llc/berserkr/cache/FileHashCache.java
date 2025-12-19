package llc.berserkr.cache;

import llc.berserkr.cache.exception.CacheException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class FileHashCache implements Cache<byte [], byte []> {

    private static final Logger logger = LoggerFactory.getLogger(FileHashCache.class);
    
    private final FileHash<byte [], byte []> hash;

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

        final File managerDataFile = new File(dataFolder, "data");
        if(!managerDataFile.exists()) {
            managerDataFile.createNewFile();
        }

        final SegmentedHashDataManager manager = new SegmentedHashDataManager(managerDataFile);
        hash = new FileHash<>(hashFile, manager, hashSize) {
            @Override
            public int hashCode(byte[] bytes) {
                return Arrays.hashCode(bytes);
            }

            @Override
            public boolean equals(byte[] key1, byte[] key2) {
                return Arrays.equals(key1, key2);
            }
        };

    }
    @Override
    public boolean exists(byte [] key) throws CacheException {
    	
        try {
            return hash.get(key) != null;
        } catch (ReadFailure e) {
            throw new CacheException("failure", e);
        }

    }

    @Override
    public byte [] get(byte [] key) throws CacheException {
        
        try {
            
            final byte [] returnVal = hash.get(key);
            
            return returnVal;
            
        } catch (ReadFailure e) {
            throw new CacheException("failure", e);
        }

    }

    @Override
    public void clear() throws CacheException {
        
        try {
            hash.clear();
        } catch (ReadFailure | WriteFailure e) {
            throw new CacheException("failure", e);
        }
    }

    @Override
    public void remove(byte [] key) throws CacheException {
                
        try {
            hash.remove(key);
        } catch (ReadFailure | WriteFailure e) {
            throw new CacheException("failure", e);
        }

    }

    @Override
    public void put(byte [] key, byte [] value) throws CacheException {
        
        try {
            
            hash.put(key, value);
            
        }
        catch (ReadFailure | WriteFailure e) {
            throw new CacheException("failure", e);
        }
        
    }

}
