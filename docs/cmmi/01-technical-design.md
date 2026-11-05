# Technical Design

Based on the code at commit `0e9450c` and on [ARCHITECTURE.md](../../ARCHITECTURE.md), which has the step-by-step guide for adding features.

## System overview

Vitalio CIS is a patient health app for Android phones, with a companion app for Wear OS watches. Patients track vitals, fluids, symptoms, medicines and diet, book and attend consultations, manage reports and prescriptions, and connect health devices.

## Modules

| Module | Type | Purpose |
|---|---|---|
| `:app` | Android application | Phone app, with 40 ViewModels and the screens in `ui/screens` |
| `:wear` | Android application | Wear OS app, including voice screens and patient session sync |
| `:core` | Android library | UI-free code shared by `:app` and `:wear`, such as voice processing |

## Application architecture

The app has three layers. Screens never call the network directly.

```text
UI (Compose screens)         ui/screens
    ↓
ViewModel                    viewmodel
    ↓
UseCase                      domain/usecase
    ↓
Repository interface         domain/repository
    ↑ implemented by
Repository implementation    data/repository
    ↓
API clients (Retrofit)       data/remote/network
    ↓
Backend services
```

- **Results:** repository methods return `DomainResult`, which is either `Success` or `Error`.
- **Wiring:** `di/AppDependencies.kt` is a manual service locator that creates repositories and use cases.
- **Navigation:** routes are in `Routes.kt`, and the navigation host is in `MainActivity.kt`.
- **Offline data:** `PrefsManager` saves selected API responses in SharedPreferences. It returns the saved copy when the device is offline or the API call fails.

## Feature packages

| Package | Purpose |
|---|---|
| `config` | Server addresses, environments and feature flags |
| `consultation` | Video call activity, service and notifications |
| `healthconnect` | Reading Health Connect data and background sync |
| `omron` | Omron device pairing and readings |
| `yonker` | Yonker BP machine and oximeter over BLE |
| `ring` | Smart ring integration |
| `watch` | Syncing the patient session to the Wear OS app |
| `voice` | Voice assistant on the phone |
| `notification` | Firebase push notifications |
| `reminder` | Medicine reminders |
| `permissions` | Runtime permission handling |

## External integrations

| Integration | Used for |
|---|---|
| Vitalio backend services | Patient, vitals, doctor, lab, fluid and master data ([03-api-inventory.md](03-api-inventory.md)) |
| Firebase | Push notifications, Crashlytics, and Remote Config with Firestore for feature flags |
| Health Connect | Reading health data from the device |
| Omron and Yonker devices | Reading blood pressure and other vitals over Bluetooth |
| Smart ring services | Ring tokens and vitals |
| Wear OS Data Layer | Sending the logged-in patient session to the watch |
| LiveKit | Video consultation |
| NutriAnalyser | Nutrition analysis |
| AI upload service | Reading lab reports and prescriptions |

## Build configuration

| Item | Value |
|---|---|
| Build types | `debug` and `release` |
| Release build | Uses code and resource shrinking, and the production environment |
| App versions | `version.properties` at the repository root |
| Local settings | `local.properties`, which is not committed: API keys, signing details, `API_ENV` and `ENV_SWITCHER` |

## Feature flags

Modules can be switched on or off for all users through Remote Config. An admin page writes to Firestore, and a Cloud Function publishes the change. If a key is missing or a fetch fails, the module stays on. See [firebase/README.md](../../firebase/README.md).

## Design decisions on record

| Decision | Source | Approved by |
|---|---|---|
| Manual service locator, because of a Hilt annotation processing conflict | `ARCHITECTURE.md` | To be filled |
| Device and SDK code stays in its own package, and server calls go through use cases | `ARCHITECTURE.md` | To be filled |
| No patient data in device or cloud backup | `data_extraction_rules.xml` | To be filled |
| Diet data comes from the backend only and is not stored on the device | Developer instruction | To be filled |

Formal decision records with the options compared do not exist in the repository.

## Known differences from ARCHITECTURE.md

See ISSUE-10 in [10-issues-and-risks.md](10-issues-and-risks.md).
