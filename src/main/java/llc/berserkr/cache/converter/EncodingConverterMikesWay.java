package llc.berserkr.cache.converter;
import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

public class EncodingConverterMikesWay implements Converter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(EncodingConverterMikesWay.class);

    public static final String ASCII = "US-ASCII";    //Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set
    public static final String ISO = "ISO-8859-1";      //ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
    public static final String UTF8 = "UTF-8";   //Eight-bit UCS Transformation Format
    public static final String UTF16BE = "UTF-16BE";    //Sixteen-bit UCS Transformation Format, big-endian byte order
    public static final String UTF16LE = "UTF-16LE";    //Sixteen-bit UCS Transformation Format, little-endian byte order
    public static final String UTF16 = "UTF-16";  //Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark

    private final String charset;

    public EncodingConverterMikesWay() {
        charset = UTF8;
    }

    public EncodingConverterMikesWay(final String charset) {
        this.charset = charset;
    }
    
    @Override
    public String convert(String old) throws ResourceException {
        
        try {
            //return URLEncoder.encode(old, charset);
            return new URI(null, old, null).toASCIIString();
        }
        catch (Exception e) {
            
            logger.error("ERROR ENCODING", e);
            
            throw new ResourceException("Cannot encode", e);
            
        }
        
    }

    @Override
    public String restore(String newVal) throws ResourceException {
        
        try {
            return URLDecoder.decode(newVal, charset);
        }
        catch (UnsupportedEncodingException e) {
            
            logger.error("ERROR ENCODING", e);
            
            throw new ResourceException("Cannot encode", e);
            
        }
        
    }

}
