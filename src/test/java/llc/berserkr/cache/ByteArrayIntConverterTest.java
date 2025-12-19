package llc.berserkr.cache;

import llc.berserkr.cache.converter.ByteArrayIntegerConverter;
import llc.berserkr.cache.exception.ResourceException;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ByteArrayIntConverterTest {
    
    static {
        BasicConfigurator.configure();
    }
    
    private static final Logger logger = LoggerFactory.getLogger(ByteArrayIntConverterTest.class);
    
    @Test
    public void testConverter() {
        
        final ByteArrayIntegerConverter converter = new ByteArrayIntegerConverter();
        
        try {
            
            final byte[] bytes = converter.restore(50);
            
            logger.debug("bytes 0: " + bytes[0]);
            logger.debug("bytes 1: " + bytes[1]);
            logger.debug("bytes 2: " + bytes[2]);
            logger.debug("bytes 3: " + bytes[3]);
            
            
            final Integer longVal = converter.convert(bytes);
            
            assertEquals(50, (int)longVal);
        
        }
        catch(ResourceException e) {
            fail();
        }
    
    }
    
}
