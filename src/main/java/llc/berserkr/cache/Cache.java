package llc.berserkr.cache;

import llc.berserkr.cache.exception.CacheException;

public interface Cache<TKey, TValue> {

	void clear() throws CacheException;
	void remove(TKey key) throws CacheException;
	void put(TKey key, TValue value) throws CacheException;
    boolean exists(TKey key) throws CacheException;
    TValue get(TKey key) throws CacheException;

}
