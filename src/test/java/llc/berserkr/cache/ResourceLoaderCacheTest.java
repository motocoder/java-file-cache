package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.loader.DefaultResourceLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class ResourceLoaderCacheTest {
	
    @Test
    public void test() {
    	
    	try{
    		
			final Cache<String, String> cache = 
				new ResourceLoaderCache<String, String>(
					new DefaultResourceLoader<String, String>() {
	
						@Override
						public String get(String key) throws ResourceException {
			                return key;
						}
					}
				);
	        
            cache.put("key", "test");
            assertEquals(cache.get("key"), "key");
            cache.remove("key");
            assertEquals(cache.exists("key"), true);
            cache.clear();
            
            {
            	
        	final List<String> keys = new ArrayList<String>();
            
            keys.add("test1");
            keys.add("test2");
            
            final List<String> results = cache.getAll(keys);
            	
            assertEquals(results.get(0), cache.get("test1"));
            assertEquals(results.get(1), cache.get("test2"));
            
            }
            
    	}
    	
        catch(ResourceException e) {
            fail("failed");
        }
        
    }
    
}
