package llc.berserkr.cache;

import llc.berserkr.cache.converter.BytesStringConverter;
import llc.berserkr.cache.converter.ReverseConverter;
import llc.berserkr.cache.converter.SerializingConverter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.provider.ResourceProvider;
import llc.berserkr.cache.provider.SequentialIDProvider;
import llc.berserkr.cache.util.SizeCountingInputStream;
import llc.berserkr.cache.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

import static llc.berserkr.cache.hash.SegmentedStreamingFile.delete;

//TODO fix this class it fails multi-threading
public class FilePersistedMaxSizeStreamCache implements Cache<String, InputStream> {

    private static final Logger logger = LoggerFactory.getLogger(FilePersistedMaxSizeCache.class);
    
    private final long maxSize;
    private final Cache<String, InputStream> internal;
    private final Cache<String, Serializable> persistCache;
    private ResourceProvider<Long> idProvider = new SequentialIDProvider();

	private File tempFolder2;
    
    /**
     * 
     * @param rootFolder
     * @param internal
     * @param maxSize
     */
    public FilePersistedMaxSizeStreamCache(
        final File rootFolder,
        final Cache<String, InputStream> internal,
        final long maxSize
    ) throws IOException {

        this.maxSize = maxSize;
        this.internal = internal;
        
        final File persistRoot = new File(rootFolder, "sizePersisted");
        
        persistRoot.mkdirs();
        
        if(!persistRoot.isDirectory()) {
            throw new IllegalArgumentException("persist root must be a folder");
        }
        
        final File dataFolder = new File(persistRoot, "data");
        final File tempFolder = new File(persistRoot, "temp");
        this.tempFolder2 = new File(persistRoot, "temp2");
        
        dataFolder.mkdirs();
        tempFolder.mkdirs();
        tempFolder2.mkdirs();
        
        if(!dataFolder.isDirectory()) {
            throw new IllegalArgumentException("Data folder must be a folder");
        }
        
        if(!tempFolder.isDirectory()) {
            throw new IllegalArgumentException("Temp folder must be folder");
        }
        
        if(!tempFolder2.isDirectory()) {
            throw new IllegalArgumentException("Temp folder must be folder");
        }
        
        final BytesFileCache diskCache = new BytesFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], byte []> keyConverted =
                new KeyConvertingCache<String, byte[], byte []>(
                        diskCache, new ReverseConverter<>(new BytesStringConverter())
                );

        final Cache<String, Serializable> cache = 
            new ValueConvertingCache<String, Serializable, byte []>(
                    keyConverted,
                new SerializingConverter<Serializable>()
            );
            
        this.persistCache = cache;
        
        try {
            
            if(!this.persistCache.exists("currentSize")) {
                this.persistCache.put("currentSize", 0);                
            }
            
        } 
        catch(ResourceException e) {
            throw new RuntimeException("could not initialize");
        }

