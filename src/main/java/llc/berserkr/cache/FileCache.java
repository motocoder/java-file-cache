package llc.berserkr.cache;
import llc.berserkr.cache.exception.ResourceException;
import llc.berserkr.cache.util.FileUtils;
import llc.berserkr.cache.util.StreamUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * File cache. Uses temp files when reading from the cache.
 * 
 * Performs cleanup in the same thread as one used so it could block
 * for several seconds depending on use case.
 * 
 * @author swagner
 *
 */
@Deprecated
public final class FileCache implements Cache<String, InputStream> {

	private final TreeSet<File> sortedFiles = new TreeSet<File>(

			new Comparator<File>() {

				@Override
				public int compare(File item1, File item2) {
					return (int)(item2.lastModified() - item1.lastModified());                
				}

			}

		);

	private final File parent;    
	private final long expiresTimeout;
	private final States states = new States();
	private final long maxSize;

	private long cacheLastModified = 0;

	/**
	 * 
	 * @param parent
	 * @param maxSize -1 for no maximum
	 * @param expiresTimeout -1 for never expiring
	 * 
	 */
	public FileCache(
		final File parent,
		final long maxSize,
		final long expiresTimeout
	) {

		this.parent = parent;
		this.expiresTimeout = expiresTimeout;
		this.maxSize = maxSize;

	}

	private boolean isCacheDirValid(){

		boolean retVal;

		if(parent.exists()) { // .exists throws SecurityException

			if (parent.isDirectory()) { // .isDirectory throws SecurityException
				refreshMetadata();
				retVal = true;
			} else {
				// Something very wrong has occurred, PANIC!
				throw new IllegalArgumentException("<FileCache><1>, Cache location already exists and it is not a directory");
			}
		} else {
		    
		    parent.mkdirs();
		    
			retVal = true;
		}

		return retVal;
	}

	private void refreshMetadata() {
		// This function will check the cache dir's time against the the 
		// time saved during initialization or the most recent put or remove,
		// and if not matching will assume that some external actor has 
		// modified the cache outside this system and will re-initialize
		// the cache metadata.

		if (parent.lastModified() != cacheLastModified) {  // .lastModified throws SecurityException

			states.setCurrentSize(0);
			sortedFiles.clear();

			for(File child : parent.listFiles()) {
				states.setCurrentSize(states.getCurrentSize() + child.length());
				sortedFiles.add(child);  // .add throws ClassCastException and NullPointerException
			}
		}
	}

	private void clean() {

		if(maxSize >= 0) {            

			while(states.getCurrentSize() > maxSize) {

				if(sortedFiles.size() <= 0)  {
					throw new RuntimeException("<FileCache><2>, Cache thinks it is bigger than max size but contains zero files.");
				}

				final File last = sortedFiles.last();

				sortedFiles.remove(last);

				final long length = last.length();

				states.removeFromSize(length);

				FileUtils.delete(last.getAbsolutePath());

			}
		}
	}

	@Override
	public void clear() {

		states.setCurrentSize(0);

		for(File file : sortedFiles) {

            FileUtils.delete(file.getAbsolutePath());

		}

		sortedFiles.clear();

		cacheLastModified = parent.lastModified();

	}

	/**
	 * 
	 * @param cacheRoot
	 * @param key
	 * @return
	 */
	private static final File buildCachedImagePath(final File cacheRoot, final String key) {
		return (new File(cacheRoot, key));  // Constructor will throw NullPointerException
	}

