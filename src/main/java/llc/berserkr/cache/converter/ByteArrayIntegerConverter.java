package llc.berserkr.cache.converter;

import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class ByteArrayIntegerConverter implements Converter<byte [], Integer>{

    private static final Logger logger = LoggerFactory.getLogger(ByteArrayIntegerConverter.class);
    
    @Override
    public Integer convert(byte[] value) throws ResourceException {
        
        int returnVal = 0;

        for (int i = 0; i < value.length; i++) {
            returnVal = (returnVal << 8) + (value[i] & 0xff);
        }
        
        return returnVal;
    }

    @Override
    public byte[] restore(Integer newVal) throws ResourceException {
        
        final byte[] returnVal = ByteBuffer.allocate(4).putInt(newVal).array();
        
        return returnVal;
        
    }

}
