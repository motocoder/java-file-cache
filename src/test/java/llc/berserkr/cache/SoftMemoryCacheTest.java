package llc.berserkr.cache;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SoftMemoryCacheTest {

    @Test
    public void testMemoryCache() {
            
        try {
            
            final Cache<String, String> cache = new SoftMemoryCache<String, String>();
            
            {
                
                assertNull(cache.get("hi"));
                assertFalse(cache.exists("hi"));
                
                final List<String> keys = new ArrayList<String>();
                
                keys.add("hi1");
                keys.add("hi2");
                
                final List<String> results = cache.getAll(keys);
                
                assertEquals(2, results.size());
                
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
            fail("should not get here");
        }

    }
    
}
