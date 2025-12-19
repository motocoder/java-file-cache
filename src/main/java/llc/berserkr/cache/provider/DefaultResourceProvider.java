package llc.berserkr.cache.provider;


import llc.berserkr.cache.exception.ResourceException;

public abstract class DefaultResourceProvider<Value> implements ResourceProvider<Value> {

    @Override
    public boolean exists() throws ResourceException {
        return provide() != null;
    }
    
}
