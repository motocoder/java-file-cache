package llc.berserkr.cache;

import java.util.ArrayList;
import java.util.List;

public class AlwaysNullCache<Key, Value> implements Cache<Key, Value> {

    private final boolean exists;
    
    public AlwaysNullCache() {
        this.exists = true;
    }
    
    public AlwaysNullCache(final boolean exists) {
        this.exists = exists;
    }
    @Override
    public void clear() {
        //nothing to clear
    }

    @Override
    public void remove(Key key) {
        //nothing to remove        
    }

    @Override
    public void put(Key key, Value value) {
        //nothing to put
    }

    @Override
    public boolean exists(Key key) {
        return exists;
    }

    @Override
    public Value get(Key key) {
        return null;
    }

    @Override
    public List<Value> getAll(List<Key> keys) {
        final List<Value> returnVals = new ArrayList<Value>();

        for(int i = 0; i < keys.size(); i++) {
            returnVals.add(null);
        }

        return returnVals;

    }

}
