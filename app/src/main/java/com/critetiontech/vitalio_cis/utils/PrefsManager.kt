package com.critetiontech.ctvitalio.utils
import android.content.Context
import android.util.Log
import androidx.core.content.edit
 import com.google.gson.Gson


data class PatientResponse(
    val status: Int,
    val message: String,
    val responseValue: List<Patient>
)

data class VerifyOtpPatient(
    val pid: Int = 0,
    val pmId: Int = 0,
    val uhId: String = "",
    val firstName: String = "",
    val lastName: String? = null,
    val profileURL: String? = null,
    val mobileNo: String = "",
    val emailId: String = "",
    val address: String = "",
    val loginDateTime: String = "",
    val age: String = "",
    val ageType: String = "",
    val dob: String = "",
    val genderId: Int = 0,
    val cityId: Int = 0,
    val stateId: Int = 0,
    val clientId: Int = 0,
    val height: String = "",
    val weight: String = "",
    val bloodGroupId: Int = 0,
    val joinedOn: String = "",
    val lastVisitDate: String? = null,
    val patientStatus: String? = null,
    val crNo: String? = null      // CR Number — used by PatientOPDAllHistory API
)

data class VerifyOtpResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: VerifyOtpPatient? = null
)

data class Patient(
    val pid: Int,
    val uhId: String,
    val firstName: String,
    val lastName: String?,
    val dob: String,
    val genderId: Int,
    val countryCallingCode: String,
    val mobileNo: String,
    val emailAddress: String,
    val age: String,
    val guardianRelationId: Int,
    val guardianName: String,
    val countryId: Int,
    val stateId: Int,
    val cityId: Int,
    val address: String,
    val imageUrl: String,
    val bloodGroupId: Int,
    val zip: String,
    val isActive: Boolean,
    val clientId: Int,
    val departmentName: String?,
    val cityName: String,
    val stateName: String,
    val countryName: String,
    val genderName: String,
    val guardianRelationName: String?,
    val bloodGroupName: String?,
    val crNo: String? = null,  // CR Number — required by PatientOPDAllHistory API
    // Nullable: patients saved before these existed come back from Gson with null
    val height: String? = null, // as the login API sends it, e.g. "176.00/cm"
    val weight: String? = null  // e.g. "76.00/kg"
)

fun genderNameFor(id: Int): String = when (id) {
    1 -> "Male"
    2 -> "Female"
    else -> "Other"
}

fun bloodGroupNameFor(id: Int): String? = when (id) {
    1 -> "A+"; 2 -> "A-"; 3 -> "B+"; 4 -> "B-"
    5 -> "O+"; 6 -> "O-"; 7 -> "AB+"; 8 -> "AB-"
    else -> null
}

/** The server sends photo paths with backslashes ("…/Upload\\Image\\x.jpg") and sometimes without a host. */
fun profileImageUrl(raw: String?): String {
    val path = raw?.trim()?.replace('\\', '/').orEmpty()
    if (path.isBlank()) return ""
    if (path.startsWith("http")) return path
    return com.critetiontech.ctvitalio.config.ApiConfig.MODULE_4082 + path.trimStart('/')
}
data class EmployeeGoal(
    val pid: Int,
    val vmId: Int,
    val vitalName: String,
    val targetValue: Int,
    val unit: String
)
class PrefsManager(context: Context) {
    private val sharedPref = context.getSharedPreferences("patient_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PATIENT = "current_patient"
        private const val KEY_Allergies = "allergies_length"
        private const val KEY_Emergency = "Emergency_length"
        private const val KEY_SmartWatch = "SmartWatch_length"
        private const val KEY_DEVICE_TOKEN = "deviceToken"
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
    }
    fun saveTheme(isDark: Boolean) {
        sharedPref.edit().putBoolean(KEY_IS_DARK_THEME, isDark).apply()
    }


    fun saveString(context: Context, key: String, value: String) {
        sharedPref.edit { putString(key, value) }
    }

    fun getString(context: Context, key: String): String? {
        return sharedPref.getString(key, null)
    }

    fun getTheme(): Boolean {
        return sharedPref.getBoolean(KEY_IS_DARK_THEME, false) // default LIGHT
    }
    // Save patient object
    fun savePatient(patient: Patient) {
        sharedPref.edit {
            putString(KEY_PATIENT, gson.toJson(patient))
        }
    }



