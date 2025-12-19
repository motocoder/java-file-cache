package llc.berserkr.cache;

import llc.berserkr.cache.collection.SoftHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SoftMemoryCache<Key, Value> implements Cache<Key, Value> {

    @SuppressWarnings("unchecked")
    private final Map<Key, Value> map = new SoftHashMap();
    
    public SoftMemoryCache() {
        
    }
    @Override
    public boolean exists(Key key) {
        return map.get(key) != null;
    }

    @Override
    public Value get(Key key) {
        return map.get(key);
    }

    @Override
    public List<Value> getAll(List<Key> keys) {
        
        final List<Value> returnVals = new ArrayList<Value>();
        
        for(final Key key : keys) {
            
            returnVals.add(this.get(key));
        }
        
        return returnVals;
        
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void remove(Key key) {
        map.remove(key);
    }

    @Override
    public void put(Key key, Value value) {
        map.put(key, value);
    }

}
