plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ezg.babytalk"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ezg.babytalk"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

kotlin { jvmToolchain(17) }
