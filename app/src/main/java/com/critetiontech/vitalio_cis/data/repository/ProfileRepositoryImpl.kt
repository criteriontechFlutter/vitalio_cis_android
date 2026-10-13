package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ProfileRepository
import com.critetiontech.ctvitalio.utils.Patient
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.critetiontech.ctvitalio.viewmodel.LocationItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProfileRepositoryImpl(
    private val context: Context,
    private val prefs: PrefsManager
) : ProfileRepository {

    private val endpoints = ApiEndPointCorporateModule()

    // ── Update patient profile (multipart PUT) ────────────────────────────
    override suspend fun updateProfile(
        patient: Patient,
        firstName: String,
        lastName: String,
        dob: String,
        mobileNo: String,
        emailId: String,
        address: String,
        zip: String,
        imageFile: File?
    ): DomainResult<Unit> = try {
        val parts = mutableListOf<MultipartBody.Part>().apply {
            fun add(name: String, value: String) =
                add(MultipartBody.Part.createFormData(name, value))

            add("FirstName", firstName)
            add("LastName", lastName.ifBlank { "-" })
            add("PatientName", "${firstName.trim()} ${lastName.trim()}".trim())
            add("Dob", dob)
            add("MobileNo", mobileNo)
            add("EmailID", emailId.ifBlank { "patient@vitalio.care" })
            add("Address", address.ifBlank { "N/A" })
            add("Zip", zip)
            add("Pid", patient.pid.toString())
            add("ClientId", patient.clientId.toString())
            add("clientId", patient.clientId.toString())
            add("UserId", patient.pid.toString())
            add("GenderId", patient.genderId.toString())
            add("BloodGroupId", patient.bloodGroupId.toString())
            add("CityId", patient.cityId.toString())
            add("StateId", patient.stateId.toString())
            add("CountryId", patient.countryId.toString())
            // Age from existing patient data
            val ageYears = Regex("^(\\d+)").find(patient.age.trim())?.value ?: "0"
            add("Age", ageYears)
            add("AgeUnitId", "1")
            add("ChoronicDiseasesJson", "[]")

            if (imageFile != null && imageFile.exists()) {
                add(
                    MultipartBody.Part.createFormData(
                        "FormFile",
                        imageFile.name,
                        imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                )
            }
        }

        val response = ApiClients.module4082.dynamicMultipartPut(
            url = endpoints.updatePatientProfile,
            headers = emptyMap(),
            parts = parts
        )

        if (response.isSuccessful) {
            // Parse new profile URL from response if present
            val bodyStr = response.body()?.string() ?: ""
            var newImageUrl = patient.imageUrl
            try {
                val json = JSONObject(bodyStr)
                val rv = json.opt("responseValue")
                when {
                    rv is JSONArray && rv.length() > 0 -> {
                        val first = rv.getJSONObject(0)
                        val url = first.optString("profileUrl").takeIf { it.isNotBlank() }
                            ?: first.optString("profileURL").takeIf { it.isNotBlank() }
                        if (!url.isNullOrBlank()) newImageUrl = ensureFullUrl(url)
                    }
                    rv is JSONObject -> {
                        val url = rv.optString("profileUrl").takeIf { it.isNotBlank() }
                            ?: rv.optString("profileURL").takeIf { it.isNotBlank() }
                        if (!url.isNullOrBlank()) newImageUrl = ensureFullUrl(url)
                    }
                }
            } catch (_: Exception) { }

            prefs.savePatient(
                patient.copy(
                    firstName = firstName,
                    lastName = lastName,
                    dob = dob,
                    mobileNo = mobileNo,
                    emailAddress = emailId.ifBlank { patient.emailAddress },
                    address = address,
                    zip = zip,
                    imageUrl = newImageUrl
                )
            )
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(Exception("Update failed (${response.code()})"))
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    // ── Location: Countries ────────────────────────────────────────────────
    // iOS loads countries from a local JSON bundle file (country_json.json).
    // Android version calls the same pattern: we provide a hardcoded list of
    // major countries (India always first) to mirror the iOS behaviour.
    // If you want to make it API-driven you can swap this for a network call.
    override suspend fun getCountries(): List<LocationItem> {
        return listOf(
            LocationItem(101, "India"),
            LocationItem(231, "United States"),
            LocationItem(102, "Afghanistan"),
            LocationItem(103, "Albania"),
            LocationItem(104, "Algeria"),
            LocationItem(105, "Andorra"),
            LocationItem(106, "Angola"),
            LocationItem(107, "Argentina"),
            LocationItem(108, "Armenia"),
            LocationItem(109, "Australia"),
            LocationItem(110, "Austria"),
            LocationItem(111, "Azerbaijan"),
            LocationItem(112, "Bahamas"),
            LocationItem(113, "Bahrain"),
            LocationItem(114, "Bangladesh"),
            LocationItem(115, "Belarus"),
            LocationItem(116, "Belgium"),
            LocationItem(117, "Belize"),
            LocationItem(118, "Bhutan"),
            LocationItem(119, "Bolivia"),
            LocationItem(120, "Bosnia"),
            LocationItem(121, "Brazil"),
            LocationItem(122, "Canada"),
            LocationItem(123, "China"),
            LocationItem(124, "France"),
            LocationItem(125, "Germany"),
            LocationItem(126, "Japan"),
            LocationItem(127, "Nepal"),
            LocationItem(128, "Pakistan"),
            LocationItem(129, "Russia"),
            LocationItem(130, "Sri Lanka"),
            LocationItem(131, "United Kingdom")
        )
    }

    // ── Location: States (API call to port 4119) ──────────────────────────
    override suspend fun getStates(countryId: Int): List<LocationItem> = try {
        val response = ApiClients.module4119.dynamicGet(
            url = "https://vitaliocis.vitalio.care:4119/api/StateMaster/GetStateMasterByCountryId",
            headers = emptyMap(),
            params = mapOf("id" to countryId.toString(), "clientId" to "176")
        )
        if (response.isSuccessful) {
            val bodyStr = response.body()?.string() ?: return emptyList()
            val json = JSONObject(bodyStr)
            val rv = json.optJSONArray("responseValue") ?: return emptyList()
            (0 until rv.length()).map { i ->
                val obj = rv.getJSONObject(i)
                LocationItem(
                    id = obj.optInt("id"),
                    name = obj.optString("stateName")
                )
            }
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    // ── Location: Cities (API call to port 4119) ──────────────────────────
    override suspend fun getCities(stateId: Int): List<LocationItem> = try {
        val response = ApiClients.module4119.dynamicGet(
            url = "https://vitaliocis.vitalio.care:4119/api/CityMaster/GetCityMasterByStateId",
            headers = emptyMap(),
            params = mapOf("id" to stateId.toString(), "clientId" to "176")
        )
        if (response.isSuccessful) {
            val bodyStr = response.body()?.string() ?: return emptyList()
            val json = JSONObject(bodyStr)
            val rv = json.optJSONArray("responseValue") ?: return emptyList()
            (0 until rv.length()).map { i ->
                val obj = rv.getJSONObject(i)
                LocationItem(
                    id = obj.optInt("id"),
                    name = obj.optString("name")
                )
            }
        } else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private fun ensureFullUrl(path: String): String {
        if (path.startsWith("http")) return path
        val clean = path.trimStart('/')
        return "https://vitaliocis.vitalio.care:4082/$clean"
    }
}
