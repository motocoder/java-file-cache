package llc.berserkr.cache;

import llc.berserkr.cache.converter.*;
import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

/**
 * 
 * @author sean
 *
 * TODO this class restricts the writes/reads to one at a time, needs to be updated now that the file cache handles async writes and reads
 *
 * @param <Value>
 */
public class FilePersistedMaxCountCache<Value> implements Cache<String, Value> {

    private static final Logger logger = LoggerFactory.getLogger(FilePersistedMaxCountCache.class);
    
    private final int maxCount;
    private final Cache<String, Value> internal;
    private final Cache<String, Serializable> persistCache;
    private final Consumer<Value> valueRemovedCallback;
    
    /**
     * 
     * @param rootFolder
     * @param internal
     * @param maxCount
     */
    public FilePersistedMaxCountCache(
        final File rootFolder,
        final Cache<String, Value> internal,
        final int maxCount,
        final Consumer<Value> valueRemovedCallback
    ) throws IOException {
        
        this.maxCount = maxCount;
        this.internal = internal;
        this.valueRemovedCallback = valueRemovedCallback;
        
        final File persistRoot = new File(rootFolder, "countPersisted");
        
        persistRoot.mkdirs();
        
        if(!persistRoot.isDirectory()) {
            throw new IllegalArgumentException("persist root must be a folder");
        }
        
        final File dataFolder = new File(persistRoot, "data");
        
        dataFolder.mkdirs();
        
        if(!dataFolder.isDirectory()) {
            throw new IllegalArgumentException("Data folder must be a folder");
        }

        final Cache<byte [], InputStream> diskCache = new StreamFileCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                new KeyConvertingCache<String, byte[], InputStream>(diskCache, new ReverseConverter<>(new BytesStringConverter()));

        this.persistCache = new ValueConvertingCache<String, Serializable, InputStream>(
                keyConvertingCache,
                new SerializingStreamConverter<>()
        );
        
        try {
            
            if(!this.persistCache.exists("currentSize")) {
                this.persistCache.put("currentSize", 0);                
            }
            
        } 
        catch(ResourceException e) {
            throw new RuntimeException("could not initialize");
        }
        
    }
    
    @Override
    public boolean exists(String key) throws ResourceException {
        return internal.exists(key);
    }

    @Override
    public Value get(String key) throws ResourceException {
        return internal.get(key);
    }

    @Override
    public List<Value> getAll(List<String> keys) throws ResourceException {
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
        
        final Value removing = internal.get(key);
        
        if(removing != null) {
            
            int currentSize = (Integer)this.persistCache.get("currentSize");
            currentSize--;
            
            this.persistCache.put("currentSize", currentSize);
            
            internal.remove(key);
            
        }
        
    }

    @Override
    public void put(String key, Value value) throws ResourceException {

        if (this.exists(key)) {
        	this.remove(key);
        }
        
        if(value != null) {
            
            {
                
                int currentSize = (Integer)this.persistCache.get("currentSize");
                currentSize++;
                
                this.persistCache.put("currentSize", currentSize);
                
            }
                    
            internal.put(key, value);
            
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
               
                while(currentSize > this.maxCount) {
                     
                    final LinkedData bottom = (LinkedData) this.persistCache.get("bottomKey");
                    
                    if(bottom == null) {
                        throw new RuntimeException("Size too big but there is nothing in it?");
                    }
                   
                    final LinkedData afterBottom = (LinkedData) this.persistCache.get("linked:" + bottom.getKeyAfter()); 
                    
                    final Value valueRemoving = internal.get(bottom.getMyKey());
                            
                    if(valueRemoving != null) {
                        
                        currentSize--;
                        
                        if(currentSize < 0) {
                            currentSize = 0;
                        }
                        
                        this.persistCache.put("currentSize", currentSize);
                        
                        final Value valueToRemove = internal.get(bottom.getMyKey());
                        
                        internal.remove(bottom.getMyKey());
                        
                        valueRemovedCallback.accept(valueToRemove);
                        
                    }
                    
                    if (afterBottom != null) {
                    	
                    	final LinkedData newBottom = new LinkedData(afterBottom.getMyKey(), null, afterBottom.getKeyAfter());
    
                        persistCache.remove("linked:" + bottom.getKeyAfter());
                        
                        persistCache.put("bottomKey", newBottom);
                        
                    }
                    else {
                        
                        if(valueRemoving == null) {
                            throw new RuntimeException("nothing to remove and nothing in the cache now.");
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

}
