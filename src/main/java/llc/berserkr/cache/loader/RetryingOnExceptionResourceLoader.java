package llc.berserkr.cache.loader;

import llc.berserkr.cache.exception.OutOfRetriesException;
import llc.berserkr.cache.exception.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import java.util.Set;
import java.util.function.Consumer;

/**
 *  
 *  The RetryingOnNullResourceLoader extends the DefaultResourceLoader
 *  class which is an implementation of the ResourceLoader interface.
 *  It takes a ResourceLoader<Key,Value>, a Callback<Void,Key>, and an
 *  int as parameters. This class will retry calling get or exists if
 *  an exception is thrown.
 *  
 *  ex:
 *  public RetryingOnNullResourceLoader(
 *      final ResourceLoader<Key, Value> internal,
 *      final Callback<Void, Key> onRetrying,
 *      final int retries)
 *      
 *      internal is the ResourceLoader to retry calls to. The onRetrying 
 *      callback is called each time a key throws an exception and has 
 *      to be retried. The retries parameter is the amount of times to 
 *      attempt retrying.
 *      
 */

/**
 * 
 *
 * @param <Key>
 * @param <Value>
 */

public class RetryingOnExceptionResourceLoader<Key, Value> extends DefaultResourceLoader<Key, Value> {

    private static final Logger logger = LoggerFactory.getLogger(RetryingOnExceptionResourceLoader.class);
    
    //TODO better implementation of getAll
    
    private final ResourceLoader<Key, Value> internal;
    private final Consumer<Key> onRetrying;
    private final int retries;
    private final Set<Class<? extends ResourceException>> exceptionTypes;
    
    /**
     * 
     * @param internal
     * @param onRetrying
     * @param retries
     * @param exceptionTypes
     */

    public RetryingOnExceptionResourceLoader(
        final ResourceLoader<Key, Value> internal,
        final Consumer<Key> onRetrying,
        final int retries,
        final Set<Class<? extends ResourceException>> exceptionTypes
    ) {
        
        this.exceptionTypes = exceptionTypes;
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
            
            try {
                return internal.exists(key);
            }
            catch(ResourceException e) {
                
                logger.info("<RetryingOnExceptionResourceLoader><1>, " + "Caught Exception: ", e);
                
                if(exceptionTypes.contains(e.getClass())) {
                    
                    onRetrying.accept(key);
                    
                }
                else {
                    throw e;
                }
                
            }
            
        }
        
        throw new OutOfRetriesException();
        
    }
    
    /**
     * 
     * 
     * @return Value
     * @param key
     * 
     */

    @Override
    public Value get(Key key) throws ResourceException {
        
        for(int i = 0; retries < 0 || i < retries; i++) {
            
            try {
                return internal.get(key);
            }
            catch(ResourceException e) {

                logger.info("<RetryingOnExceptionResourceLoader><2>, " + "Caught Exception: ", e);
                
                if(exceptionTypes.contains(e.getClass())) {
                    
                    onRetrying.accept(key);
                    
                }
                else {
                    throw e;
                }
                
            }
            
        }
        
        throw new OutOfRetriesException();
        
    }

}
