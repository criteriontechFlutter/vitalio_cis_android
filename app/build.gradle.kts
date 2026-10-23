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
        versionCode = 34
        versionName = "2.0.27"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NutriAnalyser keys come from local.properties (not committed):
        // NUTRI_APP_ID=... and NUTRI_APP_SECRET=...
        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        fun quoted(key: String) = "\"" + localProps.getProperty(key, "").replace("\"", "\\\"") + "\""
        buildConfigField("String", "NUTRI_APP_ID", quoted("NUTRI_APP_ID"))
        buildConfigField("String", "NUTRI_APP_SECRET", quoted("NUTRI_APP_SECRET"))
        // OMRON Connectivity Library partner key: OMRON_PARTNER_KEY=...
        buildConfigField("String", "OMRON_PARTNER_KEY", quoted("OMRON_PARTNER_KEY"))
        // Backend for debug builds: API_ENV=LOCAL | STAGING | PRODUCTION (see ApiConfig.kt)
        buildConfigField("String", "API_ENV", "\"" + localProps.getProperty("API_ENV", "PRODUCTION").trim().uppercase() + "\"")
        // ENV_SWITCHER=true lets a release APK (e.g. one shared with testers) change server in-app
        buildConfigField("boolean", "ENV_SWITCHER", localProps.getProperty("ENV_SWITCHER", "false").trim().toBoolean().toString())
    }

    // Release signing reads the upload keystore from local.properties (never commit it):
    //   RELEASE_STORE_FILE=/abs/path/upload-keystore.jks
    //   RELEASE_STORE_PASSWORD=...
    //   RELEASE_KEY_ALIAS=...
    //   RELEASE_KEY_PASSWORD=...
    // Until those are set the build falls back to the debug key, which is what
    // Play currently has registered as the upload key.
    val signingProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val releaseStoreFile = signingProps.getProperty("RELEASE_STORE_FILE")?.let { file(it) }

    signingConfigs {
        if (releaseStoreFile != null && releaseStoreFile.exists()) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = signingProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = signingProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = signingProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // A release build starts on production; only an ENV_SWITCHER build can be changed in-app
            buildConfigField("String", "API_ENV", "\"PRODUCTION\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug").also {
                    logger.warn("⚠ Release build is signed with the DEBUG key — set RELEASE_* in local.properties")
                }
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
    // OMRON Connectivity Library 3.5.2 (partner SDK) — Bluetooth BP monitors and scales
    implementation(files("libs/omronconnectivitylibrary.aar"))

    // LiveKit: video calls with the doctor (teleconsultation)
    implementation("io.livekit:livekit-android:2.29.1")

    // Retries medicine "taken" logs from alarms when offline
    implementation(libs.androidx.work.runtime)
 }
