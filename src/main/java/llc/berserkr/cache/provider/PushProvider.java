package llc.berserkr.cache.provider;


import llc.berserkr.cache.exception.ResourceException;

public interface PushProvider<Value> extends ResourceProvider<Value> {

    void push(Value value) throws ResourceException;
    
}
