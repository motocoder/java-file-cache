#include <gtest/gtest.h>
#include <thread>
#include <atomic>
#include <vector>
#include <memory>
#include <random>
#include <chrono>
#include "cache/native_cache_locks.h"

// =====================================================================
// Basic reader/writer lock behavior — mirrors CacheLocksTest.testLock
// =====================================================================

TEST(NativeCacheLocksTest, ReaderDoesNotBlockReader) {
    IgnoredSharedWriteLocks shared;
    NativeCacheLocks locks(&shared);

    std::atomic<bool> flag1{false};

    locks.getLock(false); // reader

    std::thread t([&] {
        locks.getLock(false); // another reader should not block
        flag1 = true;
    });

    t.join();
    EXPECT_TRUE(flag1);

    locks.releaseLock(false);
    locks.releaseLock(false);
}

TEST(NativeCacheLocksTest, WriterBlockedByReader) {
    IgnoredSharedWriteLocks shared;
    NativeCacheLocks locks(&shared);

    std::atomic<bool> flag1{false};
    std::atomic<bool> flag2{false};

    locks.getLock(false); // reader

    std::thread t([&] {
        flag1 = true;
        locks.getLock(true); // writer should block
        flag2 = true;
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    EXPECT_TRUE(flag1);
    EXPECT_FALSE(flag2);

    locks.releaseLock(false); // release reader — writer should proceed

    t.join();
    EXPECT_TRUE(flag2);

    locks.releaseLock(true);
}

TEST(NativeCacheLocksTest, ReaderBlockedByWriter) {
    IgnoredSharedWriteLocks shared;
    NativeCacheLocks locks(&shared);

    std::atomic<bool> flag1{false};
    std::atomic<bool> flag2{false};

    locks.getLock(true); // writer

    std::thread t([&] {
        flag1 = true;
        locks.getLock(false); // reader should block
        flag2 = true;
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    EXPECT_TRUE(flag1);
    EXPECT_FALSE(flag2);

    locks.releaseLock(true); // release writer — reader should proceed

    t.join();
    EXPECT_TRUE(flag2);

    locks.releaseLock(false);
}

// =====================================================================
// Writer lock behavior — mirrors CacheLocksTest.testWriterLock
// =====================================================================

TEST(NativeCacheLocksTest, WriterBlocksWriterAndReader) {
    IgnoredSharedWriteLocks shared;
    NativeCacheLocks locks(&shared);

    std::atomic<bool> flag1{false};
    std::atomic<bool> flag2{false};

    locks.getLock(true); // writer

    std::thread t1([&] {
        flag1 = true;
        locks.getLock(true); // should block
        flag2 = true;
    });

    std::thread t2([&] {
        flag1 = true;
        locks.getLock(false); // should block
        flag2 = true;
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    EXPECT_TRUE(flag1);
    EXPECT_FALSE(flag2);

    locks.releaseLock(true);
    std::this_thread::sleep_for(std::chrono::milliseconds(10));

    // One of the two should have proceeded; release its lock so the other can go
    locks.releaseLock(true);
    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    locks.releaseLock(false);

    t1.join();
    t2.join();
    EXPECT_TRUE(flag2);
}

// =====================================================================
// Shared locks — mirrors CacheLocksTest.testLockShared
// =====================================================================

TEST(NativeCacheLocksTest, SharedLocksBehavior) {
    IgnoredSharedWriteLocks shared;
    NativeCacheLocks locks1(&shared);
    NativeCacheLocks locks2(&shared);

    std::atomic<bool> flag1{false};
    std::atomic<bool> flag2{false};
    std::atomic<bool> flag3{false};

    locks1.getLock(true); // writer on locks1

    // reader on locks1 should block (same instance has writer)
    std::thread t1([&] {
        flag1 = true;
        locks1.getLock(false);
        flag2 = true;
    });

    // reader on locks2 should NOT block (different instance, IgnoredWriteLocks)
    std::thread t2([&] {
        locks2.getLock(false);
        flag3 = true;
    });

    std::this_thread::sleep_for(std::chrono::milliseconds(10));
    EXPECT_TRUE(flag1);
    EXPECT_FALSE(flag2);
    EXPECT_TRUE(flag3); // locks2 reader is independent with IgnoredWriteLocks

    locks1.releaseLock(true);
    t1.join();
    t2.join();

    locks1.releaseLock(false);
    locks2.releaseLock(false);
}

// =====================================================================
// Many threads — mirrors CacheLocksTest.testLotsOfThreads
// =====================================================================

static constexpr int LOCK_COUNT = 100;
static constexpr int THREADS_COUNT = 200;

TEST(NativeCacheLocksTest, LotsOfThreads) {
    IgnoredSharedWriteLocks shared;

    std::vector<std::unique_ptr<NativeCacheLocks>> allLocks;
    allLocks.reserve(LOCK_COUNT);
    for (int i = 0; i < LOCK_COUNT; i++) {
        allLocks.push_back(std::make_unique<NativeCacheLocks>(&shared));
    }

    // Build lock types: 10% writers, 90% readers
    std::vector<bool> lockTypes; // true = writer
    int writes = THREADS_COUNT / 10;
    int reads = THREADS_COUNT - writes;
    for (int i = 0; i < writes; i++) lockTypes.push_back(true);
    for (int i = 0; i < reads; i++) lockTypes.push_back(false);

    std::mutex typesMtx;
    size_t typesIdx = 0;

    std::atomic<int> stopped{THREADS_COUNT};
    std::atomic<int> writersWriting{0};
    std::atomic<bool> multipleWriters{false};

    std::vector<std::thread> threads;
    threads.reserve(THREADS_COUNT);

    for (int i = 0; i < THREADS_COUNT; i++) {
        threads.emplace_back([&] {
            std::mt19937 rng(std::random_device{}());
            std::uniform_int_distribution<int> dist(0, LOCK_COUNT - 1);

            for (int iter = 0; iter < 100; iter++) {
                int idx = dist(rng);

                bool isWriter;
                {
                    std::lock_guard<std::mutex> g(typesMtx);
                    isWriter = lockTypes[typesIdx % lockTypes.size()];
                    typesIdx++;
                }

                NativeCacheLocks& lock = *allLocks[idx];

                lock.getLock(isWriter);

                if (isWriter) {
                    writersWriting++;
                    std::this_thread::sleep_for(std::chrono::milliseconds(1));
                }

                if (writersWriting.load() > 1) {
                    multipleWriters = true;
                }

                if (isWriter) {
                    writersWriting--;
                }

                lock.releaseLock(isWriter);
            }

            stopped--;
        });
    }

    for (auto& t : threads) t.join();

    EXPECT_EQ(0, stopped.load());
    // With IgnoredWriteLocks (no global coordination), multiple writers across
    // different lock instances CAN overlap — same as the Java test expects.
    EXPECT_TRUE(multipleWriters.load());
}
