package llc.berserkr.cache;


import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.loader.ResourceLoader;

import java.util.List;

public class ResourceLoaderCache<Key, Value> implements Cache<Key, Value> {

    private final ResourceLoader<Key, Value> internal;

    public ResourceLoaderCache(
        final ResourceLoader<Key, Value> internal
    ) {
        this.internal = internal;
    }
    
    @Override
    public boolean exists(Key key) throws ResourceException {
        return internal.exists(key);
    }

    @Override
    public Value get(Key key) throws ResourceException {
        return internal.get(key);
    }

    @Override
    public List<Value> getAll(List<Key> keys) throws ResourceException {
        return internal.getAll(keys);
    }

    @Override
    public void clear() {
    }

    @Override
    public void remove(Key key) {
    }

    @Override
    public void put(Key key, Value value) {        
    }

}
