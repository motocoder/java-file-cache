package llc.berserkr.cache.provider;

public class DefaultPushProvider<Value> implements PushProvider<Value> {

    private Value value;
    
    @Override
    public boolean exists() {
        return value != null;
    }

    @Override
    public Value provide() {
        return value;
    }

    @Override
    public void push(Value value) {
        this.value = value;        
    }

}
