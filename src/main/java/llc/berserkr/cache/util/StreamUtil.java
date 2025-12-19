package llc.berserkr.cache.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class StreamUtil {
    
    public static final int DEFAULT_BUFFER_SIZE = 10000;
    
    private StreamUtil() {
        
    }
    
    /**
     * flushes doesn't close
     * 
     * @param in
     * @param outs
     * @param bufferSize
     * @throws IOException
     */
    public static void copyTo(
        final InputStream in,
        final Set<OutputStream> outs,
        final int bufferSize
    ) throws IOException {
        
        if(in == null) {
            throw new NullPointerException("<StreamUtil><1>" + "In cannot be null");
        }
        
        if(outs == null) {
            throw new NullPointerException("<StreamUtil><2>" + "Out cannot be null");
        }
        
        if(outs.contains(null)) {
            throw new NullPointerException("<StreamUtil><3>" + "outs cannot contain null");
        }
        
        final byte [] buffer = new byte[bufferSize];
        
        while(true) {
            
            final int read = in.read(buffer);
            
            if(read <= 0) {
                break;
            }
            
            for(final OutputStream out : outs) {
                out.write(buffer, 0, read);     
            }
            
        }
        
        for(final OutputStream out : outs) {
            out.flush();
        }
        
    }

    /**
     * flushes doesn't close
     * 
     * @param in
     * @param out
     * @param bufferSize
     * @throws IOException
     */
    public static void copyTo(
        final InputStream in, 
        final OutputStream out,
        final int bufferSize
    ) throws IOException {
        
        final Set<OutputStream> outs = new HashSet<OutputStream>();
        outs.add(out);
        
        copyTo(in, outs, bufferSize);
        
    }

    /**
     * 
     * flushes doesn't close
     * 
     * @param is
     * @param out
     * @throws IOException
     */
    public static void copyTo(InputStream is, OutputStream out) throws IOException {
        copyTo(is, out, DEFAULT_BUFFER_SIZE);
    }

    public static byte [] digest(InputStream in) throws NoSuchAlgorithmException, IOException {
        
        final MessageDigest md = MessageDigest.getInstance("MD5");
        
        final DigestInputStream is = new DigestInputStream(in, md);
        
        try {
            
            final byte [] buffer = new byte[1024];
            
            // read stream to EOF as normal...
            while(is.read(buffer) >= 0) {
                
            }           
                        
        }
        finally {
          is.close();
        }
        
        return is.getMessageDigest().digest();
        
    }

}
