# Dependency and Permission Register

Generated on 2026-11-05 from `develop` at commit `0e9450c`.

This register comes from the module build files, the version catalog and the Android manifests. Vulnerability checks have not been run on these dependencies.

## Build configuration

| Item | Value |
|---|---|
| `compileSdk` (`:app`) | 37 |
| `minSdk` (`:app`) | 26 |
| `targetSdk` (`:app`) | 36 |
| Android Gradle Plugin | 9.3.3 |
| Kotlin | 2.2.10 |

## Dependencies (59)

| Library | Version | Scope | Modules |
|---|---|---|---|
| `androidx.activity:activity-compose` | 1.13.0 | implementation | :app, :wear |
| `androidx.appcompat:appcompat` | 1.8.0 | implementation | :app |
| `androidx.arch.core:core-testing` | 2.2.0 | testImplementation | :app |
| `androidx.compose.foundation:foundation` | 1.12.1 | implementation | :app |
| `androidx.compose.foundation:foundation-layout` | 1.12.1 | implementation | :app |
| `androidx.compose.material3:material3` | 1.4.0 | implementation | :app |
| `androidx.compose.material3:material3` | From BOM | implementation | :app |
| `androidx.compose.material:material-icons-extended` | 1.7.8 | implementation | :app |
| `androidx.compose.runtime:runtime` | 1.12.1 | implementation | :app |
| `androidx.compose.ui:ui` | 1.12.1 | implementation | :app |
| `androidx.compose.ui:ui` | From BOM | implementation | :app, :wear |
| `androidx.compose.ui:ui-graphics` | From BOM | implementation | :app |
| `androidx.compose.ui:ui-test-junit4` | From BOM | androidTestImplementation | :app |
| `androidx.compose.ui:ui-test-manifest` | From BOM | debugImplementation | :app |
| `androidx.compose.ui:ui-text` | 1.12.1 | implementation | :app |
| `androidx.compose.ui:ui-tooling` | From BOM | debugImplementation | :app, :wear |
| `androidx.compose.ui:ui-tooling-preview` | From BOM | implementation | :app, :wear |
| `androidx.compose:compose-bom` | 2026.09.00 | androidTestImplementation | :app |
| `androidx.compose:compose-bom` | 2026.09.00 | implementation | :app, :wear |
| `androidx.constraintlayout:constraintlayout-compose` | 1.1.2 | implementation | :app |
| `androidx.contentpager:contentpager` | 1.0.0 | implementation | :app |
| `androidx.core:core-ktx` | 1.19.1 | implementation | :app, :wear |
| `androidx.health.connect:connect-client` | 1.1.0 | implementation | :app |
| `androidx.hilt:hilt-navigation-compose` | 1.4.0 | implementation | :app |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | implementation | :app |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.11.0 | implementation | :app |
| `androidx.navigation:navigation-compose` | 2.10.2 | implementation | :app |
| `androidx.test.espresso:espresso-core` | 3.7.0 | androidTestImplementation | :app |
| `androidx.test.espresso:espresso-core` | 3.7.0 | implementation | :app |
| `androidx.test.ext:junit` | 1.3.0 | androidTestImplementation | :app |
| `androidx.tracing:tracing-perfetto-handshake` | 1.0.1 | implementation | :app |
| `androidx.wear.compose:compose-foundation` | 1.7.1 | implementation | :wear |
| `androidx.wear.compose:compose-material3` | 1.7.1 | implementation | :wear |
| `androidx.wear:wear-tooling-preview` | 1.0.0 | implementation | :wear |
| `androidx.work:work-runtime-ktx` | 2.10.1 | implementation | :app |
| `com.google.accompanist:accompanist-permissions` | 0.37.3 | implementation | :app |
| `com.google.android.gms:play-services-wearable` | 20.0.1 | implementation | :app, :wear |
| `com.google.code.gson:gson` | 2.13.1 | implementation | :core, :wear |
| `com.google.dagger:hilt-android` | 2.60.1 | implementation | :app |
| `com.google.firebase:firebase-analytics` | 23.2.0 | implementation | :app |
| `com.google.firebase:firebase-bom` | 34.19.0 | implementation | :app |
| `com.google.firebase:firebase-common-ktx` | 21.0.0 | implementation | :app |
| `com.google.firebase:firebase-config` | From BOM | implementation | :app |
| `com.google.firebase:firebase-crashlytics` | 20.1.1 | implementation | :app |
| `com.google.firebase:firebase-firestore` | From BOM | implementation | :app |
| `com.google.firebase:firebase-messaging-ktx` | 24.1.2 | implementation | :app |
| `com.squareup.okhttp3:logging-interceptor` | 5.5.0 | implementation | :app |
| `com.squareup.okhttp3:okhttp` | 5.5.0 | implementation | :app, :core, :wear |
| `com.squareup.retrofit2:converter-gson` | 3.0.0 | implementation | :app |
| `com.squareup.retrofit2:retrofit` | 3.0.0 | implementation | :app |
| `com.vanniktech:android-image-cropper` | 4.7.0 | implementation | :app |
| `io.coil-kt:coil-compose` | 2.7.0 | implementation | :app |
| `io.coil-kt:coil-gif` | 2.7.0 | implementation | :app |
| `io.livekit:livekit-android` | 2.29.1 | implementation | :app |
| `junit:junit` | 4.13.2 | testImplementation | :app, :core |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | implementation | :app, :core, :wear |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | testImplementation | :app |
| `org.mockito.kotlin:mockito-kotlin` | 6.4.0 | testImplementation | :app |
| `org.mockito:mockito-core` | 5.24.0 | testImplementation | :app |

