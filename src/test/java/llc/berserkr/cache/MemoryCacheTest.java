package llc.berserkr.cache;

import llc.berserkr.cache.converter.StringSizeConverter;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StopWatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class MemoryCacheTest {

    @Test
    public void testMemoryCache() {
 
        try {
       
            final Cache<String, String> cache = new MemoryCache<String, String>();
            
            {
                
                assertNull(cache.get("hi"));
                assertFalse(cache.exists("hi"));
                
                final List<String> keys = new ArrayList<String>();
                
                keys.add("hi1");
                keys.add("hi2");
                
                final List<String> results = cache.getAll(keys);
                
                assertEquals(2, results.size());
                assertNull(results.get(0));
                assertNull(results.get(1));
                
                cache.put("hi", "test");
                
                assertTrue(cache.exists("hi"));
                assertEquals("test", cache.get("hi"));
                
                cache.put("hi1", "test1");
                cache.put("hi2", "test2");
                
                final List<String> results2 = cache.getAll(keys);
                
                assertEquals(results2.get(0), "test1");
                assertEquals(results2.get(1), "test2");
                
                cache.clear();
                
            }
            
            {
                assertNull(cache.get("hi"));
                assertFalse(cache.exists("hi"));
                
                final List<String> keys = new ArrayList<String>();
                
                keys.add("hi1");
                keys.add("hi2");
                
                final List<String> results = cache.getAll(keys);
                
                assertEquals(2, results.size());
                
                cache.clear();
                
            }
            
            {
                
                cache.put("hi", "test");
                
                assertEquals("test", cache.get("hi"));
                
                cache.remove("hi");
                
                assertNull(cache.get("hi"));
                
            }
            
        }
        catch(Exception e) {
            
            e.printStackTrace();
            fail("should not get here");
            
        }
        
    }
    
    @Test
    public void testMemoryCacheSizeStuff() {
        
        final Cache<String, String> cache = 
                new MemoryCache<String, String>(
                        new StringSizeConverter() {
                        }, 
                        100);
        try {
            
            cache.put("1", "1");
            cache.put("2", "1");
            cache.put("3", "1");
            cache.put("4", "1");
            cache.put("5", "1");
        
       
            
            assertNull(cache.get("1"));
            assertNull(cache.get("2"));
            assertNull(cache.get("3"));
            assertNotNull(cache.get("4"));
            assertNotNull(cache.get("5"));
            
        } 
        catch (ResourceException e) {
            
            fail("cant get here");
            e.printStackTrace();
            
        }
        
    }
    
    @Test
    public void testMemoryCacheMultiThreaded() { //CA
        
        final HashSet<String> hashset = new HashSet<String>();
        
        final Cache<String, String> cache = 
                new SynchronizedCache<String, String>(
                new MemoryCache<String, String>(
                new StringSizeConverter(), 10000)
                );
            
        for (int x = 0; x < 50; x++){
            
            final int i = x;
            
            new Thread() {
                
                @Override
                public void run() {
                    
                    try {
                        
                        final Random rand = new Random(); 
                        final int size = 1000;
                        
                        for (int x = 0; x < size; x++) {
                            
                            final int rando2 = rand.nextInt();
                            final String key = String.valueOf(rand.nextInt());
                            
                            cache.put(key, String.valueOf(rando2) + "dchvjhgvggvjgvghvjhjgvhgfcgfjgsdfghjkdfgvhjsdfghsdfghsdfg");
                            
                            cache.get(key);  
                           
                            cache.remove(key);
                            
                        }
                        
                        hashset.add(String.valueOf(i));
                        
                        synchronized(hashset) {
                            
                            if (hashset.size() == 100){
                                hashset.notifyAll();
                            }
                            
                        }
                        
                    }
                    catch(ResourceException e) {
                        fail("failed");
                    }
                    
                }
                
            }.start();
            
        }
        
        for (int x = 50; x < 100; x++){
            
            final int i = x;
            
            new Thread() {
                
                @Override
                public void run() {
                    
                    try {
                    
                        final List<String> keys2 = new ArrayList<String>(); 
                        final int size = 1000;
                        
                        for (int y=0; y < size; y++) {  
                            
                            keys2.add(String.valueOf(y));
                            cache.put(String.valueOf(y), y + "{asdfghjsdfghjksdfghjkdfghjdfghjdfgbhjsdfghjsdfghjsdfghjdfghjdfghjksdfghjdfghjdfgh");
                            
                        }
                        
                        cache.getAll(keys2); //test results
                        
                        hashset.add(String.valueOf(i));
                        
                        //need to sync hs before checking size
                        synchronized(hashset) {
                            
                            if (hashset.size() == 100){
                                hashset.notifyAll();
                            }
                            
                        }
                        
                    }
                    catch(ResourceException e) {
                        fail("failed");
                    }
                    
                }
                
            }.start();
        
        }
                        
        
        final StopWatch stop = new StopWatch();
        
        stop.start();
        
        synchronized(hashset) {
            
            while ((hashset.size() != 100) && (stop.getTime() < 10000)){ //need to sync hs before checking size
            
                try {
                    hashset.wait(50); 
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
            }
            
        }
        
        assertEquals(hashset.size(), 100);

    } 
}
