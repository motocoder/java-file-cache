package llc.berserkr.cache.hash;

/**
 * Reading and writing operations can happen simultaneous as long as they don't conflict at several points.
 *
 * If a segment is writing to an existing segment it can't be read at the same time.
 *
 * A segment also can't be written to by 2 threads at the same time.
 *
 * Searching for the end item and or writing to the end item can't happen by 2 threads at the same time.
 *
 * Merging/splitting can only happen per master address by one thread at the same time.
 *
 * The hash buckets are stored in their own segmented file that has these same rules but the overall policy used for
 * them will just be a global lock per index since the read/write operations of the hash blob are fairly isolated
 * and fast.
 *
 * This interface handles just the locks for the hash index blobs.
 */
public interface CacheLocks {

    enum LockType {READER, WRITER}

    void getLock(LockType lockType) throws InterruptedException;

    void releaseLock(LockType lockType);

    /**
     * Coordinates write visibility across multiple CacheLocks instances that share the same
     * SharedWriteLocks. Each CacheLocks instance guards a single key (hash bucket), but
     * SharedWriteLocks controls whether writes on one key block reads on all other keys.
     */
    interface SharedWriteLocks {
        int getLock(LockType lockType);
        void releaseLock();
        int peekLock();
    }

    /**
     * Global write lock — when any key is being written, all reads on all keys are blocked
     * until the write completes. This was used for an older cache design before per-bucket
     * locking was optimized. Retained for cases that require strict global consistency.
     */
    class StandardSharedWriteLocks implements SharedWriteLocks {

        private volatile int writeLocks = 0;

        @Override
        public synchronized int getLock(LockType lockType) {
            if (lockType == LockType.WRITER) {
                return writeLocks++;
            } else {
                return writeLocks;
            }
        }

        @Override
        public synchronized void releaseLock() {
            writeLocks--;
        }

        @Override
        public synchronized int peekLock() {
            return writeLocks;
        }
    }

    /**
     * Per-key locking only — writes on one key do not block reads or writes on other keys.
     * Only concurrent access to the same key is synchronized: a write blocks reads and other
     * writes on that key, but concurrent reads of the same key are allowed. This is the
     * optimized default used by FileHash and StreamingFileHash.
     */
    class IgnoredWriteLocks implements SharedWriteLocks {

        @Override
        public int getLock(LockType lockType) {
            return 0;
        }

        @Override
        public void releaseLock() {
        }

        @Override
        public int peekLock() {
            return 0;
        }
    }
}
