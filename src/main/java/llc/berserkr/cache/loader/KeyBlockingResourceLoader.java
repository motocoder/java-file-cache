package llc.berserkr.cache.loader;

import llc.berserkr.cache.exception.ResourceException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyBlockingResourceLoader<Key, Value> implements ResourceLoader<Key, Value> {

    private final Set<Key> existsOut = new HashSet<Key>();
    private final Set<Key> out = new HashSet<Key>();
    
    private final ResourceLoader<Key, Value> internal;
    
    public KeyBlockingResourceLoader(ResourceLoader<Key, Value> internal) {
        this.internal = internal;
    }
    
    @Override
    public boolean exists(Key key) throws ResourceException {
        
        //Block until key lock aquired
        while(true) {
            
            synchronized(existsOut) {
                
                if(!existsOut.contains(key)) {
                    
                    //key lock aquired
                    existsOut.add(key);
                    break;
                    
                }
                else {
                    
                    try {
                        existsOut.wait();
                    }
                    catch (InterruptedException e) {
                        throw new ResourceException("Interrupted");
                    }
                    
                }
                
            }
            
        }
        
        try {
            return internal.exists(key);
        }
        finally {
            
            synchronized(existsOut) {
                
                existsOut.remove(key);
                existsOut.notifyAll();
                
            }
            
        }
        
    }

    @Override
    public Value get(Key key) throws ResourceException {
        
        //Block until key lock aquired
        while(true) {
            
            synchronized(out) {
                
                if(!out.contains(key)) {
                    
                    //key lock aquired
                    out.add(key);
                    break;
                    
                }
                else {
                    
                    try {
                        out.wait();
                    }
                    catch (InterruptedException e) {
                        throw new ResourceException("Interrupted");
                    }
                    
                }
                
            }
            
        }
        
        try {
            return internal.get(key);
        }
        finally {
            
            synchronized(out) {
                
                out.remove(key);
                out.notifyAll();
                
            }
            
        }
    }

    @Override
    public List<Value> getAll(List<Key> keys) throws ResourceException {
        
        //Block until key lock aquired
        while(true) {
            
            synchronized(out) {
                
                if(!out.containsAll(keys)) {
                    
                    //key lock aquired
                    out.addAll(keys);
                    break;
                    
                }
                else {
                    
                    try {
                        out.wait();
                    }
                    catch (InterruptedException e) {
                        throw new ResourceException("Interrupted");
                    }
                    
                }
                
            }
            
        }
        
        try {
            return internal.getAll(keys);
        }
        finally {
            
            synchronized(out) {
                
                out.removeAll(keys);
                out.notifyAll();
                
            }
            
        }
    }

}
