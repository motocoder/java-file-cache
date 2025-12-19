package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class SingleFileCache<Key> implements Cache<Key, InputStream> {

    private final File rootFolder;
    private final int hashSize;

    /**
     * 
     * @param rootFolder
     * @param hashSize
     */
    public SingleFileCache(
        final File rootFolder,
        final int hashSize,
        final String uniqueName
    ) {
        
        this.hashSize = hashSize;
        this.rootFolder = rootFolder;
        
        if(!rootFolder.exists()) {
            rootFolder.mkdirs();
        }
        
        if(!rootFolder.isDirectory()) {
            throw new RuntimeException("must either not exist or be a directory: " + rootFolder.getAbsolutePath());
        }
        
    }

    @Override
    public boolean exists(Key key) throws ResourceException {
        return false;
    }

    @Override
    public InputStream get(Key key) throws ResourceException {
        return null;
    }

    @Override
    public List<InputStream> getAll(List<Key> keys) throws ResourceException {
        return null;
    }

    @Override
    public void clear() {
        
    }

    @Override
    public void remove(Key key) {
        
    }

    @Override
    public void put(Key key, InputStream value) {
        
    }
  

}
