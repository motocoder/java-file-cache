#include <jni.h>
#include "cache/native_cache.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativeCreate(JNIEnv* env, jobject thiz, jint bucketCount) {
    auto* cache = new NativeCache(bucketCount);
    return reinterpret_cast<jlong>(cache);
}

JNIEXPORT void JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativeDestroy(JNIEnv* env, jobject thiz, jlong handle) {
    delete reinterpret_cast<NativeCache*>(handle);
}

JNIEXPORT jboolean JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativePut(JNIEnv* env, jobject thiz, jlong handle,
                                                     jbyteArray key, jbyteArray value) {
    auto* cache = reinterpret_cast<NativeCache*>(handle);

    jsize keyLen = env->GetArrayLength(key);
    jsize valLen = env->GetArrayLength(value);
    auto* keyBytes = env->GetByteArrayElements(key, nullptr);
    auto* valBytes = env->GetByteArrayElements(value, nullptr);

    bool result = cache->put(reinterpret_cast<uint8_t*>(keyBytes), keyLen,
                             reinterpret_cast<uint8_t*>(valBytes), valLen);

    env->ReleaseByteArrayElements(key, keyBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(value, valBytes, JNI_ABORT);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativeGet(JNIEnv* env, jobject thiz, jlong handle,
                                                     jbyteArray key) {
    auto* cache = reinterpret_cast<NativeCache*>(handle);

    jsize keyLen = env->GetArrayLength(key);
    auto* keyBytes = env->GetByteArrayElements(key, nullptr);

    // Probe for size first
    int64_t size = cache->get(reinterpret_cast<uint8_t*>(keyBytes), keyLen, nullptr, 0);

    if (size < 0) {
        env->ReleaseByteArrayElements(key, keyBytes, JNI_ABORT);
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(size));
    auto* buf = env->GetByteArrayElements(result, nullptr);
    cache->get(reinterpret_cast<uint8_t*>(keyBytes), keyLen,
               reinterpret_cast<uint8_t*>(buf), size);

    env->ReleaseByteArrayElements(key, keyBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(result, buf, 0);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativeExists(JNIEnv* env, jobject thiz, jlong handle,
                                                        jbyteArray key) {
    auto* cache = reinterpret_cast<NativeCache*>(handle);

    jsize keyLen = env->GetArrayLength(key);
    auto* keyBytes = env->GetByteArrayElements(key, nullptr);

    bool result = cache->exists(reinterpret_cast<uint8_t*>(keyBytes), keyLen);

    env->ReleaseByteArrayElements(key, keyBytes, JNI_ABORT);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativeRemove(JNIEnv* env, jobject thiz, jlong handle,
                                                        jbyteArray key) {
    auto* cache = reinterpret_cast<NativeCache*>(handle);

    jsize keyLen = env->GetArrayLength(key);
    auto* keyBytes = env->GetByteArrayElements(key, nullptr);

    bool result = cache->remove(reinterpret_cast<uint8_t*>(keyBytes), keyLen);

    env->ReleaseByteArrayElements(key, keyBytes, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
Java_llc_berserkr_cache_NativeCacheBridge_nativeGetEntryCount(JNIEnv* env, jobject thiz, jlong handle) {
    auto* cache = reinterpret_cast<NativeCache*>(handle);
    return cache->getEntryCount();
}

} // extern "C"
