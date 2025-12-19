package llc.berserkr.cache.loader;

import llc.berserkr.cache.Cache;
import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *  
 *  The CachedResourceLoader is an implementation of the ResourceLoader interface
 *  with the addition of a caching mechanism.
 *  
 *  Cache<Key,Boolean> esistsCache - caches the returned values of the ResourceLoader's Exists<Key> method.
 *  
 *  Cache<Key,Value> getCache - caches the returned values from the Get<Key> method.
 *  
 *  ex:
 *  CachedResourceLoader<Key, Value>(ResourceLoader<Key,Value> internal, Cache<Key,Boolean> existsCache, Cache<Key,Value> getCache)
 *
 */

public class CachedResourceLoader<Key, Value> implements ResourceLoader<Key, Value> {
    
    private static final Logger logger = LoggerFactory.getLogger(CachedResourceLoader.class);
    
    private final Cache<Key, Value> valueCache;
    private final Cache<Key, Boolean> searchCache;
    private final ResourceLoader<Key, Value> internal;
    private final String tag;

    /**
     * 
     * @param internal
     * @param valueCache
     * @param searchCache
     */
    public CachedResourceLoader(
        final ResourceLoader<Key, Value> internal, 
        final Cache<Key, Value> valueCache,
        final Cache<Key, Boolean> searchCache
    ) {
        
        this.valueCache = valueCache;
        this.searchCache = searchCache;
        this.internal = internal;
        
        this.tag = null;
        
    }
    
    public CachedResourceLoader(
        final ResourceLoader<Key, Value> internal,
        final Cache<Key, Value> valueCache,
        final Cache<Key, Boolean> searchCache, 
        final String tag
    ) {
        
        this.internal = internal;
        this.searchCache = searchCache;
        this.valueCache = valueCache;
        
        this.tag = tag;
    }

    /**
     * This method returns a boolean if the value assigned to the
     * specified key parameter is not null as well as storing
     * the results in a cache.
     * 
     * @return boolean
     * 
     */

    @Override
    public boolean exists(Key key) throws ResourceException {
        
        if(key == null){
            throw new NullPointerException("<CachedResourceLoader><1>, " + "key cannot be null");
        }
        
        final Boolean searchCacheValue = searchCache.get(key);
        
        if(tag != null) {
            logger.info("search cache for " + tag + " key " + key + " was " + searchCacheValue);
            
        }
        
        final boolean returnVal;
        
        if(searchCacheValue != null) {
            returnVal = searchCacheValue;
        }
        else {
            
            final Value value = valueCache.get(key);
            
            if(value != null) {
                returnVal = true;
            }
            else {
                
                
                returnVal = internal.exists(key);
                
                searchCache.put(key, returnVal);
                
            }
            
        }
        
        return returnVal;
    }
    
    /**
     * This method returns the Value assigned to the
     * specified key parameter as long as the value
     * is not null. It also stores the results in 
     * a cache.
     * 
     * @return Value
     * 
     */

    @Override
    public Value get(Key key) throws ResourceException {
        
        if(key == null){
            throw new NullPointerException("<CachedResourceLoader><2>, " + "key cannot be null");
        }
        
        final Value cachedValue = valueCache.get(key);
        
        if(tag != null) {
            logger.info("get cache for " + tag + " key " + key + " was " + cachedValue);
            
        }
        
        final Value returnVal;
        
        if(cachedValue != null) {
            returnVal = cachedValue;
        }
        else {
            
            final Boolean searchVal = searchCache.get(key);
            
            if(searchVal != null) {
                
                if(searchVal) {
                    returnVal = queryAndCacheValue(key);
                }
                else {
                    returnVal = null;
                }
            }
            else {
                returnVal =  queryAndCacheValue(key);
            }
            
        }
        
        return returnVal;
        
    }
    
    /**
     * 
     * 
     * @return Value
     * 
     */
    
    private Value queryAndCacheValue(final Key key) throws ResourceException {
        
        if(key == null){
            throw new NullPointerException("<CachedResourceLoader><3>, " + "key cannot be null");
        }
        
        final Value returnVal = internal.get(key);
        
        if(returnVal == null) {

            searchCache.put(key, false);

            return null;

        }
        else {
            
            searchCache.put(key, true);
            valueCache.put(key, returnVal);

            return valueCache.get(key);
            
        }

    }

    /**
     * This method returns a List of Values assigned to the
     * specified keys parameter as well as storing them
     * in a cache.
     * 
     * @return List<Value>
     * 
     */
    
    @Override
    public List<Value> getAll(List<Key> keys) throws ResourceException {
        
        if(keys == null) {
            throw new NullPointerException("<CachedResourceLoader><4>, " + "Keys cannot be null");
        }
        
        if(keys.contains(null)) {
            throw new NullPointerException("<CachedResourceLoader><5>, " + "Keys cannot contain null");
        }
        
        final Map<Key, Value> values = new HashMap<Key, Value>();
        final Set<Key> wasSearchFalse = new HashSet<Key>();
        final Set<Key> makeCallFor = new HashSet<Key>();
        
        for(final Key key : keys) {
            
            final Value cacheValue = valueCache.get(key);
            
            if(cacheValue != null) {
                values.put(key, cacheValue);
            }
            else {
                
                final Boolean searchValue = searchCache.get(key);
                
                if(searchValue == null) {
                    makeCallFor.add(key);
                }
                else {
                    
                    if(searchValue) {
                        makeCallFor.add(key);
                    }
                    else {
                        wasSearchFalse.add(key);
                    }
                    
                }
                
            }
            
        }
        
        if(makeCallFor.size() > 0) {
        
            final List<Key> toQuery = new ArrayList<Key>(makeCallFor);
            
            final List<Value> returned = internal.getAll(toQuery);
            
            if(toQuery.size() != returned.size()) {
                throw new ResourceException("<CachedResourceLoader><6>, " + "queried values size not the same as keys size");
            }
            
            for(int i = 0; i < toQuery.size(); i++) {
                
                final Key key = toQuery.get(i);
                final Value value = returned.get(i);

                values.put(key, value);
                
            }
            
        }
        
        final List<Value> returnVals = new ArrayList<Value>(keys.size());
        
        for(final Key key : keys) {
            
            final Value value = values.get(key);
            
            if(value != null) {
                
                searchCache.put(key, true);
                valueCache.put(key, value);
                
            }
            else {
                searchCache.put(key, false);
            }
            
            returnVals.add(values.get(key));
        }
        
        return returnVals;
    }

}
