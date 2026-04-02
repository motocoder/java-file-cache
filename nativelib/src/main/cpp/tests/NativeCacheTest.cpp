#include <gtest/gtest.h>
#include <cstring>
#include <string>
#include <vector>
#include "cache/native_cache.h"

// =====================================================================
// Construction tests
// =====================================================================

TEST(NativeCacheConstruction, CreatesWithBucketCount) {
    NativeCache cache(100);
    EXPECT_EQ(0, cache.getEntryCount());
}

TEST(NativeCacheConstruction, CreatesWithSmallBucketCount) {
    NativeCache cache(1);
    EXPECT_EQ(0, cache.getEntryCount());
}

// =====================================================================
// Put tests
// =====================================================================

TEST(NativeCachePut, SingleEntry) {
    NativeCache cache(100);
    std::string key = "test-key";
    std::string value = "test-value";

    bool result = cache.put(reinterpret_cast<const uint8_t*>(key.data()), key.size(),
                            reinterpret_cast<const uint8_t*>(value.data()), value.size());
    EXPECT_TRUE(result);
    EXPECT_EQ(1, cache.getEntryCount());
}

TEST(NativeCachePut, MultipleEntries) {
    NativeCache cache(100);

    for (int i = 0; i < 10; i++) {
        std::string key = "key-" + std::to_string(i);
        std::string value = "value-" + std::to_string(i);
        cache.put(reinterpret_cast<const uint8_t*>(key.data()), key.size(),
                  reinterpret_cast<const uint8_t*>(value.data()), value.size());
    }
    EXPECT_EQ(10, cache.getEntryCount());
}

TEST(NativeCachePut, EmptyKeyAndValue) {
    NativeCache cache(100);
    bool result = cache.put(nullptr, 0, nullptr, 0);
    EXPECT_TRUE(result);
    EXPECT_EQ(1, cache.getEntryCount());
}

TEST(NativeCachePut, LargeValue) {
    NativeCache cache(100);
    std::string key = "large";
    std::vector<uint8_t> value(1024 * 1024, 0xAB);

    bool result = cache.put(reinterpret_cast<const uint8_t*>(key.data()), key.size(),
                            value.data(), value.size());
    EXPECT_TRUE(result);
}

// =====================================================================
// Get tests (stubs return not-found)
// =====================================================================

TEST(NativeCacheGet, NotFoundReturnsNegative) {
    NativeCache cache(100);
    std::string key = "missing";
    uint8_t buf[64];

    int64_t result = cache.get(reinterpret_cast<const uint8_t*>(key.data()), key.size(),
                               buf, sizeof(buf));
    EXPECT_EQ(-1, result);
}

// =====================================================================
// Exists tests (stubs return false)
// =====================================================================

TEST(NativeCacheExists, ReturnsFalseForMissing) {
    NativeCache cache(100);
    std::string key = "missing";

    bool result = cache.exists(reinterpret_cast<const uint8_t*>(key.data()), key.size());
    EXPECT_FALSE(result);
}

// =====================================================================
// Remove tests (stubs return false)
// =====================================================================

TEST(NativeCacheRemove, ReturnsFalseForMissing) {
    NativeCache cache(100);
    std::string key = "missing";

    bool result = cache.remove(reinterpret_cast<const uint8_t*>(key.data()), key.size());
    EXPECT_FALSE(result);
}

// =====================================================================
// Destruction test
// =====================================================================

TEST(NativeCacheDestruction, CleanDestruction) {
    auto* cache = new NativeCache(100);
    std::string key = "key";
    std::string value = "value";
    cache->put(reinterpret_cast<const uint8_t*>(key.data()), key.size(),
               reinterpret_cast<const uint8_t*>(value.data()), value.size());
    delete cache;
}
