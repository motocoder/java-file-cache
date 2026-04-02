#include "native_cache_locks.h"

NativeCacheLocks::NativeCacheLocks(SharedWriteLocks* shared)
    : shared_(shared) {}

void NativeCacheLocks::getLock(bool isWriter) {
    std::unique_lock<std::mutex> lock(shared_->mtx);

    while (true) {
        if (writers_ < 0 || readers_ < 0 || shared_->peekLock() < 0) {
            throw std::runtime_error("someone released too many locks");
        }

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

void NativeCacheLocks::releaseLock(bool isWriter) {
    std::lock_guard<std::mutex> lock(shared_->mtx);

    if (isWriter) {
        writers_--;
        shared_->releaseLock();
        if (shared_->peekLock() == 0 && writers_ == 0) {
            shared_->cv.notify_one();
        }
    } else {
        readers_--;
        if (readers_ == 0 && shared_->peekLock() == 0 && writers_ == 0) {
            shared_->cv.notify_one();
        }
    }
}
