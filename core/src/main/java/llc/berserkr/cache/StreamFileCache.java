package llc.berserkr.cache;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.exception.WriteFailure;
import llc.berserkr.cache.hash.StreamingFileHash;
import llc.berserkr.cache.util.WrappingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class StreamFileCache implements Cache<byte [], InputStream> {

    private static final Logger logger = LoggerFactory.getLogger(StreamFileCache.class);
    
    private final StreamingFileHash hash;

    public StreamFileCache(
        final File dataFolder
    ) throws IOException {
        this(dataFolder, 10000);
    }

    public StreamFileCache(
        final File dataFolder,
        final int hashSize
    ) {

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

        if(key == null || key.length == 0) {
            throw new IllegalArgumentException("invalid key length");
        }

        try {

            final InputStream is = hash.get(key);

            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new ResourceException(e);
                }

                return true;
            }

            return false;

        } catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }

    }

    @Override
    public InputStream get(byte [] key) throws ResourceException {

        if(key == null || key.length == 0) {
            throw new IllegalArgumentException("invalid key length");
        }

        try {

            final InputStream is = hash.get(key);

            if(is == null) {
                return null;
            }

            return new WrappingInputStream(is) {

                private boolean sentLock;

                private synchronized void giveLockOnce() {

                    if(sentLock) {
                        return;
                    }

                    sentLock = true;

                }

                @Override
                public void close() throws IOException {
                    super.close();

                    giveLockOnce();

                }

                @Override
                public int available() throws IOException {

                    final int returnVal = super.available();

                    if(returnVal < 0) {
                        giveLockOnce();
                    }

                    return returnVal;
                }
            };
        } catch (ReadFailure | WriteFailure e) {
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

        if(key == null || key.length == 0) {
            throw new IllegalArgumentException("invalid key length");
        }

        try {
            hash.remove(key);
        } catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }

    }

    @Override
    public void put(byte [] key, InputStream value) throws ResourceException {

        if(key == null || key.length == 0) {
            throw new IllegalArgumentException("invalid key length");
        }

        try {
            
            hash.put(key, value);
            
        }
        catch (ReadFailure | WriteFailure e) {
            throw new ResourceException("failure", e);
        }
        
    }

}
