plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "llc.berserkr.cache.native_lib"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++23")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "4.2.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val hostNativeBuildDir = layout.buildDirectory.dir("host-native")
val cmakeBin = listOf("/opt/homebrew/bin/cmake", "/usr/local/bin/cmake", "cmake")
    .firstOrNull { file(it).canExecute() } ?: "cmake"

val configureHostNative by tasks.registering(Exec::class) {
    val buildDir = hostNativeBuildDir.get().asFile
    inputs.file("src/test/cpp/CMakeLists.txt")
    outputs.file(hostNativeBuildDir.map { it.file("CMakeCache.txt") })
    doFirst { buildDir.mkdirs() }
    workingDir(buildDir)
    commandLine(cmakeBin, file("src/test/cpp").absolutePath, "-DCMAKE_BUILD_TYPE=Release")
}

val buildNativeTests by tasks.registering(Exec::class) {
    dependsOn(configureHostNative)
    val buildDir = hostNativeBuildDir.get().asFile
    inputs.files(
        fileTree("src/main/cpp/cache") { include("native_cache.cpp", "native_cache.h") },
        file("src/main/cpp/tests/NativeCacheTest.cpp"),
    )
    outputs.dir(buildDir)
    workingDir(buildDir)
    commandLine(cmakeBin, "--build", ".", "--config", "Release")
}

val runNativeTests by tasks.registering(Exec::class) {
    dependsOn(buildNativeTests)
    val buildDir = hostNativeBuildDir.get().asFile
    workingDir(buildDir)
    commandLine("./native_cache_test", "--gtest_color=yes")
}

tasks.named("check") { dependsOn(runNativeTests) }

dependencies {
    implementation(project(":core"))
    implementation(libs.appcompat)
    implementation(libs.slf4j.api)
    testImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
