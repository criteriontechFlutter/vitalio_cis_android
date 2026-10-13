// Shared, UI-free code used by both the phone app (:app) and the Wear OS app (:wear).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.critetiontech.ctvitalio.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.google.code.gson:gson:2.13.1")
    // Dictation WebSocket
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
}