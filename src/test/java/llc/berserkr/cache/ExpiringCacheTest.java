package llc.berserkr.cache;

import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StopWatch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class ExpiringCacheTest {
    
    @Test 
    public void testExpiringCacheMultiThreaded() {

        final StopWatch watch = new StopWatch();
        watch.start();
        
        final HashSet<String> completedThreads = new HashSet<String>();
        
        final Cache<String, String> cache = 
                new SynchronizedCache<String, String>(
                    new ExpiringCache<String, String>(
                        new MemoryCache<String, String>()
                    ,
                    100,
                    100
                )
            ); 
        
        final int THREAD_COUNT = 50;
            
        for (int x = 0; x < THREAD_COUNT; x++){
            
            final int i = x;
            
            new Thread() {
                
                @Override
                public void run() {
                    
                    try {
                        
                        // just add/remove a bunch of stuff.
                        final Random rand = new Random(); 
                        final int size = 5000;
                        
                        final List<String> keys = new ArrayList<String>();
                        
                        for (int x = 0; x < size; x++) {  
                            
                            int rando2 = rand.nextInt();
                            String key = String.valueOf(rand.nextInt());
                            
                            cache.put(key, String.valueOf(rando2));
                            keys.add(key);
                            
                            cache.get(key); 
                           
                            cache.remove(key);
                            
                        } 
                        
                        //add/get a bunch of stuff
                        final List<String> keys2 = new ArrayList<String>();
                        
                        for (int x=0; x < size; x++) {  
                            
                            keys2.add(String.valueOf(x));
                            cache.put(String.valueOf(x), String.valueOf(x));
                            
                        }
                        
                        cache.getAll(keys2);
                        
                        completedThreads.add(String.valueOf(i)); //we are done, tell stuff it has completed.
                        
                    }
                    catch(ResourceException e) {
                        fail("failed");
                    }
                    
                }
            }.start();
        }
        
        while(completedThreads.size() < THREAD_COUNT && watch.getTime() < 10000) {
            try {
                Thread.sleep(10);
            } 
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        assertEquals(THREAD_COUNT, completedThreads.size());
                
    } 
    
//	@Test
//	public void testExpiringCacheTime() { //CA
//		
//	    Random rand = new Random(); 
//		long max = -1;
//		long min = 10000;
//	
//		int rando; 
//		int rando2;
//		
//		try {
//		    
//		    final StopWatch watch = new StopWatch();
//            watch.start();
//            
//            while(true) {
//			 
//			    final Cache<String, String> cache = 
//		            new SynchronizedCache<String, String>(
//		                new ExpiringCache<String, String>(
//		                    new MemoryCache<String, String>()
//		                , 
//		                100,
//		                100
//		            )
//		        ); 
//			    
//			    final List<String> keys = new ArrayList<String>();
//			    int size = rand.nextInt(1000);
//			    
//                for (int x=0; x < size; x++) {
//			        rando = rand.nextInt();
//			        rando2 = rand.nextInt();
//			        cache.put(String.valueOf(rando), String.valueOf(rando2));
//			        keys.add(String.valueOf(rando));
//			    }
//				
//                List<String> results;
//                
//                while (true) {
//                    
//                    final StopWatch watch2 = new StopWatch();
//                    watch2.start();
//                    
//                    results = cache.getAll(keys);
//                    
//                    Set<String> uniques = new HashSet<String>(results);
//                    
//                    String test = cache.get("");
//                    
//                    if (watch2.getTime() > max){
//    				    max = watch2.getTime();
//    				}
//    				
//    				if (watch2.getTime() < min){
//                        min = watch2.getTime();
//                    }
//    				
//    				uniques.remove(null);
//                    
//                    if (uniques.size() == 0) {
//                        break;
//                    }
//                    
//                    Thread.sleep(1);
//                    
//                }
//                
//				System.out.println("The total time was " + String.valueOf(watch.getTime()) + 
//			        " and the max clear time was " + max + 
//			        " and the min clear time was "+ min);
//			
//			}
//			
//			
//		}
//		
//		catch(Exception e) {
//            fail("should not get here");
//        }
//		
//	}
	
    @Test
    public void testExpiringCacheSimply() {
    
        final Cache<String, String> cache = new ExpiringCache<String, String>(new MemoryCache<String, String>(), 10000, 10000);
        
        try {
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
    
    @Test
    public void testExpirationShortCleanup() {
        
        try {
            
            final Cache<String, String> cache = new ExpiringCache<String, String>(new MemoryCache<String, String>(), 100, 10);
                
            cache.put("hi", "test");
            assertEquals("test", cache.get("hi"));
            
            try {
                Thread.sleep(110);
            } 
            catch (InterruptedException e) {
                fail("Failed");
            }
            
            assertNull(cache.get("hi"));
            
        }
        catch(Exception e) {
            fail("should not get here");
        }
        
    }
    
    @Test
    public void testExpirationLongCleanup() {
        
        try {
            
            final Cache<String, String> cache = new ExpiringCache<String, String>(new MemoryCache<String, String>(), 100, 1000);
                
            cache.put("hi", "test");
            assertEquals("test", cache.get("hi"));
            
            try {
                Thread.sleep(110);
            } 
            catch (InterruptedException e) {
                fail("Failed");
            }
            
            assertNull(cache.get("hi"));
        
        }
        catch(Exception e) {
            fail("should not get here");
        }
            
    }
    
    @Test
    public void testGetAllExpirationLongCleanup() {
        
        try {
            
            final Cache<String, String> cache = new ExpiringCache<String, String>(new MemoryCache<String, String>(), 100, 1000);
                
            final List<Pair<String, String>> entries = new ArrayList<Pair<String, String>>();
            
            entries.add(new Pair<String, String>("hi1", "test1"));
            entries.add(new Pair<String, String>("hi2", "test2"));
            entries.add(new Pair<String, String>("hi3", "test3"));
            
            final List<String> keys = new ArrayList<String>();
            
            for(Pair<String, String> entry : entries) {
                
                keys.add(entry.getOne());
                cache.put(entry.getOne(), entry.getTwo());
                
            }
            
            List<String> values = cache.getAll(keys);
            
            for(int i = 0; i < keys.size(); i++) {
                assertEquals(values.get(i), entries.get(i).getTwo());
            }
            
            try {
                Thread.sleep(160);
            } 
            catch (InterruptedException e) {
                fail("Failed");
            }
            
            values = cache.getAll(keys);
            
            for(int i = 0; i < keys.size(); i++) {
                assertNull(values.get(i));
            }
            
            for(Pair<String, String> entry : entries) {
                cache.put(entry.getOne(), entry.getTwo());
            }
            
            try {
                Thread.sleep(110);
            } 
            catch (InterruptedException e) {
                fail("Failed");
            }
            
            cache.put("hi1", "test1");
            
            values = cache.getAll(keys);
            
            for(int i = 0; i < keys.size(); i++) {
                
                if(keys.get(i).equals("hi1")) {
                    assertEquals(values.get(i), entries.get(i).getTwo());
                }
                else {
                    assertNull(values.get(i));
                }
                
            }
        
        }
        catch(Exception e) {
            fail("should not get here");
        }
        
    }
    
    @Test
    public void testGetAllExpirationShortCleanup() {
        
        try {
        
            final Cache<String, String> cache = new ExpiringCache<String, String>(new MemoryCache<String, String>(), 100, 10);
    
            final List<Pair<String, String>> entries = new ArrayList<Pair<String, String>>();
            
            entries.add(new Pair<String, String>("hi1", "test1"));
            entries.add(new Pair<String, String>("hi2", "test2"));
            entries.add(new Pair<String, String>("hi3", "test3"));
            
            final List<String> keys = new ArrayList<String>();
            
            for(Pair<String, String> entry : entries) {
                
                keys.add(entry.getOne());
                cache.put(entry.getOne(), entry.getTwo());
                
            }
            
            List<String> values = cache.getAll(keys);
            
            for(int i = 0; i < keys.size(); i++) {
                assertEquals(values.get(i), entries.get(i).getTwo());
            }
            
            try {
                Thread.sleep(110);
            } 
            catch (InterruptedException e) {
                fail("Failed");
            }
            
            values = cache.getAll(keys);
            
            for(int i = 0; i < keys.size(); i++) {
                assertNull(values.get(i));
            }
            
            for(Pair<String, String> entry : entries) {
                cache.put(entry.getOne(), entry.getTwo());
            }
            
            try {
                Thread.sleep(110);
            } 
            catch (InterruptedException e) {
                fail("Failed");
            }
            
            cache.put("hi1", "test1");
            
            values = cache.getAll(keys);
            
            for(int i = 0; i < keys.size(); i++) {
                
                if(keys.get(i).equals("hi1")) {
                    assertEquals(values.get(i), entries.get(i).getTwo());
                }
                else {
                    assertNull(values.get(i));
                }
                
            }  
        
        }
        catch(Exception e) {
            fail("should not get here");
        }
        
    }
    
    @Test
    public void testNegInput() {
    
        final Cache<String, String> cache = new ExpiringCache<String, String>(new MemoryCache<String, String>(), 10000, 10000);
            
        {
           
            try {
                
                cache.get(null);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                cache.exists(null);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                cache.getAll(null);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                final List<String> keys = new ArrayList<String>();
                
                keys.add("hi");
                keys.add(null);
                
                cache.getAll(keys);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                cache.put(null, "");
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                cache.put("", null);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }   
            
            try {
                
                cache.remove(null);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
        }
        
    }
    
    @Test
    public void testNegConstruction() {
            
        {
           
            try {
                
                new ExpiringCache<String, String>(null, 10000, 10000);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                new ExpiringCache<String, String>(new MemoryCache<String, String>(), 0, 10000);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
            try {
                
                new ExpiringCache<String, String>(new MemoryCache<String, String>(), 10000, 0);
                fail("Shouldn't have gotten here");
                
            }
            catch(Exception e) {
                //expected behavior
            }
            
        }
        
    }

}
