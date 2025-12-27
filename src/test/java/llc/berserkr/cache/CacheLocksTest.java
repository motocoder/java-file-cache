package llc.berserkr.cache;

import llc.berserkr.cache.hash.CacheLocks;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CacheLocksTest {

    private static final Logger logger = LoggerFactory.getLogger(CacheLocksTest.class);

    boolean flag2 = false;
    boolean flag1 = false;
    boolean flag3 = false;

    private static final boolean allWritesBlockReads = false;

    @Test
    public void testLock() throws InterruptedException {

        final ExecutorService executor = Executors.newCachedThreadPool();

        final CacheLocks.SharedWriteLocks sharedWriteLocks = new CacheLocks.IgnoredWriteLocks();
        final CacheLocks locks = new CacheLocks(sharedWriteLocks);

        {

            assertFalse(flag1);
            assertFalse(flag2);
            locks.getLock(CacheLocks.LockType.READER);
            //set a reader lock the set another and make sure it doesn't block
            executor.execute(() -> {

                getLock(locks, CacheLocks.LockType.READER);

                flag1 = true;
            });

            Thread.sleep(10);
            assertTrue(flag1);
            flag1 = false;

            //make sure writer lock is blocked when reader is out
            executor.execute(() -> {

                flag1 = true;
                getLock(locks, CacheLocks.LockType.WRITER);

                flag2 = true;
            });

            Thread.sleep(10);
            assertTrue(flag1);
            assertFalse(flag2);

            //writer should go now that readers left
            locks.releaseLock(CacheLocks.LockType.READER);

            Thread.sleep(10);

            locks.releaseLock(CacheLocks.LockType.READER);

            Thread.sleep(10);
            assertTrue(flag2);

            flag1 = false;
            flag2 = false;

//            //writer is out, so reader should be blocked.
            executor.execute(() -> {

                flag1 = true;
                getLock(locks, CacheLocks.LockType.READER);

                flag2 = true;
            });

            Thread.sleep(10);
            assertTrue(flag1);
            assertFalse(flag2);

            locks.releaseLock(CacheLocks.LockType.WRITER);
            Thread.sleep(10); //let the blocked reader obtain a lock
            locks.releaseLock(CacheLocks.LockType.READER);

            Thread.sleep(10);
            assertTrue(flag2);


        }

    }

    @Test
    public void testWriterLock() throws InterruptedException {

        final ExecutorService executor = Executors.newCachedThreadPool();

        final CacheLocks.SharedWriteLocks sharedWriteLocks = new CacheLocks.IgnoredWriteLocks();
        final CacheLocks locks = new CacheLocks(sharedWriteLocks);

        flag1 = false;
        flag2 = false;

        locks.getLock(CacheLocks.LockType.WRITER);

        //writer is out, so all should be blocked
        executor.execute(() -> {

            flag1 = true;
            getLock(locks, CacheLocks.LockType.WRITER);

            flag2 = true;
        });

        //writer is out, so all should be blocked
        executor.execute(() -> {

            flag1 = true;
            getLock(locks, CacheLocks.LockType.READER);

            flag2 = true;
        });

        Thread.sleep(10);
        assertTrue(flag1);
        assertFalse(flag2);

        locks.releaseLock(CacheLocks.LockType.WRITER);
        Thread.sleep(10);
        locks.releaseLock(CacheLocks.LockType.WRITER);
        Thread.sleep(10);
        locks.releaseLock(CacheLocks.LockType.READER);

        Thread.sleep(20);
        assertTrue(flag2);

    }

    @Test
    public void testLockShared() throws InterruptedException {

        final ExecutorService executor = Executors.newCachedThreadPool();

        final CacheLocks.SharedWriteLocks sharedWriteLocks = new CacheLocks.IgnoredWriteLocks();
        final CacheLocks locks = new CacheLocks(sharedWriteLocks);
        final CacheLocks locks2 = new CacheLocks(sharedWriteLocks);

        //alright lets do the shared locks now.
        {
            flag1 = false;
            flag2 = false;

            locks.getLock(CacheLocks.LockType.WRITER);

//            writer is out, so all should be blocked
            executor.execute(() -> {

                flag1 = true;
                getLock(locks, CacheLocks.LockType.READER);

                flag2 = true;
            });

            //writer is out, so all should be blocked
            executor.execute(() -> {

                flag1 = true;
                getLock(locks2, CacheLocks.LockType.READER);

                flag3 = true;
            });

            //writer is out, so all should be blocked
            executor.execute(() -> {

                flag1 = true;
                getLock(locks, CacheLocks.LockType.WRITER);

                flag2 = true;
            });

            //writer is out, so all should be blocked
            executor.execute(() -> {

                flag1 = true;
                getLock(locks2, CacheLocks.LockType.WRITER);

                flag2 = true;
            });

            Thread.sleep(10);
            assertTrue(flag1);
            assertFalse(flag2);
            assertTrue(allWritesBlockReads ? !flag3 : flag3);

        }

    }

    private static final int LOCK_COUNT = 100;
    private static final int THREADS_COUNT = 200;

    @Test
    public void testLotsOfThreads() throws InterruptedException {

        final ExecutorService executor = Executors.newCachedThreadPool();

        final CacheLocks.SharedWriteLocks sharedWriteLocks = new CacheLocks.IgnoredWriteLocks();

        final Map<Integer, CacheLocks> allLocks = new ConcurrentHashMap<>();
        final List<CacheLocks.LockType> lockTypes = Collections.synchronizedList(new ArrayList<>());

        int writes = THREADS_COUNT / 10;
        int reads = THREADS_COUNT - writes;

        for(int i = 0; i < writes; i++) {
            lockTypes.add(CacheLocks.LockType.WRITER);
        }

        for(int i = 0; i < reads; i++) {
            lockTypes.add(CacheLocks.LockType.READER);
        }

        for(int i = 0; i < LOCK_COUNT; i++) {
            allLocks.put(i, new CacheLocks(sharedWriteLocks));
        }

        {

            flag1 = false;
            flag2 = false;
            flag3 = false;

        }

        final AtomicInteger started = new AtomicInteger(0);
        final AtomicInteger stopped = new AtomicInteger(THREADS_COUNT);

        final AtomicInteger writersWriting = new AtomicInteger(0);
        final AtomicInteger readersReading = new AtomicInteger(0);

        for(int i = 0; i < THREADS_COUNT; i++) {

            final int iFinal = i;

            executor.execute(() -> {

                for(int lockIter = 0; lockIter < 100; lockIter++) {

                    final int random = (int) (Math.random() * 100);
                    final CacheLocks.LockType type = lockTypes.removeFirst();

                    final CacheLocks lock = allLocks.get(random);

                    try {

                        lock.getLock(type);

                        switch (type) {
                            case READER -> {
                                readersReading.incrementAndGet();
                            }
                            case WRITER -> {
                                writersWriting.incrementAndGet();
                                Thread.sleep(1);
                            }
                        }

                        if (writersWriting.get() > 1) {
                            flag1 = true;
                        }

                        switch (type) {
                            case READER -> {
                                readersReading.decrementAndGet();
                            }
                            case WRITER -> {
                                writersWriting.decrementAndGet();
                            }
                        }

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } finally {

                        lockTypes.add(type);

                        lock.releaseLock(type);

                    }
                }

                stopped.decrementAndGet();

            });
        }

        executor.shutdown();
        executor.awaitTermination(
                10000, TimeUnit.SECONDS
        );

        assertEquals(0, stopped.get());

        assertTrue(flag1);

    }

    private void getLock(CacheLocks locks, CacheLocks.LockType type) {

        try {
            locks.getLock(type);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBS() {

        int alpha = (int) ((Math.min(System.currentTimeMillis() - (System.currentTimeMillis() - 200), 300) / 300f) * 255);
        logger.info("alpha = " + alpha);
    }
}
