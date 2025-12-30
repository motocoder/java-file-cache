package llc.berserkr.cache.loader;

import llc.berserkr.cache.exception.ResourceException;

import java.util.ArrayList;
import java.util.List;

/**
 *  
 *  DefaultResourceLoader is an implementation of ResourceLoader with
 *  a two overridden methods. The exists method returns the get method
 *  if the get method does not return a null value. The getAll method 
 *  returns multiple values in a list.
 *      
 */

public abstract class DefaultResourceLoader<TKey, TValue> implements ResourceLoader<TKey, TValue> {

    /**
     * This method returns a boolean if the value assigned to the
     * specified key parameter is not null.
     * 
     * @return boolean
     * 
     */
    
	@Override
	public boolean exists(TKey key) throws ResourceException {
		return get(key) != null;
	}
	
    /**
     * This method returns a List of TValues assigned to the
     * specified keys parameter.
     * 
     * @return List TValue>
     * 
     */

	@Override
	public List<TValue> getAll(List<TKey> keys) throws ResourceException {
		
		final List<TValue> returnVals = new ArrayList<TValue>();
		
		for(TKey key : keys) {
			returnVals.add(get(key));
		}
		
		return returnVals;
		
	}

}
