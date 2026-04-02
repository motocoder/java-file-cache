cmake_minimum_required(VERSION 4.2.1)

project("nativelib_test" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG v1.15.2
)
FetchContent_MakeAvailable(googletest)

set(MAIN_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../main/cpp")

add_executable(native_cache_test
    "${MAIN_CPP_DIR}/tests/NativeCacheTest.cpp"
    "${MAIN_CPP_DIR}/cache/native_cache.cpp"
)

target_include_directories(native_cache_test PRIVATE "${MAIN_CPP_DIR}")
target_link_libraries(native_cache_test GTest::gtest_main)

add_executable(native_cache_locks_test
    "${MAIN_CPP_DIR}/tests/NativeCacheLocksTest.cpp"
    "${MAIN_CPP_DIR}/cache/native_cache_locks.cpp"
)

target_include_directories(native_cache_locks_test PRIVATE "${MAIN_CPP_DIR}")
target_link_libraries(native_cache_locks_test GTest::gtest_main)

# Host shared library for JVM unit tests
find_package(JNI REQUIRED)

add_library(nativelib SHARED
    "${MAIN_CPP_DIR}/nativelib.cpp"
    "${MAIN_CPP_DIR}/cache/native_cache.cpp"
    "${MAIN_CPP_DIR}/cache/native_cache_locks.cpp"
)

target_include_directories(nativelib PRIVATE "${MAIN_CPP_DIR}" ${JNI_INCLUDE_DIRS})
target_link_libraries(nativelib ${JNI_LIBRARIES})

include(GoogleTest)
gtest_discover_tests(native_cache_test)
gtest_discover_tests(native_cache_locks_test)
