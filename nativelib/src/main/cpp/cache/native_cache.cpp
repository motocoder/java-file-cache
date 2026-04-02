#include "native_cache.h"

NativeCache::NativeCache(int bucketCount)
    : bucketCount_(bucketCount) {}

NativeCache::~NativeCache() = default;

bool NativeCache::put(const uint8_t* key, size_t keyLen,
                      const uint8_t* value, size_t valueLen) {
    // Stub — real implementation TBD
    entryCount_++;
    return true;
}

int64_t NativeCache::get(const uint8_t* key, size_t keyLen,
                         uint8_t* buf, size_t bufLen) const {
    // Stub — not found
    return -1;
}

bool NativeCache::exists(const uint8_t* key, size_t keyLen) const {
    // Stub
    return false;
}

bool NativeCache::remove(const uint8_t* key, size_t keyLen) {
    // Stub
    return false;
}

int NativeCache::getEntryCount() const {
    return entryCount_;
}
