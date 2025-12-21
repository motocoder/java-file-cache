package llc.berserkr.cache;

import llc.berserkr.cache.converter.*;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.StreamUtil;
import llc.berserkr.cache.util.StringUtilities;
import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class FileHashCacheTest {

    private static final Logger logger = LoggerFactory.getLogger(FileHashCacheTest.class);
    
    static {
    	BasicConfigurator.configure();
    }
	
	@Test 
	public void universalTest() {

        final File tempFolder = new File("./test-files/temp-dataa");
        final File dataFolder = new File("./test-files/dataa");
        final File dataFolderItem = new File("./test-files/data/dataa");
        
		try {
			
	        deleteRoot(tempFolder);
	        deleteRoot(dataFolder);
	        dataFolderItem.delete();
	        
	        tempFolder.mkdirs();
	        dataFolder.mkdirs();

            final Cache<byte [], InputStream> fileCache = new FileStreamHashCache(dataFolderItem);

            final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                    new KeyConvertingCache<String, byte[], InputStream>(fileCache, new ReverseConverter<>(new BytesStringConverter()));

            final Cache<String, InputStream> cache = new SynchronizedCache<String, InputStream>(keyConvertingCache);


            final String key = "dtsffffdfsjhgjnvdfsddfssdffsdfewfeasdf";
	        final String value = "ddwerfsadfwefwaefwfawfewsadfsad4";
	        final String key2 = "dtsffffdfsjhgjnvdfsddfsasdeasdf";
	        final String value2 = "saasdfasdfdfasdfaewasfsadfasfdadscasdfasdfasdfsdfg";
	        final String key3 = "adatiwefwwawfwfdfdsgdfgsdgasdd982y4d8913y894d012ny4dyn328yd4n289314yndm9812ynd409jkjlafoasudf8904uy04uf4309u0oidsnhfioashd9834jh0q4f08943qhlq34hf89034fh48fih3fsds";
	        String returnValue;
	        
	        // TEST PUT, GET, REMOVE, and EXISTS
	        
	        cache.put(key, new ByteArrayInputStream(value.getBytes()));
	        
	        byte [] baos = SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key));
			
	        returnValue = new String(baos);
	        
	        assertEquals(value, returnValue);
	        assertEquals(cache.exists(key), true);
	        
	        cache.remove(key);
	        
	        assertEquals(cache.exists(key), false);

	        
	        // TEST CLEAR, GETALL, and RETEST EXISTS
	        
	        List<String> keyList = new ArrayList<String>();
	        
	        keyList.add(key3);
	        keyList.add(key2);
	        
	        cache.put(key3, new ByteArrayInputStream(value.getBytes()));
	        cache.put(key2, new ByteArrayInputStream(value2.getBytes()));

	        Thread.sleep(500);
	        
	        assertEquals(true, cache.exists(key3));

            final InputStream read = cache.get(key2);
	        assertNotNull(read);
            read.close();
	        assertEquals(true, cache.exists(key2));

	        cache.clear();
		       
	        assertEquals(cache.exists(key3), false);
	        assertEquals(cache.exists(key2), false);
	        
		}
		catch (ResourceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			deleteRoot(tempFolder);
	        deleteRoot(dataFolder);
	        dataFolderItem.delete();
		}
		
	}
	
	// convert InputStream to String
	private static String getStringFromInputStream(InputStream is) {
	    
	    try {
	        
    	    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    	    
    	    StreamUtil.copyTo(is, out);
    	    
     
    		return new String(out.toByteArray(), "UTF-8");
    		
	    }
	    catch(Exception e) {
	        
	        fail();
	        throw new RuntimeException();
	        
	    }
 
	}
	
	void deleteRoot (File root) {
        if (root.exists()) {
            
        	final File[] listed = root.listFiles();
        	
        	if(listed != null) {	        	
	            for (File cacheFile: listed){
	                cacheFile.delete();
	            }
        	}
            
            root.delete();
        }
    }
	
	@Test 
	public void secondaryTest() {
		
		try {
			
	        final File tempFolder = new File("./target/test-files/temp-data");
	        final File dataFolder = new File("./target/test-files/data");
	        
	        deleteRoot(tempFolder);
	        deleteRoot(dataFolder);
	        
	        tempFolder.mkdirs();
	        dataFolder.mkdirs();

            final Cache<byte [], InputStream> fileCache = new FileStreamHashCache(dataFolder);

            final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                    new KeyConvertingCache<String, byte[], InputStream>(fileCache, new ReverseConverter<>(new BytesStringConverter()));

            final Cache<String, InputStream> cache = new SynchronizedCache<String, InputStream>(keyConvertingCache);


            // create very long string by concatenation
	        String key = "abcdefghijklmnopqrstuvwxyz0123456789";
	        String value = "abcdefghijklmnopqrstuvwxyz0123456789";
	        int iterations = 3;
	        
	        
	        for (int i = 0; i < iterations; i++) {
	        	key += key.concat(key);
	        	value += value.concat(value);
	        }
	        
	        logger.info("value is this long: " + value.length());
	        
	        String returnValue;
	        
	        // TEST PUT, GET, REMOVE, and EXISTS
	        
	        for (int i = 0; i < 10; i++) {
	        	cache.put(key, new ByteArrayInputStream(value.getBytes()));
	        }
	        
	        byte [] baos = null;
	        
	        for (int i = 0; i < 10; i++) {
	        	baos = SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key));
	        }
			
	        returnValue = new String(baos);
	        
	        assertEquals(value, returnValue);
	        for (int i = 0; i < 10; i++) {
	        	assertEquals(cache.exists(key), true);
	        }
	        
	        for (int i = 0; i < 10; i++) {
	        	cache.clear(); //Test Clear
	        }
	        
	        assertEquals(cache.exists(key), false);

	        
		}
		catch (ResourceException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test 
	public void multiThreadedTest() throws IOException {
		
		final ExecutorService pool = Executors.newFixedThreadPool(100);

        final File tempFolder = new File("./target/test-files/temp-data");
        final File dataFolder = new File("./target/test-files/data");
        
        deleteRoot(tempFolder);
        deleteRoot(dataFolder);
        
        tempFolder.mkdirs();
        dataFolder.mkdirs();
        
        final Cache<byte [], InputStream> fileCache = new FileStreamHashCache(dataFolder);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                new KeyConvertingCache<String, byte[], InputStream>(fileCache, new ReverseConverter<>(new BytesStringConverter()));

//		final Cache<String, InputStream> cache = new SynchronizedCache<String, InputStream>(keyConvertingCache);
        final Cache<String, InputStream> cache = keyConvertingCache;
		
		for (int x = 0; x < 99; x++) {

            final int pre = x;
			pool.execute(
                    
                new Runnable() {

                    @Override
                    public void run() {
					
						try {
					
					        // create varying length strings by concatenation
							final String value = pre + "adasdfasdfasfdasfasdfdfsdf";
							final Random random = new Random();
					        
							final String key = "e" + String.valueOf(random.nextInt(999999));
					        
					        logger.info("putting value: " + value + " with the key of " + key);
					        
					        // TEST PUT, GET, REMOVE, and EXISTS

                            for(int i = 0; i < 10; i++) {
                                cache.put(key, new ByteArrayInputStream(value.getBytes()));

                                for(int j = 0; j < 100; j++) {
                                    final String returnValue = new String(SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key)));

                                    assertEquals(value, returnValue);
                                    assertEquals(cache.exists(key), true);
                                }
                            }
					        
						} catch (ResourceException e) {
							e.printStackTrace();
						} catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    
                }
                
			);
			
		}
		
		pool.shutdown();

		try {
			pool.awaitTermination(1000, TimeUnit.SECONDS);
		}
		catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
	}
	
	@Test
	public void extraTests() throws IOException {
		
		try {
	        
			File root2 = new File("./target/test-files/temp/");
			
			deleteRoot(root2);
			
			final Converter<Integer, String> converter = new ReverseConverter<Integer, String>(new StringSizeConverter());
			
			final File dataFolder = new File(root2, "data");
	        final File persistingFolder = new File(root2, "persisting");

            final Cache<byte [], InputStream> fileCacheStream = new FileStreamHashCache(dataFolder);

            final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                    new KeyConvertingCache<String, byte[], InputStream>(fileCacheStream, new ReverseConverter<>(new BytesStringConverter()));

            Cache<String, String> fileCache =
				new ValueConvertingCache<String, String, InputStream>(
                        keyConvertingCache,
						new ReverseConverter<>(new InputStreamStringConverter())
					);
				
			Cache<String, String> cache =
	            new FilePersistedMaxSizeCache<String>(
	                persistingFolder,
	                fileCache,
	                converter,
	                150
	            );
	        
	        final String key = "dtsffffdfsjhgjnvdfsddfssdffsdfewfeasdf";
	        final String value = "ddwerfsadfwefwaefwfawfewsadfsad4";
	        final String key2 = "dtsffffdfsjhgjnvdfsddfsasdeasdf";
	        final String key3 = "adatiwefwwawfwfdfdsgdfgsdgasdd982y4d8913y894d012ny4dyn328yd4n289314yndm9812ynd409jkjlafoasudf8904uy04uf4309u0oidsnhfioashd9834jh0q4f08943qhlq34hf89034fh48fih3fsds";
	        String returnValue;
	        
	        // TEST PUT, GET, REMOVE, and EXISTS
	        
	        cache.put(key, value);
	        
	        returnValue = cache.get(key);
	        
	        assertEquals(value, returnValue);
	        assertEquals(cache.exists(key), true);
	        
	        cache.clear();
	        
	        assertEquals(cache.exists(key), false);
	        
	        cache.clear();
		       
	        assertEquals(cache.exists(key), false);
	        
		}
		catch (ResourceException e) {
			e.printStackTrace();
		}
		
	}
	
	@Test 
	public void varyingKeyLengthTest() throws IOException {
		
		try {
			
			for (int x = 0; x < 18; x++) {
				
		        final File tempFolder = new File("./target/test-files/temp-data");
		        final File dataFolder = new File("./target/test-files/data");
		        
		        deleteRoot(tempFolder);
		        deleteRoot(dataFolder);

                final Cache<byte [], InputStream> fileCacheStream = new FileStreamHashCache(dataFolder);

                final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                        new KeyConvertingCache<String, byte[], InputStream>(fileCacheStream, new ReverseConverter<>(new BytesStringConverter()));

				final Cache<String, InputStream> cache = new SynchronizedCache<>(keyConvertingCache);
				
		        // create varying length strings by concatenation
		        String key = "abcdef0123456789";
		        String value = "abcdefghi";
		        
		        for (int i = 0; i < x; i++) {
		        	value = value.concat(value);
		        	key = key.concat("f");
		        	
		        	
		        }
		        
		        logger.info("putting value of length: " + value.length() + " with key of length: " + key.length());
		        
		        // TEST PUT, GET, REMOVE, and EXISTS
		        
		        logger.debug("writting value " + value.getBytes().length);
		        
		        cache.put(key, new ByteArrayInputStream(value.getBytes()));
		        
		        String returnValue = new String(SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key)));
			
		        logger.debug(returnValue);
		        logger.debug(value);
		        
		        assertEquals(value.length(), returnValue.length());
		        assertEquals(value, returnValue);
		        assertEquals(cache.exists(key), true);
		        
		        cache.clear(); // test clear
		        
		        assertEquals(cache.exists(key), false);
		        
			}
		        
		}
		catch (ResourceException e) {
			e.printStackTrace();
		} 
		finally {
			
		}
		
	}
	
	@Test 
	public void assortedTest() throws IOException {

		final File root2 = new File("/target/test-files/tempor-dataa");
		
		try {
			
			for (int x = 0; x < 18; x++) {
				
		        final File tempFolder = new File("./target/test-files/temp-data");
		        final File dataFolder = new File("./target/test-files/data");
		        
		        deleteRoot(tempFolder);
		        deleteRoot(dataFolder);

                final Cache<byte [], InputStream> fileCacheStream = new FileStreamHashCache(dataFolder);

                final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                        new KeyConvertingCache<String, byte[], InputStream>(fileCacheStream, new ReverseConverter<>(new BytesStringConverter()));

				final Cache<String, InputStream> cache = new SynchronizedCache<>(keyConvertingCache);
				
		        // create varying length strings by concatenation
		        String key = "abcdef0123456789";
		        String value = "abcdefghi";
		        
		        for (int i = 0; i < x; i++) {
		        	value = value.concat(value);
		        	key = key.concat("f");
		        }
		        
		        logger.info("putting value of length: " + value.length() + " with key of length: " + key.length());
		        
		        // TEST PUT, GET, REMOVE, and EXISTS
		        
		        cache.put(key, new ByteArrayInputStream(value.getBytes()));
		        
		        String returnValue = new String(SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key)));
			
		        System.out.println(returnValue);
		        System.out.println(value);
		        
		        assertEquals(value ,returnValue);
		        assertEquals(cache.exists(key), true);
		        
		        cache.clear(); // test clear
		        
		        assertEquals(cache.exists(key), false);
		        
			}
		        
		}
		catch (ResourceException e) {
			e.printStackTrace();
		} 
		finally {
			
		}
		
	}
	
	@Test 
    public void repeatingKeyTest() throws IOException {
        
        try {
                            
            final File tempFolder = new File("./target/test-files/temp-data-repeatingKeyTest");
            final File dataFolder = new File("./target/test-files/data-repeatingKeyTest");
            final File dataFolderItem = new File("./target/test-files/data-repeatingKeyTest/data");
            
            deleteRoot(tempFolder);
            deleteRoot(dataFolder);
            dataFolderItem.delete();
           
            assertFalse(dataFolderItem.exists());
            
            tempFolder.mkdirs();
            dataFolder.mkdirs();

            final Cache<byte [], InputStream> fileCacheStream = new FileStreamHashCache(dataFolder);

            final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                    new KeyConvertingCache<String, byte[], InputStream>(fileCacheStream, new ReverseConverter<>(new BytesStringConverter()));

            Cache<String, String> fileCache =
                new ValueConvertingCache<String, String, InputStream>(
                    keyConvertingCache,
                    new ReverseConverter<>(new InputStreamStringConverter())
                );

            final Cache<String, InputStream> cache = new SynchronizedCache<>(keyConvertingCache);
            
            // create varying length strings by concatenation
            final String keyOrig = "abcdef0123456789";
            final String valueOrig = "abcdefghiasdsadsaa";
                        
            for(int i = 0; i < 500; i++) {
                
                final int random = (int) (Math.random() * 100);
                
                if(i % 100 == 0) {
                    
                    logger.debug("Size of data " + dataFolderItem.length());
                    
                    for(int j = 0; j < 100; j++) {
                        
                        final String key = StringUtilities.repeat(keyOrig, j);
                        
                        cache.remove(key);
                        
                        assertEquals(cache.exists(key), false);
                        assertNull(cache.get(key));
                        
                    }
                    
                }
                
                // TEST PUT, GET, REMOVE, and EXISTS
                
                final String value = StringUtilities.repeat(valueOrig, random);
                final String key = StringUtilities.repeat(keyOrig, random);
                
                cache.put(key, new ByteArrayInputStream(value.getBytes()));
                
                String returnValue = new String(SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key)));
                
                assertEquals(value, returnValue);
                assertEquals(cache.exists(key), true);
                
            }
                   
        }
        catch (ResourceException e) {
            
            logger.error("ERROR:", e);
            fail();
        } 
        
    }
	
	@Test 
    public void fileSizeTest() throws IOException {
        
        try {
                            
            final File tempFolder = new File("./target/test-files/temp-data-fileSizeTest");
            final File dataFolder = new File("./target/test-files/data-fileSizeTest");
            final File dataFolderItem = new File("./target/test-files/data-fileSizeTest/data");

            final Cache<byte [], InputStream> fileCacheStream = new FileStreamHashCache(dataFolder);

            final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                    new KeyConvertingCache<String, byte[], InputStream>(fileCacheStream, new ReverseConverter<>(new BytesStringConverter()));

            final Cache<String, InputStream> cache = new SynchronizedCache<>(keyConvertingCache);
            
            // create varying length strings by concatenation
            final String keyOrig = "abcdef0123456789";
            final String valueOrig = "abcdefghiasdsadsaa";
                        
            for(int i = 0; i < 500; i++) {
                
                final int random = (int) (Math.random() * 100);
                
                if(i % 100 == 0) {
                    
                    logger.debug("Size of data " + dataFolderItem.length());
                    
                    for(int j = 0; j < 100; j++) {
                        
                        final String key = StringUtilities.repeat(keyOrig, j);
                        
                        cache.remove(key);
                        
                        assertEquals(cache.exists(key), false);
                        assertNull(cache.get(key));
                        
                    }
                    
                }
                
                // TEST PUT, GET, REMOVE, and EXISTS
                
                final String value = StringUtilities.repeat(valueOrig, random);
                final String key = StringUtilities.repeat(keyOrig, random);
                
                cache.put(key, new ByteArrayInputStream(value.getBytes()));
                
                String returnValue = new String(SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key)));
                
                assertEquals(value, returnValue);
                assertEquals(cache.exists(key), true);
                
            }
                   
        }
        catch (ResourceException e) {
            
            logger.error("ERROR:", e);
            fail();
        } 
        
    }
	
	@Test 
    public void repeatingKey2Test() throws IOException {
        
        try {
                            
            final File tempFolder = new File("./target/test-files/temp-data-repeatingKeyTest");
            final File dataFolder = new File("./target/test-files/data-repeatingKeyTest");
            final File dataFolderItem = new File("./target/test-files/data-repeatingKeyTest/data");
            
            deleteRoot(tempFolder);
            deleteRoot(dataFolder);
            dataFolderItem.delete();
           
            assertFalse(dataFolderItem.exists());
            
            tempFolder.mkdirs();
            dataFolder.mkdirs();

            final Cache<byte [], InputStream> fileCacheStream = new FileStreamHashCache(dataFolder);

            final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
                    new KeyConvertingCache<String, byte[], InputStream>(fileCacheStream, new ReverseConverter<>(new BytesStringConverter()));

            final Cache<String, InputStream> cache = new SynchronizedCache<>(keyConvertingCache);
            
            // create varying length strings by concatenation
            final String keyOrig = "abcdef0123456789";
            final String valueOrig = "abcdefghiasdsadsaa";
                        
            for(int i = 0; i < 500; i++) {
                             
                if(i % 100 == 0) {
                    logger.debug("Size of data " + dataFolderItem.length());
                }
                
                {
                    
                    final int random = (int) (Math.random() * 100);
                    // TEST PUT, GET, REMOVE, and EXISTS
                    
                    final String value = StringUtilities.repeat(valueOrig, random);
                    final String key = StringUtilities.repeat(keyOrig, random);
                    
                    cache.put(key, new ByteArrayInputStream(value.getBytes()));
                    
                    String returnValue = new String(SegmentedStreamingHashDataManager.convertInputStreamToBytes(cache.get(key)));
                    
                    assertEquals(value, returnValue);
                    assertEquals(cache.exists(key), true);
                    
                }
                
                {
                    
                    final int random = (int) (Math.random() * 100);
                    
                    final String key = StringUtilities.repeat(keyOrig, random);
                    
                    cache.remove(key);
                    
                    assertEquals(cache.exists(key), false);
                    assertNull(cache.get(key));
                    
                }

                
            }
                   
        }
        catch (ResourceException e) {
            
            logger.error("ERROR:", e);
            fail();
        } 
        
    }
	
}
