package llc.berserkr.cache.exception;

public class CacheException extends Exception {
    public CacheException(String failure, Exception e) {
        super(failure, e);
    }
}
