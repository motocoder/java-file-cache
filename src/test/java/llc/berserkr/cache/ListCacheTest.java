package llc.berserkr.cache;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ListCacheTest {

    @Test
    public void testListCacheNormal() {

        try {
       
            final Cache<String, String> cache1 = new MemoryCache<String, String>();
            final Cache<String, String> cache2 = new MemoryCache<String, String>();
            final Cache<String, String> cache3 = new MemoryCache<String, String>();
            
            final List<Cache<String, String>> list = new ArrayList<Cache<String, String>>();
            
            list.add(cache1);
            list.add(cache2);
            list.add(cache3);
            
            final Cache<String, String> cache = new ListCache<String, String>(list, true);
            
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
    public void testListCachePullThrough() {

        try {
       
            final Cache<String, String> cache1 = new MemoryCache<String, String>();
            final Cache<String, String> cache2 = new MemoryCache<String, String>();
            final Cache<String, String> cache3 = new MemoryCache<String, String>();
            
            final List<Cache<String, String>> list = new ArrayList<Cache<String, String>>();
            
            list.add(cache1);
            list.add(cache2);
            list.add(cache3);
            
            final Cache<String, String> cache = new ListCache<String, String>(list, true);
            
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
                
                cache3.put("hi", "test");
                
                assertTrue(cache.exists("hi"));
                assertEquals("test", cache.get("hi"));
                assertTrue(cache2.exists("hi"));
                assertEquals("test", cache2.get("hi"));
                assertTrue(cache1.exists("hi"));
                assertEquals("test", cache1.get("hi"));
                
                cache2.put("hi1", "test1");
                cache1.put("hi2", "test2");
                
                final List<String> results2 = cache.getAll(keys);
                
                assertEquals(results2.get(0), "test1");
                assertEquals(results2.get(1), "test2");
                
                assertNull(cache3.get("hi1"));
                assertNull(cache3.get("hi2"));
                assertNull(cache2.get("hi2"));
                
                assertEquals("test1", cache1.get("hi1"));
                assertEquals("test2", cache1.get("hi2"));
                
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
                
                cache.clear();
                
            }
            
            {
                cache3.put("3", "3");
                cache2.put("2", "2");
                cache1.put("1", "1");
                
                cache.get("1");
                cache.get("2");
                cache.get("3");
                
                assertEquals("3", cache1.get("3"));
                assertEquals("2", cache1.get("2"));
                assertEquals("1", cache1.get("1"));
                
                assertEquals("3", cache2.get("3"));
                assertEquals("2", cache2.get("2"));
                assertEquals(null, cache2.get("1"));
                
                assertEquals("3", cache3.get("3"));
                assertEquals(null, cache3.get("2"));
                assertEquals(null, cache3.get("1"));
                
            }
            
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("should not get here");
        }
        
    }
    
    @Test
    public void testListCacheNotPullThrough() {

        try {
       
            final Cache<String, String> cache1 = new MemoryCache<String, String>();
            final Cache<String, String> cache2 = new MemoryCache<String, String>();
            final Cache<String, String> cache3 = new MemoryCache<String, String>();
            
            final List<Cache<String, String>> list = new ArrayList<Cache<String, String>>();
            
            list.add(cache1);
            list.add(cache2);
            list.add(cache3);
            
            final Cache<String, String> cache = new ListCache<String, String>(list, false);
            
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
                
                cache3.put("hi", "test");
                
                assertTrue(cache.exists("hi"));
                assertEquals("test", cache.get("hi"));
                assertFalse(cache2.exists("hi"));
                assertEquals(null, cache2.get("hi"));
                assertFalse(cache1.exists("hi"));
                assertEquals(null, cache1.get("hi"));
                
                cache2.put("hi1", "test1");
                cache1.put("hi2", "test2");
                
                final List<String> results2 = cache.getAll(keys);
                
                assertEquals(results2.get(0), "test1");
                assertEquals(results2.get(1), "test2");
                
                assertNull(cache3.get("hi1"));
                assertNull(cache3.get("hi2"));
                assertNull(cache2.get("hi2"));
                
                assertEquals(null, cache1.get("hi1"));
                assertEquals("test2", cache1.get("hi2"));
                
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
                
                cache.clear();
                
            }
            
            {
                cache3.put("3", "3");
                cache2.put("2", "2");
                cache1.put("1", "1");
                
                cache.get("1");
                cache.get("2");
                cache.get("3");
                
                assertEquals(null, cache1.get("3"));
                assertEquals(null, cache1.get("2"));
                assertEquals("1", cache1.get("1"));
                
                assertEquals(null, cache2.get("3"));
                assertEquals("2", cache2.get("2"));
                assertEquals(null, cache2.get("1"));
                
                assertEquals("3", cache3.get("3"));
                assertEquals(null, cache3.get("2"));
                assertEquals(null, cache3.get("1"));
                
            }
            
        }
        catch(Exception e) {
            e.printStackTrace();
            fail("should not get here");
        }
        
    }
}
