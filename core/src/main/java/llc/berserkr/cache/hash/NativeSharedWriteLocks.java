package llc.berserkr.cache.hash;

/**
 * Native (C++) backed implementation of {@link CacheLocks.SharedWriteLocks}.
 * The actual locking state lives in native memory.
 */
public class NativeSharedWriteLocks implements CacheLocks.SharedWriteLocks {

    static {
        System.loadLibrary("nativelib");
    }

    final long nativeHandle;

    private NativeSharedWriteLocks(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    /**
     * Per-key locking only — writes on one key do not block reads or writes on other keys.
     * Only concurrent access to the same key is synchronized: a write blocks reads and other
     * writes on that key, but concurrent reads of the same key are allowed. This is the
     * optimized default used by FileHash and StreamingFileHash.
     */
    public static NativeSharedWriteLocks createIgnored() {
        return new NativeSharedWriteLocks(nativeCreateIgnored());
    }

    /**
     * Global write lock — when any key is being written, all reads on all keys are blocked
     * until the write completes. This was used for an older cache design before per-bucket
     * locking was optimized. Retained for cases that require strict global consistency.
     */
    public static NativeSharedWriteLocks createStandard() {
        return new NativeSharedWriteLocks(nativeCreateStandard());
    }

    @Override
    public int getLock(CacheLocks.LockType lockType) {
        return nativeGetLock(nativeHandle, lockType == CacheLocks.LockType.WRITER);
    }

    @Override
    public void releaseLock() {
        nativeReleaseLock(nativeHandle);
    }

    @Override
    public int peekLock() {
        return nativePeekLock(nativeHandle);
    }

    public void destroy() {
        nativeDestroy(nativeHandle);
    }

    private static native long nativeCreateIgnored();
    private static native long nativeCreateStandard();
    private static native void nativeDestroy(long handle);
    private static native int nativeGetLock(long handle, boolean isWriter);
    private static native void nativeReleaseLock(long handle);
    private static native int nativePeekLock(long handle);
}
