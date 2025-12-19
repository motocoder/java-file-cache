package llc.berserkr.cache.loader;

import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 *  
 *  The RetryingOnNullResourceLoader extends the DefaultResourceLoader
 *  class which is an implementation of the ResourceLoader interface.
 *  It takes a ResourceLoader<Key,Value>, a Callback<Void,Key>, and an
 *  int as parameters. This class will retry calling get or exists if
 *  a null value is returned.
 *  
 *  ex:
 *  public RetryingOnNullResourceLoader(
 *      final ResourceLoader<Key, Value> internal,
 *      final Callback<Void, Key> onRetrying,
 *      final int retries)
 *      
 *      internal is the ResourceLoader to retry calls to. The onRetrying 
 *      callback is called each time a key returns null and has to be retried. 
 *      The retries parameter is the amount of times to attempt retrying.
 *      
 */

/**
 * 
 *
 * @param <Key>
 * @param <Value>
 */

public class RetryingOnNullResourceLoader<Key, Value> extends DefaultResourceLoader<Key, Value> {

    private static final Logger logger = LoggerFactory.getLogger(RetryingOnNullResourceLoader.class);
    
    //TODO better implementation of getAll
    
    private final ResourceLoader<Key, Value> internal;
    private final Consumer<Key> onRetrying;
    private final int retries;

    /**
     * 
     * @param internal
     * @param onRetrying
     * @param retries
     */
    public RetryingOnNullResourceLoader(
        final ResourceLoader<Key, Value> internal,
        final Consumer<Key> onRetrying,
        final int retries
    ) {
        
        this.retries = retries;
        this.internal = internal;
        this.onRetrying = onRetrying;
        
    }
    
    /**
     * 
     * @param key
     * @return boolean
     * 
     */
    
    @Override
    public boolean exists(Key key) throws ResourceException {
        
        for(int i = 0; retries < 0 || i < retries; i++) {
            
            boolean val = internal.exists(key);
            
            if(val) {
                return val;
            }
            else {
                
                onRetrying.accept(key);
                
            }
            
        }
        
        return false;
        
    }
    
    /**
     * 
     * @param key
     * @return Value
     * 
     */

    @Override
    public Value get(Key key) throws ResourceException {

        for(int i = 0; retries < 0 || i < retries; i++) {
            
            Value val = internal.get(key);
            
            if(val != null) {
                return val;
            }
            else {
                
                onRetrying.accept(key);
                
            }
            
        }
        
        return null;
        
    }

}
