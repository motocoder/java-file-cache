package llc.berserkr.cache.data;

import llc.berserkr.cache.exception.LinearStreamException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

public class ByteArrayWriter implements LinearStreamWriter {

    private int length;

	private ByteBuffer bufferMainArray;

	public ByteArrayWriter(final int originalSize) {
		this.bufferMainArray =  ByteBuffer.allocate(originalSize);
	}

	private int length() {
		return bufferMainArray.capacity();
	}

	@Override
	public int read(long index, byte[] buff, int offset, int amountToRead) throws ReadFailure {

		while ((index + amountToRead) >= length()) {
            expandBuffer(Math.max(bufferMainArray.capacity() * 2, (int) index + amountToRead));
		}

		bufferMainArray.position((int)index);

		try {

            if(!(index + amountToRead < length)) {
                amountToRead = length - (int)index;
            }

			bufferMainArray.get(buff, offset, amountToRead);

            return amountToRead;
		}
		catch (BufferUnderflowException e) {
			throw new ReadFailure("failed to read", e);
		}

	}

	private void expandBuffer(int size) {

		final ByteBuffer clone = ByteBuffer.allocate(size);
		bufferMainArray.rewind();
		clone.put(bufferMainArray);
		bufferMainArray = clone;

	}

    @Override
    public void write(long index, byte[] in, int off, int len) throws WriteFailure {

        if(index + len >= length) {
            this.length = len + (int) index;
        }

        while ((index + len) >= length()) {
            expandBuffer(Math.max(bufferMainArray.capacity() * 2, (int) index + len));
        }

        bufferMainArray.position((int) index);
        bufferMainArray.put(in, off, len);

    }

    @Override
    public void write(long index, int in) throws WriteFailure {

        while ((index + 1) >= length()) {
            expandBuffer((int) Math.max(bufferMainArray.capacity() * 2, (int) index + 1));
        }

        bufferMainArray.position((int) index);
        bufferMainArray.put((byte)in);

    }

    @Override
    public int read(long i) throws WriteFailure {

        bufferMainArray.position((int)i);

        return bufferMainArray.get();
    }
}
