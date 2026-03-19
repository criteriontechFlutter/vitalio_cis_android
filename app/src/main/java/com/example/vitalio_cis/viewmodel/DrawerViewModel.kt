package com.critetiontech.ctvitalio.viewmodel

 import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.utils.ToastUtils
 import com.example.vitalio_cis.utils.PrefsManager
 import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class DrawerViewModel  : ViewModel() {

    var loading = mutableStateOf(false)
        private set

    var updateSuccess = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    var selectedImageUri = mutableStateOf<Uri?>(null)
        private set

    fun setSelectedImage(uri: Uri?) {
        selectedImageUri.value = uri
    }

    fun updateUserData(context: Context, filePath: Uri? = null) {
        loading.value = true
        updateSuccess.value = false

        viewModelScope.launch {
            try {
                val patient = PrefsManager(context).getPatient() ?: return@launch

                // Build multipart parts (same as your original function)
                val parts = mutableListOf<MultipartBody.Part>()
                fun partFromField(key: String, value: String) =
                    MultipartBody.Part.createFormData(key, value)

                parts += partFromField("Pid", patient.id.toString())
                parts += partFromField("PatientName", patient.patientName)
                // Add other fields ...

                // File attachment
                filePath?.let {
                    it.path?.takeIf { it.isNotEmpty() }?.let { path ->
                        val file = File(path)
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val filePart =
                            MultipartBody.Part.createFormData(
                                "FormFile",
                                PrefsManager(context).getPatient()?.profileUrl,
                                requestFile
                            )
                        parts += filePart
                    }
                }

                // API Call
                val response = RetrofitInstance
                    .createApiService(includeAuthHeader = true)
//                    .dynamicMultipartPut(
//                        url = ApiEndPoint().updatePatient,
//                        parts = parts
//                    )

//                if (response.isSuccessful) {
//                    updateSuccess.value = true
//                    ToastUtils.showSuccessPopup(context, "Profile updated successfully!")
//                    getPatientDetailsByUHID()
//                } else {
//                    updateSuccess.value = false
//                    errorMessage.value = "Error: ${response.code()}"
//                }

            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Unknown error"
            } finally {
                loading.value = false
            }
        }
    }

    private fun getPatientDetailsByUHID(context: Context) {
        loading.value = true
        viewModelScope.launch {
            try {
                val queryParams = mapOf(
                    "mobileNo" to "",
                    "uhid" to PrefsManager(context ).getPatient()?.empId.toString(),
                    "ClientId" to 194
                )

                val response = RetrofitInstance.createApiService()
//                    .dynamicGet(
//                        url = ApiEndPoint().getPatientDetailsByMobileNo,
//                        params = queryParams
//                    )

//                if (response.isSuccessful) {
//                    val responseBodyString = response.body()?.string()
//                    val type = object : TypeToken<BaseResponse<List<Patient>>>() {}.type
//                    val parsed =
//                        Gson().fromJson<BaseResponse<List<Patient>>>(responseBodyString, type)
//                    parsed.responseValue.firstOrNull()?.let {
//                        PrefsManager().savePatient(it)
//                    }
//                } else {
//                    errorMessage.value = "Error: ${response.code()}"
//                }

            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                loading.value = false
            }
        }
    }
}