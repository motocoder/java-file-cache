package llc.berserkr.cache.provider;

public interface MuteResourceProvider<Value> extends ResourceProvider<Value> {

    @Override
    public abstract boolean exists();

    @Override
    public abstract Value provide();

}
