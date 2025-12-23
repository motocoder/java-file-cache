package llc.berserkr.cache.hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Reading and writing operations can happen simultaneous as long as they don't conflict at several points.
 *
 * Each write operation manipulated the transactional data portion of the SegmentedFile. This currently is a conflict
 * that could be changed.
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
 * This class handles just the locks for the hash index blobs.
 *
 * TODO write this in C
 */
public class HashLocks {

    private static final Logger logger = LoggerFactory.getLogger(HashLocks.class);

    private final SharedWriteLocks writeLocks;

    public enum LockType {READER, WRITER}

    private int writers = 0;
    private int readers = 0;

    public HashLocks(SharedWriteLocks writeLocks) {
        this.writeLocks = writeLocks;
    }

    public void getLock(final LockType lockType) throws InterruptedException {

        while(true) {

            synchronized(writeLocks) {

                if(writers < 0 || readers < 0 || writeLocks.peekLock() < 0) {
                    throw new IllegalStateException("someone released too many locks " + writeLocks.peekLock() + " " + readers + " " + writers);
                }

                switch (lockType) {
                    case READER: {
                        //as long as there's no writers, good to go
                        if (writers == 0 && writeLocks.peekLock() == 0) {
                            //reader doesn't care about other writers just our own
                            writeLocks.getLock(HashLocks.LockType.READER);
                            readers++; //lock it for writers
                            return;
                        } else {
                            //someone is writing just wait
                            writeLocks.wait();
                        }
                        break;
                    }
                    case WRITER: {

                        if (writers == 0 && readers == 0 && writeLocks.peekLock() == 0) {
                            writeLocks.getLock(LockType.WRITER);
                            writers++; // lock it for readers and writers
                            return;
                        } else {
                            //if global locks aren't blocked but others are
                            writeLocks.wait();
                        }
                        break;
                    }
                }
            }
        }
    }

    public void releaseLock(LockType lockType) {

        synchronized(writeLocks) {
            switch (lockType) {
                case READER: {
                    readers--;
                    if((readers == 0 && writeLocks.peekLock() == 0 && writers == 0)) {
                        writeLocks.notify();
                    }
                    break;
                }

                case WRITER: {
                    writers--;
                    writeLocks.releaseLock(); //only write releases global lock
                    if((writeLocks.peekLock() == 0 && writers == 0)) {
                        writeLocks.notifyAll();
                    }
                    break;
                }
            }
        }
    }

    public static class SharedWriteLocks {

        private int writeLocks = 0;

        private synchronized int getLock(LockType lockType) {

            if(lockType == LockType.WRITER) {
                return writeLocks++;
            }
            else {
                return writeLocks;
            }

        }

        private synchronized void releaseLock() {
            writeLocks--;
//            this.notifyAll();
        }

        public synchronized int peekLock() {
            return writeLocks;
        }
    }

}
