#pragma once

#include <mutex>
#include <shared_mutex>
#include <condition_variable>
#include <stdexcept>

/// Coordinates write visibility across multiple NativeCacheLocks instances that share
/// the same SharedWriteLocks. Each NativeCacheLocks guards a single key (hash bucket),
/// but SharedWriteLocks controls whether writes on one key block reads on all other keys.
///
/// The mutex and condition_variable live here for the StandardSharedWriteLocks slow path,
/// where all CacheLocks instances sharing the same SharedWriteLocks must coordinate globally.
class SharedWriteLocks {
public:
    std::mutex mtx;
    std::condition_variable cv;

    virtual ~SharedWriteLocks() = default;

    virtual bool isIgnored() const { return false; }

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
    bool isIgnored() const override { return true; }
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
///
/// When using IgnoredSharedWriteLocks (the default), each instance uses its own
/// std::shared_mutex for a kernel-optimized reader-writer lock with zero contention
/// between different lock instances.
///
/// When using StandardSharedWriteLocks, falls back to the shared mutex/cv pattern
/// for global write coordination.
class NativeCacheLocks {
public:
    explicit NativeCacheLocks(SharedWriteLocks* shared);
    ~NativeCacheLocks() = default;

    void getLock(bool isWriter);
    void releaseLock(bool isWriter);

private:
    SharedWriteLocks* shared_;
    bool fastPath_;

    // Fast path: per-instance shared_mutex (used when SharedWriteLocks is ignored)
    std::shared_mutex rwlock_;

    // Slow path state (used when SharedWriteLocks is NOT ignored)
    int writers_ = 0;
    int readers_ = 0;

    void getLockFast(bool isWriter);
    void releaseLockFast(bool isWriter);
    void getLockSlow(bool isWriter);
    void releaseLockSlow(bool isWriter);
};
