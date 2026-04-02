package llc.berserkr.cache.hash;

class CacheLocksImpl implements CacheLocks {

    private final SharedWriteLocks writeLocks;
    private final boolean fastPath;

    // Fast path: per-instance monitor object (used when SharedWriteLocks is ignored).
    // Each instance synchronizes on its own lock object instead of the shared one,
    // eliminating cross-instance contention.
    private final Object localLock;

    // Slow path synchronizes on writeLocks (shared across instances).
    private volatile int writers = 0;
    private volatile int readers = 0;

    CacheLocksImpl(SharedWriteLocks writeLocks) {
        this.writeLocks = writeLocks;
        this.fastPath = writeLocks instanceof IgnoredWriteLocks;
        this.localLock = fastPath ? new Object() : null;
    }

    static CacheLocks create(SharedWriteLocks sharedWriteLocks) {
        return new CacheLocksImpl(sharedWriteLocks);
    }

    @Override
    public void getLock(final LockType lockType) throws InterruptedException {
        if (fastPath) {
            getLockFast(lockType);
        } else {
            getLockSlow(lockType);
        }
    }

    @Override
    public void releaseLock(LockType lockType) {
        if (fastPath) {
            releaseLockFast(lockType);
        } else {
            releaseLockSlow(lockType);
        }
    }

    // Fast path: per-instance monitor, no global coordination.
    // Uses its own lock object so different CacheLocks instances never contend.

    private void getLockFast(LockType lockType) throws InterruptedException {
        while (true) {
            synchronized (localLock) {
                if (lockType == LockType.WRITER) {
                    if (writers == 0 && readers == 0) {
                        writers++;
                        return;
                    }
                } else {
                    if (writers == 0) {
                        readers++;
                        return;
                    }
                }
                localLock.wait();
            }
        }
    }

    private void releaseLockFast(LockType lockType) {
        synchronized (localLock) {
            if (lockType == LockType.WRITER) {
                writers--;
                localLock.notifyAll();
            } else {
                readers--;
                if (readers == 0) {
                    localLock.notifyAll();
                }
            }
        }
    }

    // Slow path: shared monitor for global write coordination.

    private void getLockSlow(final LockType lockType) throws InterruptedException {
        while (true) {
            synchronized (writeLocks) {
                switch (lockType) {
                    case READER: {
                        if (writers == 0 && writeLocks.peekLock() == 0) {
                            writeLocks.getLock(LockType.READER);
                            readers++;
                            return;
                        } else {
                            writeLocks.wait();
                        }
                        break;
                    }
                    case WRITER: {
                        if (writers == 0 && readers == 0 && writeLocks.peekLock() == 0) {
                            writeLocks.getLock(LockType.WRITER);
                            writers++;
                            return;
                        } else {
                            writeLocks.wait();
                        }
                        break;
                    }
                }
            }
        }
    }

    private void releaseLockSlow(LockType lockType) {
        synchronized (writeLocks) {
            switch (lockType) {
                case READER: {
                    readers--;
                    if (readers == 0 && writeLocks.peekLock() == 0 && writers == 0) {
                        writeLocks.notifyAll();
                    }
                    break;
                }
                case WRITER: {
                    writers--;
                    writeLocks.releaseLock();
                    if (writeLocks.peekLock() == 0 && writers == 0) {
                        writeLocks.notifyAll();
                    }
                    break;
                }
            }
        }
    }
}
