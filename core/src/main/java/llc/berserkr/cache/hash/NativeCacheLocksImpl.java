package llc.berserkr.cache.hash;

/**
 * Native (C++) backed implementation of {@link CacheLocks}.
 * All locking logic runs in native code using std::mutex and std::condition_variable.
 */
public class NativeCacheLocksImpl implements CacheLocks {

    static {
        System.loadLibrary("nativelib");
    }

    private final long nativeHandle;

    private NativeCacheLocksImpl(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    /**
     * Creates a native CacheLocks instance backed by the given shared write locks.
     * The shared write locks must be a {@link NativeSharedWriteLocks} instance.
     */
    static CacheLocks create(NativeSharedWriteLocks sharedWriteLocks) {
        return new NativeCacheLocksImpl(nativeCreate(sharedWriteLocks.nativeHandle));
    }

    @Override
    public void getLock(LockType lockType) throws InterruptedException {
        nativeGetLock(nativeHandle, lockType == LockType.WRITER);
    }

    @Override
    public void releaseLock(LockType lockType) {
        nativeReleaseLock(nativeHandle, lockType == LockType.WRITER);
    }

    public void destroy() {
        nativeDestroy(nativeHandle);
    }

    private static native long nativeCreate(long sharedHandle);
    private static native void nativeDestroy(long handle);
    private static native void nativeGetLock(long handle, boolean isWriter);
    private static native void nativeReleaseLock(long handle, boolean isWriter);
}
