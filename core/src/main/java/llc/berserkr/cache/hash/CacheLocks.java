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

    interface SharedWriteLocks {
        int getLock(LockType lockType);
        void releaseLock();
        int peekLock();
    }

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

    static CacheLocks create(SharedWriteLocks sharedWriteLocks) {
        return new CacheLocksImpl(sharedWriteLocks);
    }
}
