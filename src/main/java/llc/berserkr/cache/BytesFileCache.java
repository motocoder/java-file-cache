package llc.berserkr.cache;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.exception.WriteFailure;
import llc.berserkr.cache.hash.FileHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BytesFileCache implements Cache<byte [], byte []> {

    private static final Logger logger = LoggerFactory.getLogger(BytesFileCache.class);

    private final FileHash hash;

    public BytesFileCache(
        final File dataFolder
    ) throws IOException {
        this(dataFolder, 10000);
    }

    public BytesFileCache(
        final File dataFolder,
        final int hashSize
    ) throws IOException {

        dataFolder.mkdirs();

        if(!dataFolder.isDirectory()) {
            throw new IllegalArgumentException("Invalid data folder");
        }

        final File hashFile = new File(dataFolder, "hash");
        final File segmentFile = new File(dataFolder, "segments");

        hash = new FileHash(hashFile, segmentFile, hashSize);

    }
    @Override
    public boolean exists(byte [] key) throws ResourceException {
    	
        try {
            return hash.get(key) != null;
        } catch (ReadFailure e) {
            throw new ResourceException("failure", e);
        }

    }

    @Override
    public byte [] get(byte [] key) throws ResourceException {
        
        try {
            return hash.get(key);
        } catch (ReadFailure e) {
            throw new ResourceException("failure", e);
        }

    }

    @Override
    public List<byte []> getAll(List<byte[]> bytes) throws ResourceException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clear() throws ResourceException {
        
        try {
            hash.clear();
        } catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
    }

    @Override
    public void remove(byte [] key) throws ResourceException {
                
        try {
            hash.remove(key);
        } catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }

    }

    @Override
    public void put(byte [] key, byte [] value) throws ResourceException {
        
        try {
            hash.put(key, value);
        }
        catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
        
    }

}
