package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AlwaysNullCacheTest {
	
	@Test
    public void test() {
		
    	try {
    		
    		final Cache<String, String> cache = new AlwaysNullCache<String, String>();
            
    		cache.put("key", "test");
            
    		assertNull(cache.get("key"));
            cache.remove("key");
            assertEquals(cache.exists("key"), true);
            
            final List<String> keys = new ArrayList<String>();
	        
	        keys.add("test1");
	        keys.add("test2");
	        
	        final List<String> results = cache.getAll(keys);
	        
	        assertNull(results.get(0));
	        assertNull(results.get(1));
	        
	        cache.clear();
	        
	        assertNull(cache.get("test1"));
	        assertNull(cache.get("test2"));
	        
	        {
    	
    		final Cache<String, String> cache2 = new AlwaysNullCache<String, String>(true);
            
            cache2.put("test", "hello");
            
            assertNull(cache2.get("test"));
            cache2.remove("test");
            assertEquals(cache2.exists("test"), true);
            cache2.clear();
            
            assertNull(cache2.get("test"));
            
	        }
	        
    	}
    	
    	catch(ResourceException e) {
            fail("failed");
        }
    	
	}
	
}
