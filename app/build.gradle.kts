plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
}

android {
    namespace = "com.critetiontech.ctvitalio"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.critetiontech.ctvitalio"
        minSdk = 26
        targetSdk = 36
        versionCode = 31
        versionName = "2.0.24"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}


dependencies {
    implementation(project(":core"))
    // Syncs the logged-in patient to the Wear OS app
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.compose.foundation.layout)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.tracing.perfetto.handshake)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.common.ktx)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.material3)
    implementation(libs.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation.foundation)
    implementation(libs.ui)
    implementation(libs.androidx.contentpager)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.24.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)






//    added dip

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // Gson Converter
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    // OkHttp Logging
    implementation("com.squareup.okhttp3:logging-interceptor:5.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // Hilt kept as runtime-only; annotation processing requires a compatible AGP/Kotlin combination.
    // Currently blocked by Kotlin 2.1.0 + AGP 8.11.0 javapoet incompatibility in Hilt 2.55.
    // Uncomment annotation processor when a compatible version is available.
    implementation("com.google.dagger:hilt-android:2.60.1")

    implementation("androidx.navigation:navigation-compose:2.10.2")

    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.vanniktech:android-image-cropper:4.7.0")
    implementation("androidx.appcompat:appcompat:1.8.0")

    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-crashlytics:20.1.1")
    implementation("com.google.firebase:firebase-analytics:23.2.0")

    // Android Health Connect — pulls data from Wear OS / Samsung Health / Google Fit
    implementation("androidx.health.connect:connect-client:1.1.0")
 }
