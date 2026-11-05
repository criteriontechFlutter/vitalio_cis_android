# Security and Privacy Review

This is a code-level review of how the mobile app handles health data, based on commit `0e9450c`. **It is not a penetration test or a legal compliance review.** The backend and Firebase project were not reviewed.

## Controls found in the code

| Control | Evidence |
|---|---|
| Android backup is off | `android:allowBackup="false"` in the manifest |
| Patient data is excluded from cloud backup and device transfer | `res/xml/data_extraction_rules.xml` |
| Plain HTTP is blocked by default | `res/xml/network_security_config.xml` |
| Network body logging is limited to debug builds | `RetrofitInstance.kt:63` |
| `Log.v`, `Log.d`, `Log.i` and `println` are removed from release builds | `app/proguard-rules.pro` |
| API keys and signing details are kept out of git | `local.properties`, ignored by `.gitignore` |
| No keystore file has been committed | No `.jks` or `.keystore` file in git history |
| Release builds use production and cannot switch servers by default | `ApiConfig.canSwitch`, `app/build.gradle.kts` |

## Findings

Severity is the reviewer's initial assessment and must be confirmed by the team. Tracking is in [10-issues-and-risks.md](10-issues-and-risks.md).

| ID | Finding | Evidence | Severity |
|---|---|---|---|
| SEC-1 | Some services use plain HTTP, including the service that reads lab reports and prescriptions | `ApiConfig.kt` and the allowlist in `network_security_config.xml` | High |
| SEC-2 | Patient details and cached API responses are stored without app-level encryption | `PrefsManager.kt:109` uses standard SharedPreferences, and encrypted storage is not used anywhere | Medium |
| SEC-3 | `Log.e` and `Log.w` are not removed from release builds, and some include exception details | `app/proguard-rules.pro`. There are 192 log calls under `:app`. They have not all been reviewed for sensitive data. | Medium |
| SEC-4 | The NutriAnalyser secret is built into the app, where it can be extracted from the APK | `NUTRI_APP_SECRET` in `app/build.gradle.kts` | Medium |
| SEC-5 | An admin's phone number is hard-coded in code, rules and documentation | `config/FeatureFlags.kt`, `firebase/firestore.rules`, `firebase/README.md` | Medium |
| SEC-6 | A release build can fall back to the debug signing key | `app/build.gradle.kts` | Medium |
| SEC-7 | Certificate pinning is not used | No use of `CertificatePinner` in the code | Low: decide whether it is needed |

## Sensitive permissions

The app requests health data, camera, microphone, storage, overlay and background permissions. The full list is in [08-dependency-permission-register.md](08-dependency-permission-register.md). The purpose of each one still has to be recorded.

## Not reviewed

- Backend API security, including authentication and whether users can only reach their own data
- How long data is kept, and whether it is deleted fully at logout or account deletion
- Consent wording and privacy policy, including Play Store and Health Connect declarations
- Applicable regulations for health data
- Firebase security rules, beyond noting the admin number
