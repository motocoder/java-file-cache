package llc.berserkr.cache.data;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FIFOByteFileBuffer {

    private final byte[] memoryBuffer;
    private final LinearStreamWriter overflowWriter;

    private int position;
    private int positionWrite;

    public FIFOByteFileBuffer(int memoryBufferSize, LinearStreamWriter overflowWriter) {

        this.memoryBuffer = new byte[memoryBufferSize];
        this.overflowWriter = overflowWriter;

    }

    public void reset() {
        position = 0;
        positionWrite = 0;
    }

    private int readFromFile (byte[] b, int off, int len) throws IOException {

        //reading past the memory buffer into the file.

        //get the position in the file we are reading from
        int positionInFile = position - memoryBuffer.length;

        //get the max amount we can read
        int readSizeMax = (positionWrite - memoryBuffer.length) - positionInFile;

        int readSize;

        if (readSizeMax <= len) {

            //we can read what is left
            readSize = readSizeMax;
        } else { //we can't read it all
            readSize = len;
        }

        try {

            overflowWriter.read(positionInFile, b, off, readSize);

            position += readSize;

            return readSize;

        } catch (ReadFailure e) {
            throw new IOException(e);
        }

    }

    public OutputStream getOutputStream() {

        return new OutputStream() {

            @Override
            public void write(int b) throws IOException {

                if(positionWrite >= memoryBuffer.length) {
                    try {
                        overflowWriter.write(memoryBuffer.length - positionWrite, b);
                    } catch (WriteFailure e) {
                        throw new IOException("failed to write buffer", e);
                    }
                }
                else {
                    memoryBuffer[positionWrite++] = (byte) b;
                }
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {

                if(positionWrite + len >= memoryBuffer.length) {

                    final int wrote;

                    //if we are starting in the memory buffer but need to write more than what fits.
                    if(positionWrite <  memoryBuffer.length) {

                        wrote = memoryBuffer.length - positionWrite;

                        System.arraycopy(b, off, memoryBuffer, positionWrite, wrote);

                        positionWrite += wrote;
                        len -= wrote;

                    }
                    else {
                        wrote = 0;
                    }

                    try {
                        overflowWriter.write(positionWrite - memoryBuffer.length, b, off + wrote, len);

                        positionWrite += len;

                    } catch (WriteFailure e) {
                        throw new IOException("failed to write buffer", e);
                    }

                }
                else {

                    System.arraycopy(b, off, memoryBuffer, positionWrite, len);
                    positionWrite += len;

                }

            }
        };

    }

    public InputStream getInputStream() {
        return new InputStream() {

            @Override
            public int available() {
                return positionWrite - position;
            }

            @Override
            public int read() throws IOException {

                final int available = available();

                if (available <= 0) {
                    return -1;
                }

                if (position >= memoryBuffer.length) {

                    try {
                        return overflowWriter.read(position - memoryBuffer.length);
                    } catch (WriteFailure e) {
                        throw new IOException(e);
                    }
                    finally {
                        position++;
                    }
                }
                else {
                    return memoryBuffer[position++];
                }
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {

                final int available = available();

                if (available <= 0) {
                    return -1;
                }

                if (available < len) {
                    len = available;
                }

                //reading position starts at the beginning of the byte array buffer
                // check to see if we are reading past that buffer into the file
                if (position >= memoryBuffer.length) {
                    return readFromFile(b, off, len);
                } else {

                    //we are still in the memory buffer
                    if (position + len < memoryBuffer.length) {

                        System.arraycopy(memoryBuffer, position, b, off, len);

                        position += len;

                        return len;

                    } else {
                        //array doesn't contain it all, read the rest from memory
                        final int memoryLen = memoryBuffer.length - position;
                        System.arraycopy(memoryBuffer, position, b, off, memoryLen);

                        position += (memoryBuffer.length - position);;
                        //then the rest from file.
                        return memoryLen + readFromFile(b, off + memoryLen, len - memoryLen);
                    }

                }

            }
        };
    }
}