	@Override
	public InputStream get(String key) throws ResourceException {

		if(!isCacheDirValid()) {
			return null;
		}

		final File inCache = buildCachedImagePath(parent, key);

		final InputStream returnVal;

		try {

			final long age = System.currentTimeMillis() - inCache.lastModified(); // lastModified throws SecurityException

			// If the file exists load it then convert it to a <TValue> 
			if(inCache.exists() && (expiresTimeout <= 0 || age < expiresTimeout )) {  // .exists throws SecurityException

				//Reader from file
				try {

					final InputStream in = new FileInputStream(inCache);  // Throws SecurityException and FileNotFoundException

					try {

						inCache.setLastModified(System.currentTimeMillis());  // setLastModified throws IllegalArgumentException and SecurityException

						sortedFiles.add(inCache); // add throws ClassCastException and NullPointerException

						final File tempFile = buildCachedImagePath(parent, key + "TeMPoRaRy_FILE");

						final OutputStream tempOut= new FileOutputStream(tempFile);

						try {
						    
						    StreamUtil.copyTo(in, tempOut);

						    tempOut.flush();
						    
						}
						finally {
						    tempOut.close();
						}

						returnVal = new FileInputStream(tempFile) {

                            @Override
                            public void close() throws IOException {
                                super.close();

                                FileUtils.delete(tempFile.getAbsolutePath());

                            }

                        };

					}
					finally {
						in.close();
					}

				} 
				catch(FileNotFoundException e) {
					throw new RuntimeException("<FileCache><3>, This cannot happen");	                
				}

			}
			else if(inCache.exists() && expiresTimeout > 0 && age > expiresTimeout) {

				// Delete the file if it's expired.
				states.removeFromSize(inCache.length());
				sortedFiles.remove(inCache);

                FileUtils.delete(inCache.getAbsolutePath());

				returnVal = null;

			}
			else {
				returnVal = null;
			}

			return returnVal;

		}
		catch(IOException e) {
			throw new RuntimeException("<FileCache><4>, This shouldn't happen", e);    
		}

	}

	@Override
	public void put(String key, InputStream in) {
		
		// NOTE: The current implementation of put() assumes that the object 
		// being stored is smaller than the cache size.  If you try to 
		// store a large object the cache will be empty.
		
		// NOTE: The current implementation of put() allows the cache to grow
		// above the max size temporarily because it cleans out old files only
		// after the put() is complete.

		if(!isCacheDirValid()) {

			// Put is the only function that truly needs the cache directory
			// if one hasn't been created yet, so it is created here now.

			boolean dirCreatedOk = parent.mkdirs();

			if (!dirCreatedOk){
				// The directory could not be created.  This can occur whenever
				// a phone is connected to a PC as an external storage device.
				return;
			}
		}

		final File inCache = buildCachedImagePath(parent, key);

		try {
		    
			if(inCache.exists()) {

				states.removeFromSize(inCache.length());
				sortedFiles.remove(inCache);

                FileUtils.delete(inCache.getAbsolutePath());


			}

			final FileOutputStream out = new FileOutputStream(inCache, false);

			try {
				StreamUtil.copyTo(in, out);
				out.flush();	            
			}
			finally {
			    
				out.close();
				cacheLastModified = parent.lastModified();
				
			}    

			final File wroteFile = buildCachedImagePath(parent, key);

			states.addSize(wroteFile.length());
			sortedFiles.add(wroteFile);

		}
		catch (IOException e) {
			throw new RuntimeException("<FileCache><5>, This shouldn't happen", e);
		}

		clean();

	}

	@Override
	public boolean exists(String key) {

		if(!isCacheDirValid()) {
			return false;
		}

		return buildCachedImagePath(parent, key).exists();

	}

	@Override
	public void remove(String key) {

		final File file = buildCachedImagePath(parent, key);

		if(file.exists()) {

            FileUtils.delete(file.getAbsolutePath());

			cacheLastModified = parent.lastModified();
			
		}

	}

	@Override
	public List<InputStream> getAll(List<String> keys) throws ResourceException {

		if(!isCacheDirValid()) {

			final List<InputStream> returnVals = new ArrayList<InputStream>();

			for(int ii = 0; ii < keys.size(); ii++) {
				returnVals.add(null);
			}

			return returnVals;

		}

		final List<InputStream> returnVals = new ArrayList<InputStream>();

		for(String key : keys) {
			returnVals.add(get(key));
		}

		return returnVals;

	}
	
	public long size() {
		return states.getCurrentSize();
	}

	private static final class States {

		private long currentSize;

		public States() {

		}

		public void addSize(long length) {
			this.currentSize += length;
		}

		public long getCurrentSize() {
			return currentSize;
		}

		public void setCurrentSize(long currentSize) {
			this.currentSize = currentSize;
		}

		public void removeFromSize(long remove) {
			this.currentSize -= remove;
		}

	}

}