## Android permissions (35)

The purpose of each permission must be confirmed by the developer who added it, so that column is left open.

| Permission | Modules | Purpose |
|---|---|---|
| `android.permission.ACCESS_NETWORK_STATE` | :app | To be filled |
| `android.permission.CAMERA` | :app | To be filled |
| `android.permission.FOREGROUND_SERVICE` | :app | To be filled |
| `android.permission.FOREGROUND_SERVICE_CAMERA` | :app | To be filled |
| `android.permission.FOREGROUND_SERVICE_MICROPHONE` | :app | To be filled |
| `android.permission.INTERNET` | :app, :wear | To be filled |
| `android.permission.POST_NOTIFICATIONS` | :app | To be filled |
| `android.permission.READ_EXTERNAL_STORAGE` | :app | To be filled |
| `android.permission.READ_MEDIA_IMAGES` | :app | To be filled |
| `android.permission.READ_MEDIA_VIDEO` | :app | To be filled |
| `android.permission.RECEIVE_BOOT_COMPLETED` | :app | To be filled |
| `android.permission.RECORD_AUDIO` | :app, :wear | To be filled |
| `android.permission.SCHEDULE_EXACT_ALARM` | :app | To be filled |
| `android.permission.SYSTEM_ALERT_WINDOW` | :app | To be filled |
| `android.permission.USE_FULL_SCREEN_INTENT` | :app | To be filled |
| `android.permission.VIBRATE` | :app, :wear | To be filled |
| `android.permission.WAKE_LOCK` | :app | To be filled |
| `android.permission.WRITE_EXTERNAL_STORAGE` | :app | To be filled |
| `android.permission.health.READ_ACTIVE_CALORIES_BURNED` | :app | To be filled |
| `android.permission.health.READ_BLOOD_GLUCOSE` | :app | To be filled |
| `android.permission.health.READ_BLOOD_PRESSURE` | :app | To be filled |
| `android.permission.health.READ_BODY_TEMPERATURE` | :app | To be filled |
| `android.permission.health.READ_DISTANCE` | :app | To be filled |
| `android.permission.health.READ_EXERCISE` | :app | To be filled |
| `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` | :app | To be filled |
| `android.permission.health.READ_HEART_RATE` | :app | To be filled |
| `android.permission.health.READ_HEART_RATE_VARIABILITY` | :app | To be filled |
| `android.permission.health.READ_HEIGHT` | :app | To be filled |
| `android.permission.health.READ_OXYGEN_SATURATION` | :app | To be filled |
| `android.permission.health.READ_RESPIRATORY_RATE` | :app | To be filled |
| `android.permission.health.READ_RESTING_HEART_RATE` | :app | To be filled |
| `android.permission.health.READ_SLEEP` | :app | To be filled |
| `android.permission.health.READ_STEPS` | :app | To be filled |
| `android.permission.health.READ_TOTAL_CALORIES_BURNED` | :app | To be filled |
| `android.permission.health.READ_WEIGHT` | :app | To be filled |
