plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "llc.berserkr.androidfilecache"
    compileSdk = 36

    defaultConfig {
        applicationId = "llc.berserkr.androidfilecache"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }

    buildFeatures {
        compose = false
        aidl = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "log4j.properties"
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
