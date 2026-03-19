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

data class BaseResponse<T>(
    val status: Int,
    val message: String,
    val responseValue: T
)
// Data class for patient (include all fields from your JSON)
data class Patient(
    val pid: String,
     @SerializedName("empName")
     val patientName: String,
    val registrationDate: String,
     val ageUnitId: String,
     val categoryId: String?,
     val createdDate: String,
     val educationalQualificationId: String?,
     val ethinicityId: String?,
    val gender: String,
     val guardianAddress: String,
    val guardianMobileNo: String,
    val guardianName: String,
    val guardianRelationId: String?,
    val height: String,
     val idNumber: String,
    val idTypeId: String,
    val imageURL: String,
    val languageId: String,
    val maritalStatusId: String,
     val occupationId: String?,
    val raceTypeId: String?,
    val refferedFrom: String?,
    val sexualOrientation: String,
     val status: String,
//    @SerializedName("empId")
    val uhID: String,
    val userId: String,
    val weight: String,
     val departmentId: String,
    val doctorID: String,
    val patientGender: String,
    val departmentName: String,
     val isCashLess: Boolean,
    val insuranceCompanyId: Int,
    val policyOrCardNumber: String,
    val profileUrl: String,
    var isHoldToSpeak: Int = 0,
    val id: Int,
    val empId: String,
    val mobileNo: String,
    val genderId: Int,
    val age: String,
    val ageType: String,
    val address: String,
    val dob: String,
    val joiningDate: String,
    val countryCallingCode: String,
    val countryId: Int,
    val stateId: Int,
    val zip: String,
    val cityId: Int,
    val emailID: String,
    val bloodGroupId: Int,
    val clientId: Int,
    val isFirstLoginCompleted: Int,



    val employeegoalsDetails: String
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
    val currentPatientName: String?
        get() = getPatient()?.patientName

    val currentPatientUHID: String?
        get() = getPatient()?.uhID



    suspend fun <T> getData(
        key: String,
        clazz: Class<T>,
        apiCall: suspend () -> T?
    ): T? {
        return withContext(Dispatchers.IO) {

            try {
                // 🔥 1️⃣ API CALL
                val apiResult = apiCall()

                Log.d("CACHE", "Saved to local")
                if (apiResult != null) {

                    // 🔥 2️⃣ SAVE DATA
                    save(key, apiResult)
                    Log.d("CACHE", "Saved to local")

                    // 🔥 3️⃣ IMMEDIATELY GET FROM LOCAL (VERIFY)
                    val savedData = get(key, clazz)

                    Log.d("CACHE", "After Save → Fetched from local: ${Gson().toJson(savedData)}")

                    return@withContext savedData
                }

            } catch (e: Exception) {
                Log.e("CACHE", "API Failed: ${e.message}")
            }

            // 🔥 4️⃣ FALLBACK TO LOCAL
            val localData = get(key, clazz)

            if (localData != null) {
                Log.d("CACHE", "Fallback → Fetched from local: ${Gson().toJson(localData)}")
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