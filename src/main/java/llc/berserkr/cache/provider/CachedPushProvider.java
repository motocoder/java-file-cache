package llc.berserkr.cache.provider;
import llc.berserkr.cache.Cache;
import llc.berserkr.cache.MemoryCache;
import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This PushProvider caches what is put through it.
 * 
 * If nothing exists in the cache it will use the notInCacheProvider if provided.
 * 
 * @author swagner
 *
 * @param <T>
 */
public class CachedPushProvider<T> implements PushProvider<T> {
 
    private static final Logger logger = LoggerFactory.getLogger(CachedPushProvider.class);
    
    private final String KEY;
    private final Cache<String, T> cache;
    private final ResourceProvider<T> notInCacheProvider;
    private final Cache<String, Boolean> searchCache = new MemoryCache<String, Boolean>();
    private final String logTag;

    /**
     * 
     * @param cache
     */
    public CachedPushProvider(
        final String key,
        final String keyName, 
        final Cache<String, T> cache
    ) {
        
        this(key, cache, null, "provider-default");
                
    }
    
    public CachedPushProvider(
        final String key,
        final Cache<String, T> cache,
        final ResourceProvider<T> notInCacheProvider
    ) {
        this(key, cache, notInCacheProvider, "provider-default");
    }
    
    /**
     * 
     * @param cache
     * @param notInCacheProvider
     */
    public CachedPushProvider(
        final String key,
        final Cache<String, T> cache,
        final ResourceProvider<T> notInCacheProvider,
        final String logTag
    ) {
        this.KEY = key;
        this.logTag = logTag;        
        this.cache = cache;
        this.notInCacheProvider = notInCacheProvider;
        
    }
    
    @Override
    public boolean exists() throws ResourceException {
        
        final boolean returnVal;
        
        if(cache.exists(KEY)) {
            returnVal = true;            
        }
        else if(searchCache.exists(KEY)) {
            returnVal = searchCache.get(KEY);
        }
        else if(notInCacheProvider != null) {
            
            returnVal = notInCacheProvider.exists();
            searchCache.put(KEY, returnVal);
            
        }
        else {
            returnVal = false;
        }
        
        return returnVal;
        
    }

    @Override
    public T provide() throws ResourceException {
        
        final T returnVal;
        
        final T temp = cache.get(KEY);
                
        if(temp != null) {
            returnVal = temp;            
        }
        else if(searchCache.exists(KEY) && !searchCache.get(KEY)) {
            returnVal = null;
        }
        else if(notInCacheProvider != null) {
            
            returnVal = notInCacheProvider.provide();
            
            cache.put(KEY, returnVal);
            searchCache.put(KEY, true);
            
        }
        else {
            
            returnVal = null;
            searchCache.put(KEY, false);
            
        }
      
        return returnVal;
        
    }

    @Override
    public void push(T value) throws ResourceException {
        
        cache.put(KEY, value);
        searchCache.put(KEY, value != null);
        
    }
    
};
