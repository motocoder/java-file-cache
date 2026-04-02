package llc.berserkr.cache.exception;

import java.util.List;

public class SpaceFragementedException extends RuntimeException {

    private final long address;
    private int segmentSize;

    public SpaceFragementedException(long address, int segmentSize) {
        super("space fragemented");

        this.address = address;
        this.segmentSize = segmentSize;

    }

    public int getSegmentSize() {
        return segmentSize;
    }

    public long getAddress() {
        return address;
    }
}
