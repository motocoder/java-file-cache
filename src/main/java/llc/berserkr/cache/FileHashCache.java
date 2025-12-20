package llc.berserkr.cache;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FileHashCache implements Cache<byte [], InputStream> {

    private static final Logger logger = LoggerFactory.getLogger(FileHashCache.class);
    
    private final StreamingFileHash hash;

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

        hash = new StreamingFileHash(hashFile, blobFile, dataFile, tempFolder, hashSize);

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
    public InputStream get(byte [] key) throws ResourceException {
        
        try {
            return hash.get(key);
        } catch (ReadFailure e) {
            throw new ResourceException("failure", e);
        }

    }

    @Override
    public List<InputStream> getAll(List<byte[]> bytes) throws ResourceException {
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
    public void put(byte [] key, InputStream value) throws ResourceException {
        
        try {
            
            hash.put(key, value);
            
        }
        catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
        
    }

}
