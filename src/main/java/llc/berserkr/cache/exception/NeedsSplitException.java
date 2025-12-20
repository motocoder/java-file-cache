package llc.berserkr.cache.exception;

public class NeedsSplitException extends RuntimeException {

    private final long address;
    private int segmentSize;

    public NeedsSplitException(long address, int segmentSize) {
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
