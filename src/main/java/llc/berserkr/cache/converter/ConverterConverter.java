package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

public class ConverterConverter<Key, NewVal, OldVal> implements Converter<Key, NewVal> {

    private final Converter<Key, OldVal> oldConverter;
    private final Converter<OldVal, NewVal> valConverter;

    public ConverterConverter(
        final Converter<Key, OldVal> oldConverter, 
        final Converter<OldVal, NewVal> valConverter
    ) {
        
        this.oldConverter = oldConverter;
        this.valConverter = valConverter;
        
    }
    
    @Override
    public NewVal convert(Key old) throws ResourceException {
        return valConverter.convert(oldConverter.convert(old));        
    }

    @Override
    public Key restore(NewVal newVal) throws ResourceException {
        return oldConverter.restore(valConverter.restore(newVal));        
    }

}
