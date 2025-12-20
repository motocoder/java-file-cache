package llc.berserkr.cache;

import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * This class is used for expiring the values. It only works on memory caches. This will not work on a file cache.
 * 
 * TODO this class really has an issue with large and numerous keys. It will degrade performance with O(N)
 * @author Sean Wagner
 *
 * @param <Key>
 * @param <Value>
 */
public class FilePersistedExpiringCache<Value> implements Cache<String, Value>{
    
    private static final Logger logger = LoggerFactory.getLogger(FilePersistedExpiringCache.class);

    private static final String LAST_UPDATED_KEY = "filePersistedExpiringCache.lastUpdated";
    private static final String LAST_UPDATED_PRE_KEY = "filePersistedExpiringCache.lastUpdated.";
    
    private final Cache<String, Value> internal;
    private final long timeout;
    private final ExpiringStates states = new ExpiringStates();
    private final long cleanupTimeout;
    private final Cache<String, byte []> persisting;

    public FilePersistedExpiringCache(
        final Cache<String, Value> internal,
        final Cache<String, byte []> persistingRoot,
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
        
        this.persisting = persistingRoot;

        this.cleanupTimeout = cleanupTimeout;
        this.internal = internal;
        this.timeout = timeout;
        
        try {
            
        	LinkedList<Pair<String, Long>> lastUpdated;
            
            if(persisting.get(LAST_UPDATED_KEY) == null) {
                
                lastUpdated = new LinkedList<Pair<String, Long>>();
                persisting.put(LAST_UPDATED_KEY, DataUtils.serialize(lastUpdated));
                
            } else {
            	lastUpdated = DataUtils.deserialize(persisting.get(LAST_UPDATED_KEY));
            }
            
        } 
        catch (IOException e) {
            throw new RuntimeException("could not create cache 1", e);    
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("could not create cache 2", e);
        } 
        catch (ResourceException e) {
            throw new RuntimeException("could not create cache 3", e);
        }
        
    }
    
    @Override
    public boolean exists(String key) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><4>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();
        final long lastUpdatedTime;
        final boolean wasPerformed = this.cleanup(time);
        
        final byte [] temp = this.persisting.get(LAST_UPDATED_PRE_KEY + key);
        
        if(temp == null) {
            lastUpdatedTime = time;
        }
        else {
            
            Long deserialized;
            
            try {
                deserialized = DataUtils.deserialize(temp);
            } 
            catch (IOException e) {
                
                logger.error("Could not deserialize 1", e);
                deserialized = time;
                
            }
            catch (ClassNotFoundException e) {
                
                logger.error("Could not deserialize 2", e);
                deserialized = time;
                
            }
            
            lastUpdatedTime = deserialized;
            
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
            returnVal = internal.get(key) != null;
        }
        
