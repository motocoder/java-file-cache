package llc.berserkr.cache.converter;

import llc.berserkr.cache.exception.ResourceException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BytesStringConverter implements Converter<byte [], String> {

    @Override
    public String convert(byte[] old) throws ResourceException {
        return new String(old, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] restore(String newVal) throws ResourceException {
        return newVal.getBytes(StandardCharsets.UTF_8);
    }

}
