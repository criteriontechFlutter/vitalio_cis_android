import java.util.Properties

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
        versionCode = 32
        versionName = "2.0.25"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NutriAnalyser keys come from local.properties (not committed):
        // NUTRI_APP_ID=... and NUTRI_APP_SECRET=...
        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        fun quoted(key: String) = "\"" + localProps.getProperty(key, "").replace("\"", "\\\"") + "\""
        buildConfigField("String", "NUTRI_APP_ID", quoted("NUTRI_APP_ID"))
        buildConfigField("String", "NUTRI_APP_SECRET", quoted("NUTRI_APP_SECRET"))
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
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
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
    implementation(libs.retrofit)

    // Gson Converter
    implementation(libs.converter.gson)

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    // OkHttp Logging
    implementation(libs.logging.interceptor)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Hilt kept as runtime-only; annotation processing requires a compatible AGP/Kotlin combination.
    // Currently blocked by Kotlin 2.1.0 + AGP 8.11.0 javapoet incompatibility in Hilt 2.55.
    // Uncomment annotation processor when a compatible version is available.
    implementation("com.google.dagger:hilt-android:2.60.1")

    implementation(libs.navigation.compose)

    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.accompanist.permissions)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.android.image.cropper)
    implementation(libs.androidx.appcompat)

    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-crashlytics:20.1.1")
    implementation("com.google.firebase:firebase-analytics:23.2.0")

    // Android Health Connect — pulls data from Wear OS / Samsung Health / Google Fit
    implementation(libs.androidx.connect.client)

    // Retries medicine "taken" logs from alarms when offline
    implementation(libs.androidx.work.runtime)
 }