    fun saveSmartWatch(smartWatchLength: String) {
        sharedPref.edit {
            putString(KEY_SmartWatch,smartWatchLength)
        }
    }


    fun saveDeviceToken(deviceToken: String) {
        sharedPref.edit {
            putString(KEY_DEVICE_TOKEN,deviceToken)
        }
    }

    fun getDeviceToken(): String? {
        return sharedPref.getString(KEY_DEVICE_TOKEN, null)

    }

    // Retrieve patient with null safety
    fun getPatient(): Patient? {
        return try {
            gson.fromJson(
                sharedPref.getString(KEY_PATIENT, null),
                Patient::class.java
            )
        } catch (e: Exception) {
            null
        }
    }
    fun getAllergies(): String? {
        return try {

            return sharedPref.getString(KEY_Allergies, "")
        } catch (e: Exception) {
            ""
        }
    }

    fun getEmergency(): String? {
        return try {

            return sharedPref.getString(KEY_Emergency, "")
        } catch (e: Exception) {
            ""
        }
    }
    fun getSmartWatch(): String? {
        return try {

            return sharedPref.getString(KEY_SmartWatch, "")
        } catch (e: Exception) {
            ""
        }
    }

    // Clear patient data
    fun clearPatient() {
        sharedPref.edit { remove(KEY_PATIENT) }
        sharedPref.edit { remove(KEY_Allergies) }
        sharedPref.edit { remove(KEY_Emergency) }
        sharedPref.edit { remove(KEY_SmartWatch) }
        sharedPref.edit { remove(KEY_DEVICE_TOKEN) }
    }

    // Optional: Direct property access

    suspend fun getData(
        key: String,
        shouldSave: Boolean = true,
        apiCall: suspend () -> String?
    ): String? = try {
        val apiResult = apiCall()

        if (!apiResult.isNullOrEmpty()) {
            if (shouldSave) {
                save(key, apiResult)
            }

            apiResult  // ✅ Directly return API result
        } else {
            val localData = get(key, String::class.java)
            Log.d("CACHE", "API empty, fallback to cache for $key")
            localData
        }
    } catch (e: Exception) {
        val localData = get(key, String::class.java)
        Log.d("CACHE", "API failed, fallback to cache for $key: ${e.message}")
        localData
    }

//    suspend fun <T> getData(
//        key: String,
//        clazz: Class<T>,
//        shouldSave: Boolean = true, // 🔥 NEW FLAG
//        apiCall: suspend () -> T?
//    ): T? {
//        return withContext(Dispatchers.IO) {
//
//            try {
//                // 🔥 1️⃣ API CALL
//                val apiResult = apiCall()
//
//                if (apiResult != null) {
//
//                    // 🔥 2️⃣ SAVE ONLY IF ALLOWED
//                    if (shouldSave) {
//                        save(key, apiResult)
//                        val localData = get(key, clazz)
//                        Log.d("CACHE", "Saved tonnnnn local"+localData.toString())
//                    } else {
//                        Log.d("CACHE", "Save skipped (shouldSave = false)")
//                    }
//
//                    // 🔥 3️⃣ RETURN DIRECT RESULT (no need to re-fetch)
//                    return@withContext apiResult
//                }
//
//            } catch (e: Exception) {
//                Log.e("CACHE", "API Failed: ${e.message}")
//            }
//
//            // 🔥 4️⃣ FALLBACK TO LOCAL
//            val localData = get(key, clazz)
//
//            if (localData != null) {
//                Log.d("CACHE", "Fetched from local: ${Gson().toJson(localData)}")
//            } else {
//                Log.d("CACHE", "No local data found")
//            }
//
//            return@withContext localData
//        }
//    }

    // Save any object to SharedPreferences as JSON
    private fun <T> save(key: String, obj: T) {
        val json = gson.toJson(obj)
        sharedPref.edit().putString(key, json).apply()
    }

    // Get object from SharedPreferences
    fun <T> get(key: String, clazz: Class<T>): T? {
        val json = sharedPref.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            null
        }
    }

    // Optional: clear key
    fun clearKey(key: String) {
        sharedPref.edit().remove(key).apply()
    }

    // Optional: clear all
    fun clearAll() {
        sharedPref.edit().clear().apply()
    }
}