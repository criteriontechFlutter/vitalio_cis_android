package com.example.vitalio_cis.utils
import android.content.Context
import android.icu.text.DisplayOptions.DisplayLength
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
 import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


data class PatientResponse(
    val status: Int,
    val message: String,
    val responseValue: List<Patient>
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
    val bloodGroupName: String?
)
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
        Log.d("TAG", "saveDeviceToken: "+deviceToken.toString())
        sharedPref.edit {
            putString(KEY_DEVICE_TOKEN,deviceToken)
        }
    }

    fun getDeviceToken(): String? {
        Log.d("TAG", "saveDeviceToken: "+sharedPref.getString(KEY_DEVICE_TOKEN, null))
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



    suspend fun <T> getData(
        key: String,
        clazz: Class<T>,
        shouldSave: Boolean = true, // 🔥 NEW FLAG
        apiCall: suspend () -> T?
    ): T? {
        return withContext(Dispatchers.IO) {

            try {
                // 🔥 1️⃣ API CALL
                val apiResult = apiCall()

                if (apiResult != null) {

                    // 🔥 2️⃣ SAVE ONLY IF ALLOWED
                    if (shouldSave) {
                        save(key, apiResult)
                        Log.d("CACHE", "Saved to local")
                    } else {
                        Log.d("CACHE", "Save skipped (shouldSave = false)")
                    }

                    // 🔥 3️⃣ RETURN DIRECT RESULT (no need to re-fetch)
                    return@withContext apiResult
                }

            } catch (e: Exception) {
                Log.e("CACHE", "API Failed: ${e.message}")
            }

            // 🔥 4️⃣ FALLBACK TO LOCAL
            val localData = get(key, clazz)

            if (localData != null) {
                Log.d("CACHE", "Fetched from local: ${Gson().toJson(localData)}")
            } else {
                Log.d("CACHE", "No local data found")
            }

            return@withContext localData
        }
    }

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