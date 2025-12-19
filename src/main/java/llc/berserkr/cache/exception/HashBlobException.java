package llc.berserkr.cache.exception;

public class HashBlobException extends Exception {

    private static final long serialVersionUID = 8197261382035106729L;
    
    public HashBlobException(final String msg) {
        super(msg);
    }
    
    public HashBlobException(final String msg, final Throwable e) {
        super(msg, e);
    }

}
