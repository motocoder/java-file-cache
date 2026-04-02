package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

public class ReverseConverter<TKey, TValue> implements Converter<TKey, TValue> {

    private final Converter<TValue, TKey> internal;

    public ReverseConverter(Converter<TValue, TKey> internal) {
        this.internal = internal;
    }
    
    @Override
    public TValue convert(TKey old) throws ResourceException {
        return internal.restore(old);
    }

    @Override
    public TKey restore(TValue newVal) throws ResourceException {
        return internal.convert(newVal);
    }

}