        throw new RuntimeException("this class is broken");

    }
    
    @Override
    public boolean exists(String key) throws ResourceException {
        return internal.exists(key);
    }

    @Override
    public InputStream get(String key) throws ResourceException {
        return internal.get(key);
    }

    @Override
    public List<InputStream> getAll(List<String> keys) throws ResourceException {
        return internal.getAll(keys);
    }

    @Override
    public void clear() throws ResourceException {
        
        this.persistCache.clear();
        
        internal.clear();
        
        this.persistCache.put("currentSize", 0);
        
    }

    @Override
    public void remove(String key) throws ResourceException {
        
        final InputStream removing = internal.get(key);
        
        if(removing != null) {
            
        	final File tempFile = new File(tempFolder2, "temp-" + idProvider.provide() + ".tmp");
        	
        	final int removingSize;
        	
        	try(removing) {
        	    removingSize = this.checkSize(removing, tempFile);
        	} catch (IOException e) {
                throw new ResourceException("failed to read size", e);
            } finally {

                delete(tempFile.getAbsolutePath());

        	}
        	
            int currentSize = (Integer)this.persistCache.get("currentSize");
            currentSize -= removingSize;
            
            this.persistCache.put("currentSize", currentSize);
            
            internal.remove(key);
            
        }
        
    }

    @Override
    public void put(String key, InputStream value) throws ResourceException {

        if (this.exists(key)) {
        	this.remove(key);
        }
        
        if(value != null) { // we already removed it
            
            final File tempFile = new File(tempFolder2, "temp-" + idProvider.provide() + ".tmp");
            
            try {
                
                final int sizeOfAdding;
                
                try {
                    sizeOfAdding = this.checkSize(value, tempFile);
                }
                finally {
                    
                    try {
                        value.close();
                    }
                    catch (IOException e) {
                        throw new ResourceException("Failed to close");    
                    }
                                
                }
                
                {
                    
                    int currentSize = (Integer)this.persistCache.get("currentSize");
                    currentSize += sizeOfAdding;
                    
                    this.persistCache.put("currentSize", currentSize);
                    
                }
                        
                try {
                    internal.put(key, new FileInputStream(tempFile));
                } 
                catch (FileNotFoundException e1) {
                    throw new ResourceException("FAILED TO WRITE TEMP");
                }
                
            }
            finally {
                delete(tempFile.getAbsolutePath());
            }
            
            final LinkedData topKey = (LinkedData) persistCache.get("topKey");
            
            if(topKey == null) {
    
                final LinkedData myLinkedData = new LinkedData(key, null, null);
                        
                persistCache.put("bottomKey", myLinkedData);
                persistCache.put("topKey", myLinkedData);
                
            }
            else {
                
                final LinkedData myLinkedData = new LinkedData(key, topKey.getMyKey(), null);
                
                final LinkedData keyAfterMine = new LinkedData(topKey.getMyKey(), topKey.getKeyBefore(), myLinkedData.getMyKey());
                
                persistCache.put("linked:" + keyAfterMine.getMyKey(), keyAfterMine);
                persistCache.put("topKey", myLinkedData);
                
                final LinkedData bottom = (LinkedData) this.persistCache.get("bottomKey");
                
                if (bottom.getKeyAfter() == null) {
                
    	            final LinkedData newBottom = new LinkedData(bottom.getMyKey(), null, key);
    	            
    	            persistCache.put("bottomKey", newBottom);
    	            
                }
                
            }
            
            {
                int currentSize = (Integer)this.persistCache.get("currentSize");
               
                while(currentSize > this.maxSize) {
                     
                    final LinkedData bottom = (LinkedData) this.persistCache.get("bottomKey");
                    
                    if(bottom == null) {
                        throw new RuntimeException("Size too big but there is nothing in it?");
                    }
                   
                    final LinkedData afterBottom = (LinkedData) this.persistCache.get("linked:" + bottom.getKeyAfter()); 
                    
                    final InputStream valueRemoving = internal.get(bottom.getMyKey());
                            
                    if(valueRemoving != null) {
                       
                        final File tempFile2 = new File(tempFolder2, "temp-" + idProvider.provide() + ".tmp");
                        
                        final int size;
                        
                        try {
                            size = this.checkSize(valueRemoving, tempFile2);
                        }
                        finally {
                            
                            try {
                                valueRemoving.close();
                            } 
                            catch (IOException e) {
                                
                                logger.error("ERROR:", e);
                                throw new ResourceException("ERROR");
                                
                            }

                            delete(tempFile2.getAbsolutePath());

                        }
                        
                        currentSize -= size;
                        
                        if(currentSize < 0) {
                            currentSize = 0;
                        }
                        
                        this.persistCache.put("currentSize", currentSize);
                        
                        internal.remove(bottom.getMyKey());
                        
                    }
                    
                    if (afterBottom != null) {
                    	
                    	final LinkedData newBottom = new LinkedData(afterBottom.getMyKey(), null, afterBottom.getKeyAfter());
    
                        persistCache.remove("linked:" + bottom.getKeyAfter());
                        
                        persistCache.put("bottomKey", newBottom);
                        
                    }
                    else {
                        
                        if(valueRemoving == null) { //triggers because the cache exceeds the size but there's nothing to remove
                            throw new RuntimeException("nothing to remove and nothing in the cache now. " + maxSize + " " + currentSize);
                        }
                        
                    }
                    
                }
                
            }
        }
        
    }
    
    private static class LinkedData implements Serializable {
        
        private static final long serialVersionUID = 3976267230111713664L;
        
        private final String keyBefore;
        private final String keyAfter;
        private final String myKey;

        public LinkedData(
            final String myKey,
            final String keyBefore,
            final String keyAfter
        ) {
            
            this.myKey = myKey;
            this.keyBefore = keyBefore;
            this.keyAfter = keyAfter;
            
        }
        
        public String getMyKey() {
            return myKey;
        }

        public String getKeyBefore() {
            return keyBefore;
        }

        public String getKeyAfter() {
            return keyAfter;
        }
        
    }
    
    private int checkSize(InputStream newVal, File tempFile) throws ResourceException {
    	
        //final File tempFile = new File(tempFolder, "temp-" + idProvider.provide() + ".tmp");
		
		if(tempFile.exists()) {
			//throw new RuntimeException("Temp file exists"); //commenting this out fixes the issue
		}
		
		final SizeCountingInputStream wrapped = new SizeCountingInputStream(newVal);
		
		try {
			
			final FileOutputStream out = new FileOutputStream(tempFile);
			
			try {
				
				StreamUtil.copyTo(wrapped, out);
				
			} 
			catch (IOException e) {
				
				logger.error("ERROR:", e);
				throw new ResourceException("ERROR:", e);
				
			}
			finally {
				
				try {
					out.close();
				}
				catch (IOException e) {
					
					logger.error("ERROR:", e);
					throw new ResourceException("ERROR:", e);
					
				}
				
			}
			
			
		} 
		catch (FileNotFoundException e) {
			
			logger.error("ERROR:", e);
			throw new ResourceException("ERROR:", e);
			
		}
		
		return (int) wrapped.getTotalRead();
		
    }

}
