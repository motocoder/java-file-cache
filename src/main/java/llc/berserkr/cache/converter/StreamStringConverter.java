package llc.berserkr.cache.converter;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StreamStringConverter implements Converter<InputStream, String> {

    private static final Logger logger = LoggerFactory.getLogger(StreamStringConverter.class);

    @Override
    public String convert(InputStream old) throws ResourceException {
        if(old == null) {
            return null;
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        String fullSchemaString;

        try {

            try {
                StreamUtil.copyTo(old, out);
            }
            finally {
                old.close();
            }

            fullSchemaString = new String(out.toByteArray(), StandardCharsets.UTF_8);

        }
        catch (IOException e) {
            logger.error("SchemaAndStreamConverter failed " + e.getMessage(), e);
            return null;
        }

        return fullSchemaString;
    }

    @Override
    public InputStream restore(String newVal) throws ResourceException {
        return new ByteArrayInputStream(newVal.getBytes(StandardCharsets.UTF_8));
    }

}