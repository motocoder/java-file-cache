package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;

import java.util.List;

public class SynchronizedCache<Key, Value> implements Cache<Key, Value> {
    
    private final Cache<Key, Value> internal;

    public SynchronizedCache(
        final Cache<Key, Value> internal
    ) {
        this.internal = internal;
    }

    @Override
    public synchronized boolean exists(Key key) throws ResourceException {
        return internal.exists(key);
    }

    @Override
    public synchronized Value get(Key key) throws ResourceException {
        return internal.get(key);
    }

    @Override
    public synchronized List<Value> getAll(List<Key> keys) throws ResourceException {
        return internal.getAll(keys);
    }

    @Override
    public synchronized void clear() throws ResourceException {
        internal.clear();
    }

    @Override
    public synchronized void remove(Key key) throws ResourceException {
        internal.remove(key);
    }

    @Override
    public synchronized void put(Key key, Value value) throws ResourceException {
        internal.put(key, value);    
    }

}