        return returnVal;
        
    }

    @Override
    public Value get(String key) throws ResourceException {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><5>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();
        final long lastUpdatedTime;            
        final boolean wasPerformed = this.cleanup(time);
        
        final byte [] temp = this.persisting.get(LAST_UPDATED_PRE_KEY + key);
        
        if(temp == null) {
            lastUpdatedTime = time;
        }
        else {
            
            Long deserialized;
            
            try {
                deserialized = DataUtils.deserialize(temp);
            } 
            catch (IOException e) {
                
                logger.error("Could not deserialize 1", e);
                deserialized = time;
                
            }
            catch (ClassNotFoundException e) {
                
                logger.error("Could not deserialize 2", e);
                deserialized = time;
                
            }
            
            lastUpdatedTime = deserialized;
            
        }         
        
        final Value returnVal;
        
        if(!wasPerformed) { //if cleanup wasn't performed check the individual value for expiration.
            
            final long timeToExpire = time - this.timeout;
            
            if(lastUpdatedTime > timeToExpire) {
                returnVal = internal.get(key);
            }
            else {
                
                if(internal.exists(key)) {
                    internal.remove(key);
                }
                
                returnVal = null;
            }
            
        }
        else { //cleanup was performed in this cycle we don't need to check again.
            returnVal = internal.get(key);
        }
        
        return returnVal;
        
    }

    @Override
    public List<Value> getAll(List<String> keys) throws ResourceException {
        
        if(keys == null) {
            throw new NullPointerException("<ExpiringCache><6>, Keys cannot be null");
        }
        
        if(keys.contains(null)) {
            throw new NullPointerException("<ExpiringCache><7>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();
        final Set<String> expired = new HashSet<String>();
            
        if(!this.cleanup(time)) { //if cleanup wasn't performed we need to check all our specific keys.
        
            for(final String key : keys) {
                
                final long lastUpdatedTime;
                
                final byte [] temp = this.persisting.get(LAST_UPDATED_PRE_KEY + key);
                
                if(temp == null) {
                    lastUpdatedTime = time;
                }
                else {
                    
                    Long deserialized;
                    
                    try {
                        deserialized = DataUtils.deserialize(temp);
                    } 
                    catch (IOException e) {
                        
                        logger.error("Could not deserialize 1", e);
                        deserialized = time;
                        
                    }
                    catch (ClassNotFoundException e) {
                        
                        logger.error("Could not deserialize 2", e);
                        deserialized = time;
                        
                    }
                    
                    lastUpdatedTime = deserialized;
                    
                }    
                
                final long timeToExpire = time - this.timeout;
                                    
                if(lastUpdatedTime < timeToExpire) {
                    expired.add(key);
                }
                
            }
            
        }
        
        //remove expired keys
        if(expired.size() > 0) {
            
            for(final String key : keys) {
                internal.remove(key);
            }
            
        }
        
        final List<String> toGet = new ArrayList<String>();
        
        for(final String key : keys) {
            
            if(!expired.contains(key)) {
                toGet.add(key);
            }
            
        }
        
        final List<Value> gotten = internal.getAll(toGet);
        
        final Map<String, Value> finalValues = new HashMap<String, Value>();
        
        for(int i = 0; i < toGet.size(); i++) {
            finalValues.put(toGet.get(i), gotten.get(i));
        }
        
        final List<Value> finalList = new ArrayList<Value>();
        
        for(final String key : keys) {
            finalList.add(finalValues.get(key));
        }
        
        return finalList;
        
    }

    @Override
    public void clear() throws ResourceException {
        
        this.internal.clear();
        this.persisting.clear();
        
        final LinkedList<Pair<String, Long>> lastUpdated = new LinkedList<Pair<String, Long>>();
        
        try {
            persisting.put(LAST_UPDATED_KEY, DataUtils.serialize(lastUpdated));
        } 
        catch (IOException e) {
            throw new ResourceException("Couldn't recreate last updated");
        }
        
    }

    @Override
    public void remove(String key) throws ResourceException  {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><8>, Key cannot be null");
        }

        this.internal.remove(key);
        
        persisting.remove(LAST_UPDATED_PRE_KEY + key);
        
        try {
        
            final LinkedList<Pair<String, Long>> lastUpdated = DataUtils.deserialize(persisting.get(LAST_UPDATED_KEY));
            
            final Set<Pair<String, Long>> toRemove = new HashSet<Pair<String, Long>>();
            
            for(final Pair<String, Long> entry : lastUpdated) {
                
                if(entry.getOne().equals(key)) {
                    toRemove.add(entry);
                }
                
            }
            
            lastUpdated.removeAll(toRemove);            
        
            persisting.put(LAST_UPDATED_KEY, DataUtils.serialize(lastUpdated));
        } 
        catch (IOException e) {
            throw new ResourceException("failed to serialize", e);
        } 
        catch (ClassNotFoundException e) {
            throw new ResourceException("failed to serialize", e);
        }
        
    }

    @Override
    public void put(String key, Value value) throws ResourceException  {
        
        if(key == null) {
            throw new NullPointerException("<ExpiringCache><9>, Key cannot be null");
        }
        
        final long time = System.currentTimeMillis();       

        try {
            this.persisting.put(LAST_UPDATED_PRE_KEY + key, DataUtils.serialize(time));
        }
        catch (IOException e1) {
            
            logger.error("ERROR PUTTING LAST UPDATED 4", e1);
            return;
            
        }
        
        this.cleanup(time);
        
        this.internal.put(key, value);  
        
        if(value == null) {
            persisting.remove(LAST_UPDATED_PRE_KEY + key);
        }
        else {

            try {
                
                final LinkedList<Pair<String, Long>> lastUpdated = DataUtils.deserialize(persisting.get(LAST_UPDATED_KEY));
                
                lastUpdated.addFirst(new Pair<String, Long>(key, time));
                
                persisting.put(LAST_UPDATED_KEY, DataUtils.serialize(lastUpdated));
                
            } 
            catch (IOException e) {
                
                logger.error("ERROR PUTTING LAST UPDATED 1", e);
                return;
                
            }
            catch (ClassNotFoundException e) {
                
                logger.error("ERROR PUTTING LAST UPDATED 2", e);
                return;
                
            }
            catch (ResourceException e) {
                
                logger.error("ERROR PUTTING LAST UPDATED 3", e);
                return;
                
            }
            
        }
        
    }
    
    private boolean cleanup(long currentTime) throws ResourceException {
            
        final LinkedList<Pair<String, Long>> lastUpdated;
        
        if (persisting.get(LAST_UPDATED_KEY) == null) {
        	return true;
        }
        
        try {
            
            lastUpdated = DataUtils.deserialize(persisting.get(LAST_UPDATED_KEY));
        
            final boolean wasPerformed;
            final long timeToExpire = currentTime - this.timeout;
            final long timeToClean = currentTime - this.cleanupTimeout;
            
            if(this.states.getLastCleanup() < timeToClean) { //determine if we need to clean up or not
                
                wasPerformed = true;
                
                final Set<String> toRemove = new HashSet<String>();
                
                while(true) {
                    
                    final Pair<String, Long> current;
                    
                    try {
                        current = lastUpdated.removeLast();
                    }
                    catch(NoSuchElementException e) {
                        break;
                    }
                    
                    final long time = current.getTwo();
                    final String key = current.getOne();
                    
                    if(time > timeToExpire) {
                        
                        lastUpdated.addLast(current);
                        break;
                    }
                    else {
                        
                        toRemove.add(key);
                        
                    }
                    
                }
                
                final Set<String> toRetain = new HashSet<String>();
                
                for(final Pair<String, Long> entry : lastUpdated) {
                    toRetain.add(entry.getOne());
                }
                
                for(final String key : toRemove) {
                    
                    if(!toRetain.contains(key)) {
                        
                        internal.remove(key);
                        this.persisting.remove(LAST_UPDATED_PRE_KEY + key);
                        
                    }

                }
                
                persisting.put(LAST_UPDATED_KEY, DataUtils.serialize(lastUpdated));
    
                this.states.markClean();
                    
            }
            else {
                wasPerformed = false;
            }
            
            return wasPerformed;
                
        } 
        catch (IOException e) {
            
            logger.error("ERROR PUTTING LAST UPDATED 1", e);
            throw new ResourceException("error putting last 1");
            
        }
        catch (ClassNotFoundException e) {
            
            logger.error("ERROR PUTTING LAST UPDATED 2", e);
            throw new ResourceException("error putting last 1");
            
        }
        catch (ResourceException e) {
            
            logger.error("ERROR PUTTING LAST UPDATED 4", e);
            throw new ResourceException("error putting last 1");
            
        }
        
        
        
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
