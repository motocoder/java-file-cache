package llc.berserkr.cache.hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link CacheLocks} instances. Attempts to use the native (C++) implementation
 * when available, falling back to the Java implementation otherwise.
 */
public class CacheLocksFactory {

    private static final Logger logger = LoggerFactory.getLogger(CacheLocksFactory.class);

    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("nativelib");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            logger.debug("Native library not available, using Java CacheLocks implementation");
        }
        NATIVE_AVAILABLE = loaded;
    }

    private CacheLocksFactory() {}

    /**
     * Creates a CacheLocks instance. Uses the native (C++) implementation when the native library
     * is available and ignoreNative is false, otherwise falls back to the Java implementation.
     *
     * @param ignoreNative if true, always use the Java implementation. If false, use the native
     *                     implementation when the native library is available, falling back to Java.
     */
    public static CacheLocks createWithIgnoredWriteLocks(boolean ignoreNative) {
        if (!ignoreNative && NATIVE_AVAILABLE) {
            return NativeCacheLocksImpl.create(NativeSharedWriteLocks.createIgnored());
        }
        return CacheLocksImpl.create(new CacheLocks.IgnoredWriteLocks());
    }

    public static CacheLocks createJavaWithIgnoredWriteLocks() {
        return CacheLocksImpl.create(new CacheLocks.IgnoredWriteLocks());
    }

    public static CacheLocks createNativeWithIgnoredWriteLocks() {
        return NativeCacheLocksImpl.create(NativeSharedWriteLocks.createIgnored());
    }
}
