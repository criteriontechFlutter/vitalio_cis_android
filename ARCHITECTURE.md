# Vitalio CIS — Clean Architecture Guide

## How the Architecture Works

```
UI (Screens)
    ↓ calls
ViewModel
    ↓ calls
UseCase  (domain layer)
    ↓ calls
Repository Interface  (domain layer)
    ↑ implemented by
RepositoryImpl  (data layer)
    ↓ calls
ApiHelper / ApiClients  (network layer)
    ↓ HTTP
Remote API
```

---

## The 3 Layers

### 1. Domain Layer  (`domain/`)
Pure business logic. No Android imports. No API knowledge.

| Folder | What lives here |
|---|---|
| `domain/model/` | `DomainResult.kt` — a `sealed class Success / Error` wrapper |
| `domain/repository/` | **Interfaces** describing what data operations exist |
| `domain/usecase/` | **Use cases** — one action per class, delegates to a repository |

### 2. Data Layer  (`data/`)
Knows how to fetch data. Talks to the network. Maps responses.

| Folder | What lives here |
|---|---|
| `data/repository/` | Concrete implementations of the domain repository interfaces |

Each `*RepositoryImpl` gets `Context + PrefsManager` and calls `ApiHelper` + `ApiClients` internally.

### 3. Presentation Layer  (`viewmodel/` + `ui/`)
Knows what to show. Never talks to the network directly.

| Folder | What lives here |
|---|---|
| `viewmodel/` | `ViewModel` classes — hold UI state, call use cases |
| `ui/screens/` | Compose screens — observe ViewModel state, call ViewModel methods |

---

## Wiring (AppDependencies)

Because Hilt annotation processing is blocked by a javapoet version conflict with AGP 8.11.0 + Kotlin 2.1.0, we use a manual service locator instead:

```
di/AppDependencies.kt
```

This object:
- Holds singleton instances of `PrefsManager` and all `*RepositoryImpl` classes
- Provides factory functions for every use case

ViewModels call it like:
```kotlin
private val sendOtpUseCase = AppDependencies.sendOtp()
private val prefs           = AppDependencies.prefs
```

When Hilt annotation processing becomes compatible, `AppDependencies` will be replaced by `@HiltViewModel @Inject constructor(...)`.

---

## Data Flow — Concrete Example (Login OTP)

```
LoginView.kt
  └─ viewModel.sendOTP()
        │
        ▼
LoginViewModel.kt
  private val sendOtpUseCase = AppDependencies.sendOtp()
  sendOtpUseCase(mobile)
        │
        ▼
SendOtpUseCase.kt  (domain/usecase/auth/)
  repository.sendOtp(mobile)
        │
        ▼
AuthRepository  (interface, domain/repository/)
        │ implemented by
        ▼
AuthRepositoryImpl.kt  (data/repository/)
  ApiHelper().callApi(context, endpoints.corporateEmployeeLogin) { url ->
      ApiClients.module4082.queryDynamicRawPost(url, params)
  }
        │
        ▼
Remote API → response parsed → DomainResult.Success(isRegistered)
        │
        ▲ back up the chain
LoginViewModel
  _navigationEvent.emit("otp/$mobile")   ← UI listens and navigates
```

---

## DomainResult

Every repository method returns `DomainResult<T>`:

```kotlin
sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()
    data class Error(val exception: Exception) : DomainResult<Nothing>()
}
```

ViewModels handle it with `when`:

```kotlin
when (val result = useCase(params)) {
    is DomainResult.Success -> _stateFlow.value = result.data
    is DomainResult.Error   -> Log.e(TAG, result.exception.message.orEmpty())
}
```

---

## How to Add a New Page with an API

Follow these 5 steps every time.

---

### Step 1 — Add the API endpoint constant

File: `utils/ApiEndPointCorporateModule.kt`

```kotlin
class ApiEndPointCorporateModule {
    // ... existing endpoints ...
    val fetchPrescriptions = "api/Prescription/FetchPrescriptions"   // ← add yours
}
```

---

### Step 2 — Add the model (DTO)

Create a new file in `model/`:

**`model/PrescriptionApiResponse.kt`**
```kotlin
data class PrescriptionApiResponse(
    val status: Int,
    val message: String,
    val responseValue: List<Prescription>
)

data class Prescription(
    val id: Int,
    val medicineName: String,
    val dosage: String,
    val prescribedDate: String
)
```

---

### Step 3 — Add the Repository Interface + Use Case

**`domain/repository/PrescriptionRepository.kt`**
```kotlin
interface PrescriptionRepository {
    suspend fun fetchPrescriptions(uhid: String, clientId: String): DomainResult<List<Prescription>>
}
```

**`domain/usecase/prescription/FetchPrescriptionsUseCase.kt`**
```kotlin
class FetchPrescriptionsUseCase(private val repository: PrescriptionRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<Prescription>> =
        repository.fetchPrescriptions(uhid, clientId)
}
```

---

### Step 4 — Add the Repository Implementation + wire it in AppDependencies

