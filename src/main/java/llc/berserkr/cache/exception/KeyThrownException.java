package llc.berserkr.cache.exception;

public class KeyThrownException extends ResourceException {
    
    private static final long serialVersionUID = -4096733198946054462L;
    
    private final Object key;
    private final Throwable thrown;

    public KeyThrownException(final Object key, Throwable thrown) {
        
        this.key = key;
        this.thrown = thrown;
        
    }

    public Object getKey() {
        return key;
    }

    public Throwable getThrown() {
        return thrown;
    }
    
}
