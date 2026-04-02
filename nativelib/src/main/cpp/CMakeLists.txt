cmake_minimum_required(VERSION 4.2.1)

project("nativelib")

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(${CMAKE_PROJECT_NAME} SHARED
        nativelib.cpp
        cache/native_cache.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)

# Test target — EXCLUDE_FROM_ALL so nothing here is built during the Android build,
# but Android Studio can index the sources and resolve includes (gtest, etc.).
include(FetchContent)
FetchContent_Declare(
        googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.15.2
        EXCLUDE_FROM_ALL
)
FetchContent_MakeAvailable(googletest)

add_executable(native_cache_test EXCLUDE_FROM_ALL
        tests/NativeCacheTest.cpp
        cache/native_cache.cpp
)
target_include_directories(native_cache_test PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_link_libraries(native_cache_test GTest::gtest_main)
