package llc.berserkr.cache.converter;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SerializingConverter<Value> implements Converter<Value, byte []> {

    private static final Logger logger = LoggerFactory.getLogger(SerializingConverter.class);


    @Override
    public byte[] convert(Value old) throws ResourceException {
        
        try {
            return DataUtils.serialize(old);
        }
        catch (IOException e) {
            logger.error("ERROR", e);
            throw new ResourceException("<SerializingConverter><1>, " + e, e);
        }
        
    }

    @Override
	@SuppressWarnings("unchecked")  // Supresses warning from unchecked cast.  Cast was added to fix error in Jenkins build environment.
    public Value restore(byte[] newVal) throws ResourceException {
        
        try {
            return (Value) DataUtils.deserialize(newVal);
        }
        catch (IOException e) {

            logger.error("ERROR", e);
            throw new ResourceException("<SerializingConverter><2>, " + e, e);
        }
        catch (ClassNotFoundException e) {

            logger.error("ERROR", e);
            throw new ResourceException("<SerializingConverter><3>, " + e, e);
        }
        
    }

}
