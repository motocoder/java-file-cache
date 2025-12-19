package llc.berserkr.cache.exception;

public class ResourceException extends Exception {

    public ResourceException(String string) {
        super(string);
    }
    
    public ResourceException(String string, Exception e) {
        super(string, e);
    }

    public ResourceException(Throwable e) {
        super(e);
    }

    protected ResourceException() {
    }

    private static final long serialVersionUID = -9007563624704352499L;
    
}
