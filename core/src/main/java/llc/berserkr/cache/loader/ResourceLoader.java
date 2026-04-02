package llc.berserkr.cache.loader;


import llc.berserkr.cache.exception.ResourceException;

import java.util.List;

/**
 *  
 *  The ResourceLoader is an interface used to load
 *  various resources using a key and value based mapping system.
 *  
 *  Get(key) - returns a value
 *  Exists(key) - returns a boolean
 *  getAll(List<Key>) - returns a list of values
 *
 */

/**
 * 
 * @author seanwagner
 *
 * @param <TKey>
 * @param <TValue>
 */
public interface ResourceLoader<TKey, TValue> {
	
    /**
     * 
     * @param key
     * @return boolean
     * @throws ResourceException
     */
	boolean exists(TKey key) throws ResourceException;
	
	/**
	 * 
	 * @param key
	 * @return TValue
	 * @throws ResourceException
	 */
	TValue get(TKey key) throws ResourceException;
	
	/**
	 * 
	 * @param keys
	 * @return List TValue> - The list returned must have the same length as the input list of keys
	 *           and the order values returned must match their associated keys.
	 * @throws ResourceException
	 */
	List<TValue> getAll(List<TKey> keys) throws ResourceException;

}