**`data/repository/PrescriptionRepositoryImpl.kt`**
```kotlin
class PrescriptionRepositoryImpl(
    private val context: Context,
    private val prefs: PrefsManager
) : PrescriptionRepository {

    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchPrescriptions(uhid: String, clientId: String): DomainResult<List<Prescription>> = try {
        val params = mapOf("uhid" to uhid, "clientId" to clientId)

        val json = prefs.getData(key = endpoints.fetchPrescriptions, shouldSave = true) {
            val response = ApiHelper().callApi(
                context,
                endpoints.fetchPrescriptions,
                showNoConnectionDialog = false
            ) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string()
            else throw Exception("API Error: ${response.code()}")
        }

        val list = if (!json.isNullOrEmpty()) {
            Gson().fromJson(json, PrescriptionApiResponse::class.java).responseValue
        } else emptyList()

        DomainResult.Success(list)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }
}
```

**`di/AppDependencies.kt`** — add the new repository and use case factory:
```kotlin
object AppDependencies {
    // ... existing ...

    // Add this:
    val prescriptionRepository by lazy {
        PrescriptionRepositoryImpl(MyApplication.appContext, prefs)
    }

    fun fetchPrescriptions() = FetchPrescriptionsUseCase(prescriptionRepository)
}
```

---

### Step 5 — Create the ViewModel and Screen

**`viewmodel/PrescriptionViewModel.kt`**
```kotlin
class PrescriptionViewModel : ViewModel() {

    private val fetchPrescriptionsUseCase = AppDependencies.fetchPrescriptions()
    private val prefs = AppDependencies.prefs

    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())
    val prescriptions: StateFlow<List<Prescription>> = _prescriptions

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadPrescriptions() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val result = fetchPrescriptionsUseCase(patient.uhId, patient.clientId.toString())) {
                is DomainResult.Success -> _prescriptions.value = result.data
                is DomainResult.Error   -> Log.e("PrescriptionVM", result.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }
}
```

**`ui/screens/PrescriptionView.kt`**
```kotlin
@Composable
fun PrescriptionScreen(viewModel: PrescriptionViewModel = viewModel()) {

    val prescriptions by viewModel.prescriptions.collectAsState()
    val loading       by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPrescriptions()
    }

    CommonAppBar(title = "Prescriptions") {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(prescriptions) { item ->
                    PrescriptionCard(item)
                }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(item: Prescription) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.medicineName, fontWeight = FontWeight.Bold)
            Text(item.dosage, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
```

**Add route + navigation in `MainActivity.kt`:**
```kotlin
// In Routes.kt — add:
const val PRESCRIPTIONS = "prescriptions"

// In MainActivity.kt NavHost — add:
composable(Routes.PRESCRIPTIONS) { PrescriptionScreen() }
```

---

## Quick Reference — What to Create for Each New Feature

| What you need | Where to create it |
|---|---|
| API endpoint constant | `utils/ApiEndPointCorporateModule.kt` |
| Response data class | `model/YourFeatureResponse.kt` |
| Repository interface | `domain/repository/YourRepository.kt` |
| Use case(s) | `domain/usecase/yourfeature/YourUseCase.kt` |
| Repository implementation | `data/repository/YourRepositoryImpl.kt` |
| Wire into DI | `di/AppDependencies.kt` (add `val repo` + `fun useCase()`) |
| ViewModel | `viewmodel/YourViewModel.kt` |
| Screen | `ui/screens/YourView.kt` |
| Route + nav entry | `Routes.kt` + `MainActivity.kt` |

---

## Which API Client to Use

| Base URL | Client | Used for |
|---|---|---|
| `vitaliocis.vitalio.care:4082` | `ApiClients.module4082` | Most patient APIs (vitals, auth, allergies, doctors) |
| `vitaliocis.vitalio.care:5084` | `ApiClients.module4084` | Doctor profile APIs |
| `vitaliocis.vitalio.care:4096` | `ApiClients.module4094` | Fluid intake APIs |
| `vitaliocis.vitalio.care:44374` | `ApiClients.module44374` | — |
| `52.172.134.222:205/api/v1.0/` | `ApiClients.digidoctor_BaseURL` | Symptom/problem icon APIs |

---

## POST vs GET

```kotlin
// GET with query params:
ApiClients.module4082.dynamicGet(url = url, params = mapOf("uhid" to "X"))

// POST with JSON body:
ApiClients.module4082.dynamicRawPost(url = url, body = mapOf("key" to "value"))

// POST with query params only (no body):
ApiClients.module4082.queryDynamicRawPost(url = url, params = mapOf("key" to "value"))

// Multipart file upload:
ApiClients.module4082.dynamicMultipartPost(url = url, parts = listOf(filePart))
```

---

## Caching

`prefs.getData(key, shouldSave) { /* api call */ }` handles caching automatically:
- **Online**: calls the API, saves response to SharedPreferences if `shouldSave = true`
- **Offline**: returns cached response from SharedPreferences
- **API failure**: falls back to cached response

Use `shouldSave = true` for read-heavy screens (vitals, allergies, doctors).
Use `shouldSave = false` for write-then-read flows (OTP verification, adding data).
