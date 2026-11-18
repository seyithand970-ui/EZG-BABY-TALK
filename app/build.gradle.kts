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
        versionCode = 2
        versionName = "0.2.0"
    }
}
dependencies {
    implementation("io.github.webrtc-sdk:android:150.7871.04")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
kotlin { jvmToolchain(17) }
