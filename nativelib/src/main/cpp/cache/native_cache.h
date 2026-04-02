#pragma once

#include <cstdint>
#include <cstddef>

/// Native file-backed hash cache.
/// Stub interface — real implementation TBD.
class NativeCache {
public:
    NativeCache(int bucketCount);
    ~NativeCache();

    /// Store a key-value pair. Returns true on success.
    bool put(const uint8_t* key, size_t keyLen,
             const uint8_t* value, size_t valueLen);

    /// Retrieve a value by key. Returns the value length, or -1 if not found.
    /// Caller provides a buffer; if bufLen is too small, returns required size.
    int64_t get(const uint8_t* key, size_t keyLen,
                uint8_t* buf, size_t bufLen) const;

    /// Check if a key exists.
    bool exists(const uint8_t* key, size_t keyLen) const;

    /// Remove a key. Returns true if the key was found and removed.
    bool remove(const uint8_t* key, size_t keyLen);

    /// Return the number of stored entries.
    int getEntryCount() const;

private:
    int bucketCount_;
    int entryCount_ = 0;
};
