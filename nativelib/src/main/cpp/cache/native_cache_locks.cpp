#include "native_cache_locks.h"

NativeCacheLocks::NativeCacheLocks(SharedWriteLocks* shared)
    : shared_(shared), fastPath_(shared->isIgnored()) {}

void NativeCacheLocks::getLock(bool isWriter) {
    if (fastPath_) {
        getLockFast(isWriter);
    } else {
        getLockSlow(isWriter);
    }
}

void NativeCacheLocks::releaseLock(bool isWriter) {
    if (fastPath_) {
        releaseLockFast(isWriter);
    } else {
        releaseLockSlow(isWriter);
    }
}

// Fast path: per-instance std::shared_mutex, no global coordination.
// std::shared_mutex is backed by OS-level primitives (pthread_rwlock on macOS/Linux)
// and allows concurrent readers with exclusive writers, with no cross-instance contention.

void NativeCacheLocks::getLockFast(bool isWriter) {
    if (isWriter) {
        rwlock_.lock();
    } else {
        rwlock_.lock_shared();
    }
}

void NativeCacheLocks::releaseLockFast(bool isWriter) {
    if (isWriter) {
        rwlock_.unlock();
    } else {
        rwlock_.unlock_shared();
    }
}

// Slow path: shared mutex + condition_variable for global write coordination.
// Used when StandardSharedWriteLocks is in play — any write on any instance
// blocks reads on all instances.

void NativeCacheLocks::getLockSlow(bool isWriter) {
    std::unique_lock<std::mutex> lock(shared_->mtx);

    while (true) {
        if (isWriter) {
            if (writers_ == 0 && readers_ == 0 && shared_->peekLock() == 0) {
                shared_->getLock(true);
                writers_++;
                return;
            }
        } else {
            if (writers_ == 0 && shared_->peekLock() == 0) {
                shared_->getLock(false);
                readers_++;
                return;
            }
        }

        shared_->cv.wait(lock);
    }
}

void NativeCacheLocks::releaseLockSlow(bool isWriter) {
    std::lock_guard<std::mutex> lock(shared_->mtx);

    if (isWriter) {
        writers_--;
        shared_->releaseLock();
        if (shared_->peekLock() == 0 && writers_ == 0) {
            shared_->cv.notify_all();
        }
    } else {
        readers_--;
        if (readers_ == 0 && shared_->peekLock() == 0 && writers_ == 0) {
            shared_->cv.notify_all();
        }
    }
}
