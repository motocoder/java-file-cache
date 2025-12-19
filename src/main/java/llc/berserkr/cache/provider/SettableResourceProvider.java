package llc.berserkr.cache.provider;

public class SettableResourceProvider<Value> implements PushProvider<Value> {

    private Value internal;
    
    @Override
    public boolean exists() {
        return internal != null;
    }

    @Override
    public Value provide() {
        return internal;
    }

    public void setInternal(Value internal) {
        this.internal = internal;
    }

    @Override
    public void push(Value value) {
        setInternal(value);        
    }
    
}
