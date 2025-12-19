package llc.berserkr.cache.loader;

import llc.berserkr.cache.exception.ResourceException;

import java.util.List;

/**
 *  
 *  SynchronizedResourceLoader is an implementation of the
 *  ResourceLoader interface and it synchronizes each of 
 *  ResourceLoader's methods with this class's object in 
 *  order to easily enable thread safe manipulations on the 
 *  cache. This is important for many types of cache objects.
 *
 */

/**
 * 
 *
 * @param <Key>
 * @param <Value>
 */


public class SynchronizedResourceLoader<Key, Value> implements ResourceLoader<Key, Value> {

    private final ResourceLoader<Key, Value> internal;
    
    /**
     * 
     * @param internal
     */

    public SynchronizedResourceLoader(ResourceLoader<Key, Value> internal) {
        this.internal = internal;
    }
    
    /**
     * This method returns a boolean if the value assigned to the
     * specified key parameter is not null and is synchronized for 
     * thread safety.
     * 
     * @param key
     * @return boolean
     */
    
    @Override
    public synchronized boolean exists(Key key) throws ResourceException {
        return internal.exists(key);
    }
    
    /**
     * This method returns the Value assigned to the
     * specified key parameter as long as the value
     * is not null and is synchronized for thread safety.
     * 
     * @param key
     * @return Value
     */

    @Override
    public synchronized Value get(Key key) throws ResourceException {
        return internal.get(key);
    }
    
    /**
     * This method returns a List of TValues assigned to the
     * specified keys parameter and is synchronized for 
     * thread safety.
     * 
     * @param keys
     * @return List<Value>
     * 
     */

    @Override
    public synchronized List<Value> getAll(List<Key> keys) throws ResourceException {
        return internal.getAll(keys);
    }

}
