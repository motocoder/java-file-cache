package llc.berserkr.cache.provider;

import llc.berserkr.cache.exception.ResourceException;

public class SynchronizedPushProvider<T> implements PushProvider<T> {

    private final PushProvider<T> internal;

    public SynchronizedPushProvider(final PushProvider<T> internal) {
        
        this.internal = internal;
        
    }
    
    @Override
    public synchronized boolean exists() throws ResourceException {
        return internal.exists();
    }

    @Override
    public synchronized T provide() throws ResourceException {
        return internal.provide();
    }

    @Override
    public synchronized void push(T value) throws ResourceException {
        internal.push(value);
        
    }

}
