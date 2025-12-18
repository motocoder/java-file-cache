package llc.berserkr.cache.exception;

public class WriteFailure extends Exception {
    public WriteFailure(String message) {
        super(message);
    }

    public WriteFailure(String message, Exception e) {
        super(message, e);
    }
}
