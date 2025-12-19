package llc.berserkr.cache.loader;

import llc.berserkr.cache.exception.ResourceException;

import java.util.*;

/**
 *  
 *  The ListResourceLoader is an implementation of the ResourceLoader interface
 *  that takes a list of ResourceLoaders at construction and iterates through 
 *  them when a method is called in order to find the needed resource.
 *  
 * 
 *
 */

public class ListResourceLoader<Key, Value> implements ResourceLoader<Key, Value> {
    
    private final List<ResourceLoader<Key, Value>> caches;
    
    /**
     * 
     * @param caches
     */

    public ListResourceLoader(final List<ResourceLoader<Key, Value>> caches) {
        
        if(caches == null) {
            throw new NullPointerException("<ListResourceLoader><1>, " + "Caches cannot be null");
        }
        
        if(caches.isEmpty()) {
            throw new IllegalArgumentException("<ListResourceLoader><2>, " + "Caches cannot be empty");
        }
        
        if(caches.contains(null)) {
            throw new NullPointerException("<ListResourceLoader><3>, " + "caches cannot contain null");
        }      
        this.caches = new ArrayList<ResourceLoader<Key, Value>>(caches);
        
    }
    
    /**
     * Exists(key) - Calls the Exists(key) method from the ResourceLoader interface on the 
     *  first loader in the list. If the first loader returns false, it will then proceed to the next item in 
     *  the list until it either finds a true value, or runs out of loaders in the list.
     * 
     * @return boolean
     * 
     */

    @Override
    public boolean exists(Key key) throws ResourceException {
       
        if(key == null) {
            throw new NullPointerException("<ListResourceLoader><4>, " + "key cannot be null");
        }
        
        for(final ResourceLoader<Key, Value> cache : caches) {
            
            if(cache.exists(key)) {
                return true;
            }
            
        }
        
        return false;
    }

    
    /**
     * Get(key) - Calls the Get(key) method from the ResourceLoader interface on the 
     *  first loader in the list. If the first loader returns null, it will then proceed to the next item in 
     *  the list until it either finds a value, or runs out of loaders in the list.
     * 
     * @return Value
     * 
     */
    
    @Override
    public Value get(Key key) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ListResourceLoader><5>, " + "key cannot be null");
        }
        int i = 0;
        
        final int size = caches.size();
        
        Value returnVal = null;
        
        //find the value
        
        for(; i < size; i++) {
            
            final ResourceLoader<Key, Value> cache = caches.get(i);
            final Value fromCache = cache.get(key); 
            
            if(fromCache != null) {
                
                returnVal = fromCache;
                break;
                
            }
        }
                
        return returnVal;
    }
    
    /**
     *  GetAll(List<key>) - Queries all keys on the first ResourceLoader. Any keys that return null values on the 
     *  first loader will then be queried on the next ResourceLoader, and so on, until either all values are
     *  loaded or all loader are exhausted.
     * 
     * @return List<Value>
     * 
     */

    @Override
    public List<Value> getAll(List<Key> keys) throws ResourceException {
        
        if(keys == null) {
            throw new NullPointerException("<ListResourceLoader><6>, " + "keys cannot be null");
        }
        
        if(keys.contains(null)) {
            throw new NullPointerException("<ListResourceLoader><7>, " + "Keys cannot contain null");
        }
        
        final Set<Key> unfound = new HashSet<Key>(keys);
        final Map<Key, Value> values = new HashMap<Key, Value>();
        
        final Map<ResourceLoader<Key, Value>, Set<Key>> foundIn = new HashMap<ResourceLoader<Key, Value>, Set<Key>>();
        
        int index = 0;
        
        for(; index < caches.size(); index++) {
            
            if(unfound.size() == 0) {
                break;
            }
            
            final ResourceLoader<Key, Value> cache = caches.get(index);
            
            final List<Key> querying = new ArrayList<Key>(unfound);
            final List<Value> queried = cache.getAll(querying);
            
            if(querying.size() != queried.size()) {
                throw new ResourceException("<ListResourceLoader><8>, " + "something wierd just happened");
            }
            
            final int size = querying.size();
            
            final Set<Key> foundInSet = new HashSet<Key>();
            
            for(int i = 0; i < size; i++) {
                
                final Key key = querying.get(i);
                final Value value = queried.get(i);
                
                if(value != null) {
                    values.put(key, value);
                    unfound.remove(key);
                    foundInSet.add(key);
                    
                }
                
            }
            
            foundIn.put(cache, foundInSet);
            
        }
        
        final List<Value> returnVals = new ArrayList<Value>();
        
        for(final Key key : keys) {
            returnVals.add(values.get(key));
        }
        
        return returnVals;
        
    }


}
