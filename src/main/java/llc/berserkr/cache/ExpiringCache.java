package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;

import java.util.*;

/**
 * This class is used for expiring the values. It only works on memory caches. This will not work on a file cache.
 * 
 * @author Sean Wagner
 *
 * @param <Key>
 * @param <Value>
 * @deprecated
 */
@Deprecated
public class ExpiringCache<Key, Value> implements Cache<Key, Value>{
    
    private final LinkedList<Pair<Key, Long>> lastUpdated = new LinkedList<Pair<Key, Long>>();
    
    private final Map<Key, Long> lastUpdatedMap = new HashMap<Key, Long>();
    
    private final Cache<Key, Value> internal;
    private final long timeout;
    private final ExpiringStates states = new ExpiringStates();
    private final long cleanupTimeout;

    public ExpiringCache(
        final Cache<Key, Value> internal,
        final long timeout,
        final long cleanupTimeout
    ) {
                
        if(internal == null) {
            throw new NullPointerException("<ExpiringCache><1>, Internal cannot be null");
        }
        
        if(timeout <= 0) {
            throw new IllegalArgumentException("<ExpiringCache><2>, Timeout must be > 0");
        }
        
        if(cleanupTimeout <= 0) {
            throw new IllegalArgumentException("<ExpiringCache><3>, CleanupTimeout must be > 0");
        }
        
        this.cleanupTimeout = cleanupTimeout;
        this.internal = internal;
        this.timeout = timeout;
        
    }
    
    @Override
    public boolean exists(Key key) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><4>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();
        final long lastUpdatedTime;            
        final boolean wasPerformed = this.cleanup(time);
        
        final Long temp = this.lastUpdatedMap.get(key);
        
        if(temp == null) {
            lastUpdatedTime = time;
        }
        else {
            lastUpdatedTime = temp;
        }            
        
        final boolean returnVal;
        
        if(!wasPerformed) { //if cleanup wasn't performed check the individual value for expiration.
            
            final long timeToExpire = time - this.timeout;
            
            if(lastUpdatedTime > timeToExpire) {
                returnVal = internal.exists(key);
            }
            else {
                returnVal = false;
            }
            
        }
        else { //cleanup was performed in this cycle we don't need to check again.
            returnVal = internal.exists(key);
        }
        
        return returnVal;
        
    }

    @Override
    public Value get(Key key) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><5>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();
        final long lastUpdatedTime;            
        final boolean wasPerformed = this.cleanup(time);
        
        final Long temp = this.lastUpdatedMap.get(key);
        
        if(temp == null) {
            lastUpdatedTime = time;
        }
        else {
            lastUpdatedTime = temp;
        }            
        
        final Value returnVal;
        
        if(!wasPerformed) { //if cleanup wasn't performed check the individual value for expiration.
            
            final long timeToExpire = time - this.timeout;
            
            if(lastUpdatedTime > timeToExpire) {
                returnVal = internal.get(key);
            }
            else {
                returnVal = null;
            }
            
        }
        else { //cleanup was performed in this cycle we don't need to check again.
            returnVal = internal.get(key);
        }
        
        return returnVal;
        
    }

    @Override
    public List<Value> getAll(List<Key> keys) throws ResourceException {
        
        if(keys == null) {
            throw new NullPointerException("<ExpiringCache><6>, Keys cannot be null");
        }
        
        if(keys.contains(null)) {
            throw new NullPointerException("<ExpiringCache><7>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();
        final Set<Key> expired = new HashSet<Key>();
            
        if(!this.cleanup(time)) { //if cleanup wasn't performed we need to check all our specific keys.
        
            for(final Key key : keys) {
                
                final long lastUpdatedTime;
                
                final Long temp = this.lastUpdatedMap.get(key);
                
                if(temp == null) {
                    lastUpdatedTime = time; 
                }
                else {
                    lastUpdatedTime = temp;
                }
                
                final long timeToExpire = time - this.timeout;
                                    
                if(lastUpdatedTime < timeToExpire) {
                    expired.add(key);
                }
                
            }
            
        }
        
        final List<Key> toGet = new ArrayList<Key>();
        
        for(final Key key : keys) {
            
            if(!expired.contains(key)) {
                toGet.add(key);
            }
            
        }
        
        final List<Value> gotten = internal.getAll(toGet);
        
        final Map<Key, Value> finalValues = new HashMap<Key, Value>();
        
        for(int i = 0; i < toGet.size(); i++) {
            finalValues.put(toGet.get(i), gotten.get(i));
        }
        
        final List<Value> finalList = new ArrayList<Value>();
        
        for(final Key key : keys) {
            finalList.add(finalValues.get(key));
        }
        
        return finalList;
        
    }

    @Override
    public void clear() throws ResourceException {
        
        this.internal.clear();
        
    }

    @Override
    public void remove(Key key) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><8>, Key cannot be null");
        }

        this.internal.remove(key);
        
    }

    @Override
    public void put(Key key, Value value) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><9>, Key cannot be null");
        }
        
        
        if(value == null) {
            throw new NullPointerException("<ExpiringCache><10>, Value cannot be null");
        }
        
        final long time = System.currentTimeMillis();       
        this.lastUpdatedMap.put(key, time);
        this.lastUpdated.addFirst(new Pair<Key, Long>(key, time));
        
        this.internal.put(key, value);  
        
    }
    
    private boolean cleanup(long currentTime) throws ResourceException  {
            
        
        
        final boolean wasPerformed;
        final long timeToExpire = currentTime - this.timeout;
        final long timeToClean = currentTime - this.cleanupTimeout;
        
        if(this.states.getLastCleanup() < timeToClean) { //determine if we need to clean up or not
            
            wasPerformed = true;
            
            final Set<Key> toRemove = new HashSet<Key>();
            
            while(true) {
                
                final Pair<Key, Long> current;
                
                if(lastUpdated.size() <= 0) {
                    break;
                }
                
                current = lastUpdated.removeLast();
                
                final long time = current.getTwo();
                final Key key = current.getOne();
                
                if(time > timeToExpire) {
                    
                    lastUpdated.addLast(current);
                    break;
                }
                else {
                    toRemove.add(key);
                }
                
            }
            
            final Set<Key> toRetain = new HashSet<Key>();
            
            for(final Pair<Key, Long> entry : lastUpdated) {
                toRetain.add(entry.getOne());
            }
            
            for(final Key key : toRemove) {
                
                if(!toRetain.contains(key)) {
                    
                    internal.remove(key);
                    lastUpdatedMap.remove(key);
                    
                }
                
            }

            this.states.markClean();
        }
        else {
            wasPerformed = false;
        }
        
        return wasPerformed;
        
    }
    
    private static class ExpiringStates {
        
        private long lastCleanup = System.currentTimeMillis();
        
        public ExpiringStates() {
            
        }
        
        public void markClean() {
            this.lastCleanup = System.currentTimeMillis();
        }

        public long getLastCleanup() {
            return lastCleanup;
        }
        
        
   
    }

}
