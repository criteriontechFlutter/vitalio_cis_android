import java.util.Properties

// Wear OS companion app. Shares the phone app's applicationId and signing key so the
// Wearable Data Layer can pair the two installs.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.critetiontech.ctvitalio.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.critetiontech.ctvitalio"
        minSdk = 30
        targetSdk = 36
        // Version lives in version.properties at the repo root (shared with :app)
        val versionProps = Properties().apply {
            rootProject.file("version.properties").inputStream().use { load(it) }
        }
        versionCode = versionProps.getProperty("WEAR_VERSION_CODE").trim().toInt()
        versionName = versionProps.getProperty("WEAR_VERSION_NAME").trim()
    }

    // Same upload key as the phone app (RELEASE_* in local.properties). Play rejects debug-signed bundles.
    val signingProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val releaseStoreFile = signingProps.getProperty("RELEASE_STORE_FILE")?.let { rootProject.file(it).takeIf { f -> f.exists() } ?: file(it) }

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
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Must match the phone app's signing config (see app/build.gradle.kts)
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug").also {
                    logger.warn("⚠ Wear release build is signed with the DEBUG key — set RELEASE_* in local.properties")
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
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.wearable)
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
}