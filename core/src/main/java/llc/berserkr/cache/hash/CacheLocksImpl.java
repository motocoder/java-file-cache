package llc.berserkr.cache.hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO write this in C
 */
class CacheLocksImpl implements CacheLocks {

    private static final Logger logger = LoggerFactory.getLogger(CacheLocksImpl.class);

    private final SharedWriteLocks writeLocks;

    private volatile int writers = 0;
    private volatile int readers = 0;

    CacheLocksImpl(SharedWriteLocks writeLocks) {
        this.writeLocks = writeLocks;
    }

    static CacheLocks create(SharedWriteLocks sharedWriteLocks) {
        return new CacheLocksImpl(sharedWriteLocks);
    }

    @Override
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
                            writeLocks.getLock(CacheLocks.LockType.READER);
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

    @Override
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
                        writeLocks.notify();
                    }
                    break;
                }
            }
        }
    }
}
