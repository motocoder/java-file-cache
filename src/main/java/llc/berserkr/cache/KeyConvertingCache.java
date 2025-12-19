package llc.berserkr.cache;

import llc.berserkr.cache.converter.Converter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.loader.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

/*
 * This class allows access to a cache via an interface key type (TKey)
 * which is converted to the cache's key type (TOldKey).  
 */

public class KeyConvertingCache<Key, OldKey, Value> implements Cache<Key, Value> {

    private final Cache<OldKey, Value> internal;
    private final Converter<Key, OldKey> converter;

    public KeyConvertingCache(
        final Cache<OldKey, Value> internal,
        final Converter<Key, OldKey> converter
    ) {
        
        if(converter == null) {
            throw new NullPointerException("<KeyConvertingCache><1>, converter must not be null");
        }
        
        if(internal == null) {
            throw new NullPointerException("<KeyConvertingCache><2>, Internal must not be null");
        }
                
        this.converter = converter;
        this.internal = internal;
        
    }
        
    public KeyConvertingCache(
         final ResourceLoader<OldKey, Value> internal,
        final Converter<Key, OldKey> converter
    ) {
        
        if(internal == null) {
            throw new NullPointerException("<KeyConvertingCache><3>, Internal must not be null");
        }
        
        if(converter == null) {
            throw new NullPointerException("<KeyConvertingCache><4>, converter must not be null");
        }
        
        this.converter = converter;
        this.internal = new ResourceLoaderCache<OldKey, Value>(internal);
        
    }
        

	@Override
	public boolean exists(final Key key) throws ResourceException {
		return internal.exists(converter.convert(key));
	}

	@Override
	public Value get(final Key key) throws ResourceException {
		return internal.get(converter.convert(key));
	}

	@Override
	public List<Value> getAll(final List<Key> keys) throws ResourceException {

		final List<OldKey> oldKeys = new ArrayList<OldKey>();

		for(final Key key: keys) {
			oldKeys.add(converter.convert(key));
		}

		return internal.getAll(oldKeys);
		
	}

	@Override
	public void clear() throws ResourceException {
		internal.clear();
	}

	@Override
	public void remove(final Key key) {
		
		try {
			internal.remove(converter.convert(key));
		} 
		catch (final ResourceException e) {
            throw new RuntimeException("<KeyConvertingCache><1>, " + e);
        }
		
	}

	@Override
	public void put(final Key key, final Value value) {
		
		try {
			internal.put(converter.convert(key), value);
		} 
		catch (final ResourceException e) {
            throw new RuntimeException("<KeyConvertingCache><1>, " + e);
        }
		
	}

}
