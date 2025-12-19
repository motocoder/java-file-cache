package llc.berserkr.cache;

import llc.berserkr.cache.exception.CacheException;
import llc.berserkr.cache.exception.ResourceException;

import java.util.List;

public interface Cache<Key, Value> {

	void clear() throws ResourceException;
	void remove(Key key) throws ResourceException;
	void put(Key key, Value value) throws ResourceException;
    boolean exists(Key key) throws ResourceException;
    Value get(Key key) throws ResourceException;
    List<Value> getAll(List<Key> keys) throws ResourceException;
}
