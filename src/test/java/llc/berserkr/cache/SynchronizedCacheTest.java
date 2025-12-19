package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.ParallelControl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class SynchronizedCacheTest {
	
	@Test
    public void test() { 
		
		final Cache<String, String> cache = 
				new SynchronizedCache<String, String>(
					new Cache<String, String>() {

						@Override
						public String get(String key) throws ResourceException {
			                return key;
						}

						@Override
						public boolean exists(String key) throws ResourceException {
							return false;
						}

						@Override
						public List<String> getAll(List<String> keys) throws ResourceException {
							return null;
						}

						@Override
						public void clear() {
						}

						@Override
						public void remove(String key) {
						}

						@Override
						public void put(String key, String value) {
						}
						
					}
					
				);
		
    	try {
    		
			assertEquals(cache.get("key"), "key");
			assertEquals(false, cache.exists("key"));
            
			cache.put("key", "test");
            
            assertEquals(cache.get("key"), "key");
            assertEquals(false, cache.exists("key"));
            
            cache.remove("key");
            
            assertEquals(cache.get("key"), "key");
            assertEquals(false, cache.exists("key"));
            
            cache.clear();
            
            assertEquals(cache.get("key"), "key");
            assertEquals(false, cache.exists("key"));
            
            {
            	
	        	final List<String> keys = new ArrayList<String>();
	            
	            keys.add("test1");
	            keys.add("test2");
	            
	            cache.getAll(keys);
	            
	            assertEquals(cache.get("key"), "key");
	            assertEquals(false, cache.exists("key"));
            
            }
            
    	}
    	
        catch(ResourceException e) {
            fail("failed");
        }
        
    }
	
	@Test
	public void test2() throws InterruptedException {
		
		final ParallelControl<String> control1 = new ParallelControl<String>();
		final ParallelControl<String> control2 = new ParallelControl<String>();
			
		final Cache<String, String> cache = 
				new SynchronizedCache<String, String>( 
					new Cache<String, String>() {

						@Override
						public String get(String key) throws ResourceException {
			                
						    try {
			                	
			                	control1.unBlockOnce();
								control2.blockOnce();
								
							} 
						    catch (InterruptedException e) {
								e.printStackTrace();
							}
			                
							return key;
						
						}

						@Override
						public boolean exists(String key) throws ResourceException {
							return false;
						}

						@Override
						public List<String> getAll(List<String> keys) throws ResourceException {
							return null;
						}

						@Override
						public void clear() {
						}

						@Override
						public void remove(String key) {
						}

						@Override
						public void put(String key, String value) {
						}
						
					} 
					
				);
		
		
		final Runnable thread2 = new Runnable() {
            
            @Override
            public void run() {
                
                try {
                    
                    control1.unBlockOnce();
                    assertEquals(cache.get("key"), "key");
                    assertEquals(false, cache.exists("key"));
                
                    cache.put("key", "test");
                    
                    control1.unBlockOnce();
                    
                }
                catch(ResourceException e) {
                    fail("failed");
                }
                
            }
            
        };
        
		Runnable thread1 = new Runnable() {
			
		    @Override
            public void run() {
		        
		        new Thread(thread2).start();
				
		        try {
					
					cache.put("key", "test");
					
					assertEquals(cache.get("key"), "key");
					control2.setValue("");
					assertEquals(false, cache.exists("key"));
					
					cache.put("key", "test");
					
					control2.unBlockOnce();
					
				}
				catch(ResourceException e) {
		            fail("failed");
		        }
				
			}
			
		};
		
		
		
		new Thread(thread1).start();
		
		
		assertNull(control2.getValue());
        
        control2.unBlockOnce();
        
        
	}
	
}
