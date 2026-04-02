package llc.berserkr.cache.converter;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class InputStreamStringConverter implements Converter<InputStream, String> {

    private static final Logger logger = LoggerFactory.getLogger(InputStreamStringConverter.class);
    
    @Override
    public String convert(InputStream old) throws ResourceException {
        
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            
            StreamUtil.copyTo(old, out);
            
            out.flush();
            
        } 
        catch (IOException e) {
            
            logger.error("<InputStreamConverter><1>, ERROR:", e);
            throw new ResourceException("<InputStreamConverter><2>, Couldn't convert", e);
            
        }
        finally {
            
            try {
                old.close();
            } 
            catch (IOException e) {
                throw new ResourceException("<InputStreamConverter><3>, Couldn't convert", e);
            }
            
        }
        
        return out.toString(StandardCharsets.UTF_8);
        
    }

    @Override
    public InputStream restore(String newVal) throws ResourceException {
        return new ByteArrayInputStream(newVal.getBytes(StandardCharsets.UTF_8));
    }

}
