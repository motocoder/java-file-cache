package llc.berserkr.cache.data;

import llc.berserkr.cache.exception.LinearStreamException;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RandomAccessFileWriter implements LinearStreamWriter {
	
	private final RandomAccessFile random;
	private final String ACCESS_MODE = "rws";
	
	public RandomAccessFileWriter(final File file) throws LinearStreamException {
		
		try {
			random = new RandomAccessFile(file, ACCESS_MODE);
		}
		catch (FileNotFoundException e) {
			throw new LinearStreamException(e);
		}
		
	}
	
	private void seek(final long index) throws ReadFailure {
		try {
			random.seek(index);
		} 
		catch (IOException e) {
			throw new ReadFailure("Failed to seek", e);
		}
	}
	
	@Override
    public int read(long index, byte [] b, int offset, int length) throws ReadFailure {

        try {

            this.seek(index);

            return random.read(b, offset, length);
        } catch (IOException e) {
            throw new ReadFailure("failed to read", e);
        }

    }

    @Override
    public void write(long index, byte[] in, int off, int len) throws WriteFailure {
        try {

            this.seek(index);
            this.random.write(in, off, len);

        } catch (ReadFailure | IOException e) {
            throw new WriteFailure("failed to write", e);
        }
    }

    private void write(final byte[] in) throws WriteFailure {
		try {
			random.write(in);
		} 
		catch (IOException e) {
			throw new WriteFailure("faild", e);
		}
	}

    @Override
    public void write(long index, int data) throws WriteFailure {
        try {
            this.seek(index);
            random.write(data);
        } catch (ReadFailure | IOException e) {
            throw new WriteFailure("failed to write", e);
        }
    }

    @Override
    public int read(long index) throws WriteFailure {
        try {

            this.seek(index);

            return random.readByte();
        }
        catch (IOException e) {
            throw new WriteFailure("failed to read", e);
        }
        catch (ReadFailure e) {
            throw new WriteFailure("write failure", e);
        }
    }

    public void close() throws LinearStreamException {
		try {
			random.close();
		}
		catch (IOException e) {
			throw new LinearStreamException(e);
		}
	}

}
