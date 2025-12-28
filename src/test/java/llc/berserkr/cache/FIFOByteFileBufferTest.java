package llc.berserkr.cache;

import llc.berserkr.cache.data.ByteArrayWriter;
import llc.berserkr.cache.data.FIFOByteFileBuffer;
import llc.berserkr.cache.data.RandomAccessFileWriter;
import llc.berserkr.cache.exception.LinearStreamException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
import llc.berserkr.cache.util.StreamUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FIFOByteFileBufferTest {

    @Test
    public void testInMemoryRead() throws IOException {

        final FIFOByteFileBuffer buffer = new FIFOByteFileBuffer(500, new ByteArrayWriter(1000));

        final ByteArrayInputStream in = new ByteArrayInputStream("test".getBytes());

        final OutputStream bufferOut = buffer.getOutputStream();

        StreamUtil.copyTo(in, bufferOut);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final InputStream buffferIn = buffer.getInputStream();

        StreamUtil.copyTo(buffferIn, out);

        assertEquals("test", new String(out.toByteArray(), StandardCharsets.UTF_8));

    }

    @Test
    public void testMemoryAndFileRead() throws IOException {

        final FIFOByteFileBuffer buffer = new FIFOByteFileBuffer(5, new ByteArrayWriter(1000));

        final ByteArrayInputStream in = new ByteArrayInputStream("testing both memory and file".getBytes());

        final OutputStream bufferOut = buffer.getOutputStream();

        StreamUtil.copyTo(in, bufferOut);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final InputStream buffferIn = buffer.getInputStream();

        StreamUtil.copyTo(buffferIn, out);

        assertEquals("testing both memory and file", new String(out.toByteArray(), StandardCharsets.UTF_8));

    }

    @Test
    public void testFileRead() throws IOException {

        final FIFOByteFileBuffer buffer = new FIFOByteFileBuffer(0, new ByteArrayWriter(1000));

        final ByteArrayInputStream in = new ByteArrayInputStream("testing both memory and file".getBytes());

        final OutputStream bufferOut = buffer.getOutputStream();

        StreamUtil.copyTo(in, bufferOut);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final InputStream buffferIn = buffer.getInputStream();

        StreamUtil.copyTo(buffferIn, out);

        assertEquals("testing both memory and file", new String(out.toByteArray(), StandardCharsets.UTF_8));

    }

    @Test
    public void testByteWriter() throws IOException, WriteFailure, ReadFailure {

        final ByteArrayWriter byteWriter = new ByteArrayWriter(10000);

        byteWriter.write(0, "this is a test string".getBytes(StandardCharsets.UTF_8), 0, "this is a test string".getBytes(StandardCharsets.UTF_8).length);

        final byte [] buffer = new byte[1024];

        int readAmount = byteWriter.read(0, buffer, 0, 1024);

        assertEquals("this is a test string".length(), readAmount);
        assertEquals("this is a test string", new String(buffer, 0, readAmount, StandardCharsets.UTF_8));

        assertEquals('h', byteWriter.read(1));

        readAmount = byteWriter.read("this is a test ".length(), buffer, 0, 1024);

        assertEquals("string".length(), readAmount);
        assertEquals("string",  new String(buffer, 0, readAmount, StandardCharsets.UTF_8));

        byteWriter.write(0, "thos as a tast stuing".getBytes(StandardCharsets.UTF_8),0, "thos as a tast stuing".getBytes(StandardCharsets.UTF_8).length);

        readAmount = byteWriter.read(0, buffer, 0, 1024);

        assertEquals("this is a test string".length(), readAmount);
        assertEquals("thos as a tast stuing", new String(buffer, 0, readAmount, StandardCharsets.UTF_8));

    }

    @Test
    public void testFileWriter() throws IOException, WriteFailure, ReadFailure, LinearStreamException {

        final  File tempDir = new File("./file-cache-temp");
        final File testFile = new File(tempDir,"./file-writer-" + UUID.randomUUID());

        final RandomAccessFileWriter fileWriter = new RandomAccessFileWriter(testFile);

        fileWriter.write(0, "this is a test string".getBytes(StandardCharsets.UTF_8), 0, "this is a test string".getBytes(StandardCharsets.UTF_8).length);

        final byte [] buffer = new byte[1024];

        int readAmount = fileWriter.read(0, buffer, 0, 1024);

        assertEquals("this is a test string".length(), readAmount);
        assertEquals("this is a test string", new String(buffer, 0, readAmount, StandardCharsets.UTF_8));

        assertEquals('h', fileWriter.read(1));

        readAmount = fileWriter.read("this is a test ".length(), buffer, 0, 1024);

        assertEquals("string".length(), readAmount);
        assertEquals("string",  new String(buffer, 0, readAmount, StandardCharsets.UTF_8));

        fileWriter.write(0, "thos as a tast stuing".getBytes(StandardCharsets.UTF_8), 0, "this is a test string".getBytes(StandardCharsets.UTF_8).length);

        readAmount = fileWriter.read(0, buffer, 0, 1024);

        assertEquals("this is a test string".length(), readAmount);
        assertEquals("thos as a tast stuing", new String(buffer, 0, readAmount, StandardCharsets.UTF_8));

    }

}
