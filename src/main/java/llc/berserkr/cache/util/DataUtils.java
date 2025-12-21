package llc.berserkr.cache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(DataUtils.class);
    
    private DataUtils() {
        
    }
    
    /**
     * 
     * @param object
     * @return
     * @throws IOException
     */
    public static byte [] serialize(final Object object) throws IOException {
        
        if(object == null) {
            throw new NullPointerException("<DataUtils><1>, " + "object cannot be null");
        }
        
        final ByteArrayOutputStream returnVal = new ByteArrayOutputStream();
        final ObjectOutputStream objectsOut = new ObjectOutputStream(returnVal);
        
        objectsOut.writeObject(object);
        objectsOut.flush();
        objectsOut.close();
        
        return returnVal.toByteArray();
        
    }
    
    /**
     * Beware class case exception if you expect the wrong type of return value.
     * 
     * If you fear you wont know what the return value is, put the return value into an object reference 
     * then do instanceof until you figure it out.
     * 
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(final byte [] bytes) throws IOException, ClassNotFoundException {
        
        if(bytes == null) {
            throw new NullPointerException("<DataUtils><2>, " + "bytes cannot be null");
        }
        
        final ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
        final ObjectInputStream objectsIn = new ObjectInputStream(bytesIn);
        
        return (T) objectsIn.readObject();
        
    }

    /**
     * 
     * @param bytesIn
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(final InputStream bytesIn) throws IOException, ClassNotFoundException {
        
        if(bytesIn == null) {
            throw new NullPointerException("<DataUtils><2>, " + "bytes cannot be null");
        }

        try {
            final ObjectInputStream objectsIn = new ObjectInputStream(bytesIn);

            return (T) objectsIn.readObject();
        }
        finally {
            bytesIn.close();
        }
        
    }
	
    /**
     * 
     * @param object
     * @return
     * @throws IOException
     */
	public static InputStream serializeToStream(final Object object) throws IOException {
        
        if(object == null) {
            throw new NullPointerException("<DataUtils><1>, " + "object cannot be null");
        }
        
        final PipedOutputStream pipedOut = new PipedOutputStream();        
        final PipedInputStream pipedIn = new PipedInputStream(pipedOut);
        
        final ObjectOutputStream objectsOut = new ObjectOutputStream(pipedOut);

        Executors.newSingleThreadExecutor().execute(
            new Runnable() {

                @Override
                public void run() {
                    try {
                    objectsOut.writeObject(object);
                    
                    objectsOut.flush();
                    objectsOut.close();
                    
                    }
                    catch (IOException e) {
                        logger.error("ERROR:", e);
                    }
                    finally {
                        
                        try {
                            objectsOut.close();
                        } 
                        catch (IOException e) {
                            logger.error("ERROR:",e);
                        }
                        
                    }
                    
                }
                
            }
            
        );

        
        return pipedIn;
        
    }

    public static byte[] compress(String text) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(text.getBytes(StandardCharsets.UTF_8)); // Specify character encoding
        gzip.close();
        return bos.toByteArray();
    }

    public static String encode(byte [] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static byte [] decode(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    public static String decompress(byte[] compressedData, int skip) throws IOException {
        return decompress(compressedData, skip, compressedData.length);
    }

    public static String decompress(byte[] compressedData, int skip, int end) throws IOException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(compressedData, skip, end);
        final GZIPInputStream gzip = new GZIPInputStream(bis);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = gzip.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        gzip.close();
        bos.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8); // Specify character encoding
    }

    public static String decompress(byte[] compressedData) throws IOException {
        return decompress(compressedData, 0);
    }

    public static byte[] intToByteArray(int value) {
        // Allocate a ByteBuffer with capacity for 4 bytes (an int)
        ByteBuffer buffer = ByteBuffer.allocate(4);

        // Set the byte order (e.g., BIG_ENDIAN or LITTLE_ENDIAN)
        // BIG_ENDIAN is common for network protocols and human readability
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Put the integer into the buffer
        buffer.putInt(value);

        // Return the byte array representation of the buffer's content
        return buffer.array();
    }

    public static int bytesToInt(byte [] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] charToBytes(char ch) {

        // Extract the most significant byte (MSB)
        byte msb = (byte) ((ch >> 8) & 0xFF);

        // Extract the least significant byte (LSB)
        byte lsb = (byte) (ch & 0xFF);

        return new byte[] {msb, lsb};

    }

    public static char bytesToChar(byte[] bytes) {

        if(bytes.length != 2) { throw new IllegalArgumentException("bytes must be 2 lenght " + bytes.length); }

        final byte byte1 = bytes[0]; // Example: 'A' (most significant byte)
        final byte byte2 = bytes[1]; // Example: (least significant byte for 'A' in little-endian UTF-16)

        // Combine the two bytes into a short, then cast to char
        // Assuming byte1 is the most significant byte and byte2 is the least significant byte
        // This order is typical for big-endian systems, or if you're constructing a specific UTF-16 value.
        return (char) (((byte1 & 0xFF) << 8) | (byte2 & 0xFF));

    }

    public static byte[] createKeyBytes(int keyLengthBytes) {

        // Create a cryptographically strong random number generator
        final SecureRandom secureRandom = new SecureRandom();

        // Create a byte array to hold the key
        final byte[] keyBytes = new byte[keyLengthBytes];

        // Fill the byte array with random bytes
        secureRandom.nextBytes(keyBytes);

        return keyBytes;
    }

    public static byte[] convertInputStreamToBytes(InputStream inputStream) throws IOException {

        if(inputStream == null) {
            return null;
        }

        try { // Automatic resource management (try-with-resources) closes the stream
            return inputStream.readAllBytes();
        }
        finally {
            inputStream.close();
        }
    }

}
