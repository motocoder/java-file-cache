package llc.berserkr.cache.data;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

public interface LinearStreamWriter {
	int read(long index, byte [] b, int offset, int length) throws ReadFailure;
    void write(long index, byte[] in, int off, int len) throws WriteFailure;
    void write(long index, int data) throws WriteFailure;
    int read(long i) throws WriteFailure;
}
