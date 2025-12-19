package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.loader.ResourceLoader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class KeyEncodingCache<Value> implements Cache<String, Value> {
    
    public static final String ASCII = "US-ASCII";    //Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set
    public static final String ISO = "ISO-8859-1";      //ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
    public static final String UTF8 = "UTF-8";   //Eight-bit UCS Transformation Format
    public static final String UTF16BE = "UTF-16BE";    //Sixteen-bit UCS Transformation Format, big-endian byte order
    public static final String UTF16LE = "UTF-16LE";    //Sixteen-bit UCS Transformation Format, little-endian byte order
    public static final String UTF16 = "UTF-16";  //Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark
    
    private final Cache<String, Value> internal;
    private final String charSet;

    /**
     * 
     * @param internal
     */
    public KeyEncodingCache(
        final ResourceLoader<String, Value> internal
    ) {
        this.charSet = UTF8;
        this.internal = new ResourceLoaderCache<String, Value>(internal);
    }
    
    public KeyEncodingCache(
        final Cache<String, Value> internal
    ) {
        this.charSet = UTF8;
        this.internal = internal;
    }
    
    public KeyEncodingCache(
        final ResourceLoader<String, Value> internal,
        final String charSet
    ) {
        this.charSet = charSet;
        this.internal = new ResourceLoaderCache<String, Value>(internal);
    }
    
    public KeyEncodingCache(
        final Cache<String, Value> internal,
        final String charSet
    ) {
        this.charSet = charSet;
        this.internal = internal;
    }
    
    @Override
    public boolean exists(String key) throws ResourceException {
        return internal.exists(key);
    }

    @Override
    public Value get(String key) throws ResourceException {

        try {
            return internal.get(URLEncoder.encode(key, charSet));
        } 
        catch (UnsupportedEncodingException e) {
            throw new ResourceException("<KeyEncodingCache><1>, " + e);
        }
        
    }

    @Override
    public List<Value> getAll(List<String> keys) throws ResourceException {
        
        final List<String> encoded = new ArrayList<String>();
        
        try {
            
            for(final String unencoded : keys) {
                encoded.add(URLEncoder.encode(unencoded, charSet));
            }
        
        } 
        catch (UnsupportedEncodingException e) {
            throw new ResourceException("<KeyEncodingCache><2>, " + e);
        }
        
        return internal.getAll(encoded);
        
    }

    @Override
    public void clear() throws ResourceException {
        this.internal.clear();
    }

    @Override
    public void remove(String key) throws ResourceException {
        this.internal.remove(key);
    }

    @Override
    public void put(String key, Value value) throws ResourceException {
        this.internal.put(key, value);
    }

}
