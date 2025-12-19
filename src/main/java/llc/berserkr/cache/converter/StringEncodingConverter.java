package llc.berserkr.cache.converter;


import llc.berserkr.cache.exception.ResourceException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class StringEncodingConverter implements Converter<String, String> {

    public static final String ASCII = "US-ASCII";    //Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set
    public static final String ISO = "ISO-8859-1";      //ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
    public static final String UTF8 = "UTF-8";   //Eight-bit UCS Transformation Format
    public static final String UTF16BE = "UTF-16BE";    //Sixteen-bit UCS Transformation Format, big-endian byte order
    public static final String UTF16LE = "UTF-16LE";    //Sixteen-bit UCS Transformation Format, little-endian byte order
    public static final String UTF16 = "UTF-16";  //Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark
    
    private final String charSet;
    
    public StringEncodingConverter() {
        this.charSet = UTF8;
    }
    @Override
    public String convert(String old) throws ResourceException {
        
        try {
            return URLEncoder.encode(old, charSet);
        } 
        catch (UnsupportedEncodingException e) {
            
            throw new ResourceException("Could not encode " + e.getMessage(), e);
            
        }
        
    }

    @Override
    public String restore(String newVal) throws ResourceException {
       
        try {
            return URLDecoder.decode(newVal, charSet);
        } 
        catch (UnsupportedEncodingException e) {
            
            throw new ResourceException("Could not decode " + e.getMessage(), e);
            
        }
        
    }

}
