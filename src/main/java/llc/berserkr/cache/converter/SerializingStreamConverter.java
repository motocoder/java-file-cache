package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.DataUtils;

import java.io.IOException;
import java.io.InputStream;

public class SerializingStreamConverter<Value> implements Converter<Value, InputStream> {

    @Override
    public InputStream convert(Value old) throws ResourceException {
        
        try {
            return DataUtils.serializeToStream(old);
        }
        catch (IOException e) {
            throw new ResourceException("<SerializingConverter><1>, " + e);
        }
        
    }

    @Override
	@SuppressWarnings("unchecked")  // Supresses warning from unchecked cast.  Cast was added to fix error in Jenkins build environment.
    public Value restore(InputStream newVal) throws ResourceException {
        
        try {
            return (Value) DataUtils.deserialize(newVal);
        }
        catch (IOException e) {
            throw new ResourceException("<SerializingConverter><2>, " + e);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceException("<SerializingConverter><3>, " + e);
        }
        
    }

}
