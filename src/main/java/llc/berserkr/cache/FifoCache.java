package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.provider.PushProvider;
import llc.berserkr.cache.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Provided a FIFO implementation using some provided caches.
 * 
 * This class is not synchronized.
 * 
 * @author seanwagner
 *
 * @param <Item>
 */
public class FifoCache<Item> implements Queue<Item> {

    private static final Logger logger = LoggerFactory.getLogger(FifoCache.class);
    
    private final Cache<Long, Item> cache;
    private final ResourceProvider<Long> idProvider;
    private final PushProvider<LinkedList<Long>> idQueueProvider;
    
    /**
     * 
     * @param idQueueProvider
     * @param cache
     */
    public FifoCache(
        final PushProvider<LinkedList<Long>> idQueueProvider, 
        final Cache<Long, Item> cache
    ) {
        
        this.cache = cache;
        this.idQueueProvider = idQueueProvider;
        
        long largest = 0;

        try {

            for(final Long id : idQueueProvider.provide()) {

                if(id > largest) {
                    largest = id;
                }

            }

        }
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><1>, There is something wrong with your provider");
        }

        final long largestFinal = largest;
        
        idProvider = new ResourceProvider<Long>() {

            private long id = largestFinal;
            
            @Override
            public boolean exists() {
                return true;
            }

            @Override
            public Long provide() {
                return id++;
            }

        };
        
    }

    @Override
    public synchronized int size() {
        
        try {
            return idQueueProvider.provide().size();
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><2>, There is something wrong with your provider");
        }
        
    }

    @Override
    public synchronized boolean isEmpty() {
        
        try {
            return idQueueProvider.provide().isEmpty();
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><3>, There is something wrong with your provider");
        }
        
    }

    @Override
    public synchronized boolean contains(Object o) {
        try {
            return idQueueProvider.provide().contains(o);
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><4>, There is something wrong with your provider");
        }
        
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public synchronized Iterator iterator() {
        throw new RuntimeException("<FifoCache><5>, Not supported");
    }

    @Override
    public synchronized Object[] toArray() {
        
        try {
            
            final Object [] array = new Object[idQueueProvider.provide().size()];
            
            for(int i = 0; i < array.length; i++) {
                
                try {
                    array[i] = cache.get(idQueueProvider.provide().get(i));
                }
                catch (ResourceException e) {
                    
                    logger.error("<FifoCache><6>, ERROR:", e);
                    throw new RuntimeException("<FifoCache><7>, " + e);
                    
                }
            }
            
            return array;
            
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><8>, There is something wrong with your provider");
        }
        
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized Object[] toArray(Object[] a) {
        return toArray();
    }

    @Override
    public synchronized boolean remove(Object o) {
        throw new RuntimeException("<FifoCache><9>, not supported");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public synchronized boolean containsAll(Collection c) {
        throw new RuntimeException("<FifoCache><10>, not supported");
    }

    @Override
    public synchronized boolean addAll(Collection c) {

        boolean returnVal = true;

        for(Object cItem : c) {

            if(!this.add((Item) cItem)) {
                returnVal = false;
            }

        }

        return returnVal;

    }

    @SuppressWarnings("rawtypes")
    @Override
    public synchronized boolean removeAll(Collection c) {
        throw new RuntimeException("<FifoCache><12>, not supported");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public synchronized boolean retainAll(Collection c) {
        throw new RuntimeException("<FifoCache><13>, not supported");
    }

    @Override
    public synchronized void clear() {
        
        try {
            
            final LinkedList<Long> idQueue = idQueueProvider.provide();
            
            cache.clear();
            idQueue.clear();
            
            idQueueProvider.push(idQueue);
            
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><14>, There is something wrong with your provider");
        }
        
    }

    @Override
    public synchronized boolean add(Item e) {
        
        if(e == null) {
            throw new NullPointerException("Cannot add null");
        }
        
        try {
            
            final long id = idProvider.provide();
            final LinkedList<Long> idQueue = idQueueProvider.provide();
            
            cache.put(id, e);
            idQueue.add(id);
            
            idQueueProvider.push(idQueue);
            
            return true;
            
        } 
        catch (ResourceException t) {
            throw new RuntimeException("<FifoCache><15>, There is something wrong with your provider");
        }
        
    }

    @Override
    public synchronized boolean offer(Item e) {
        
        add(e);
        
        return false;
        
    }

    @Override
    public synchronized Item remove() {
        
        final Item item = poll();
        
        if(item == null) {
            throw new IndexOutOfBoundsException("<FifoCache><16>, nothing in the FIFO");
        }
        else {
            return item;
        }
        
    }

    @Override
    public synchronized Item poll() {
        
        try {
            
            final LinkedList<Long> idQueue = idQueueProvider.provide();
            
            if(idQueue.size() > 0) {
                
                final Long id = idQueue.remove(0);
                
                idQueueProvider.push(idQueue);
                
                try {
                    
                    final Item item = cache.get(id);
                    
                    cache.remove(id);
                    
                    return item;
                    
                } 
                catch (ResourceException e) {
                    throw new RuntimeException("<FifoCache><17>, " + e);
                }
                
                
            }
            else {
                return null;
            }
            
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><18>, There is something wrong with your provider");
        }
       
        
    }

    @Override
    public synchronized Item element() {
        
        final Item item = peek();
        
        if(item == null) {
            throw new IndexOutOfBoundsException("<FifoCache><19>, nothing in the FIFO");
        }
        else {
            return item;
        }
        
    }

    @Override
    public synchronized Item peek() {
        
        try {
           
            final LinkedList<Long> idQueue = idQueueProvider.provide();
            
            if(idQueue.size() > 0) {
                
                final Long id = idQueue.get(0);
                
                idQueueProvider.push(idQueue);
                
                try {
                    
                    final Item item = cache.get(id);
    
                    return item;
                    
                } 
                catch (ResourceException e) {
                    throw new RuntimeException("<FifoCache><20>, " + e);
                }
                
            }
            else {
                return null;
            }
            
        } 
        catch (ResourceException e) {
            throw new RuntimeException("<FifoCache><21>, There is something wrong with your provider");
        }
        
    }

}
