#pragma once

#include <mutex>
#include <condition_variable>
#include <stdexcept>

/// Coordinates write visibility across multiple NativeCacheLocks instances that share
/// the same SharedWriteLocks. Each NativeCacheLocks guards a single key (hash bucket),
/// but SharedWriteLocks controls whether writes on one key block reads on all other keys.
///
/// The mutex and condition_variable live here because in the Java version,
/// all CacheLocks instances sharing the same SharedWriteLocks synchronize on
/// and wait/notify on that single shared object.
class SharedWriteLocks {
public:
    std::mutex mtx;
    std::condition_variable cv;

    virtual ~SharedWriteLocks() = default;

    /// Called while mtx is held — no internal locking needed.
    virtual int getLock(bool isWriter) = 0;
    virtual void releaseLock() = 0;
    virtual int peekLock() = 0;
};

/// Per-key locking only — writes on one key do not block reads or writes on other keys.
/// Only concurrent access to the same key is synchronized: a write blocks reads and other
/// writes on that key, but concurrent reads of the same key are allowed. This is the
/// optimized default used by FileHash and StreamingFileHash.
class IgnoredSharedWriteLocks : public SharedWriteLocks {
public:
    int getLock(bool /*isWriter*/) override { return 0; }
    void releaseLock() override {}
    int peekLock() override { return 0; }
};

/// Global write lock — when any key is being written, all reads on all keys are blocked
/// until the write completes. This was used for an older cache design before per-bucket
/// locking was optimized. Retained for cases that require strict global consistency.
class StandardSharedWriteLocks : public SharedWriteLocks {
    int writeLocks_ = 0;
public:
    int getLock(bool isWriter) override {
        if (isWriter) return writeLocks_++;
        return writeLocks_;
    }
    void releaseLock() override { writeLocks_--; }
    int peekLock() override { return writeLocks_; }
};

/// Native implementation of CacheLocks. Mirrors CacheLocksImpl from Java.
/// Uses the shared mutex/cv from SharedWriteLocks for synchronization,
/// matching the Java pattern where synchronized(writeLocks) is the monitor.
class NativeCacheLocks {
public:
    explicit NativeCacheLocks(SharedWriteLocks* shared);
    ~NativeCacheLocks() = default;

    void getLock(bool isWriter);
    void releaseLock(bool isWriter);

private:
    SharedWriteLocks* shared_;
    int writers_ = 0;
    int readers_ = 0;
};
