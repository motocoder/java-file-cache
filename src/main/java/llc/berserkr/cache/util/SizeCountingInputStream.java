package llc.berserkr.cache.util;

import java.io.IOException;
import java.io.InputStream;

public class SizeCountingInputStream extends WrappingInputStream {

	private long totalRead;
	
	public SizeCountingInputStream(InputStream toWrap) {
		super(toWrap);
	}
	
	@Override
	public int read() throws IOException {
		
		final int returnVal = super.read();
		
		totalRead ++;
		
		return returnVal;
		
	}

	@Override
	public int read(byte[] b) throws IOException {
		
		final int returnVal = super.read(b);
		
		totalRead += returnVal;
		
		return returnVal;
		
	}

	@Override
	public int read(byte[] b, int off, int len)
			throws IOException {
		
		final int returnVal = super.read(b, off, len);
		
		totalRead += returnVal;
		
		return returnVal;
		
	}

	@Override
	public long skip(long n) throws IOException {
		
		final long returnVal = super.skip(n);
		
		totalRead += returnVal;
		
		return returnVal;
		
	}

	public long getTotalRead() {
		return totalRead;
	}

	public void setTotalRead(long totalRead) {
		this.totalRead = totalRead;
	}
	
}
