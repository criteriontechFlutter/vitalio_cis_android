package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.DoctorDetails
import com.example.vitalio_cis.model.DoctorResponsedata
import com.example.vitalio_cis.model.ShiftData
import com.example.vitalio_cis.model.ShiftResponse
import com.example.vitalio_cis.ui.components.showToast
import com.example.vitalio_cis.ui.screens.BookingDetails
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class DoctorDetailsViewModel @Inject constructor() : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _doctorId = MutableLiveData("")
    val doctorId: LiveData<String> = _doctorId

    fun setDoctorId(id: String){
        _doctorId.value = id
    }

    private val _slotTime = MutableLiveData("")
    val slotTime: LiveData<String> = _slotTime

    fun setSlotTime(value: String){
        _slotTime.value = value
    }

    private val _slotDate = MutableLiveData("")
    val slotDate: LiveData<String> = _slotDate

    fun setSlotDate(value: String){
        _slotDate.value = value
    }


    private val _shiftId = MutableLiveData("")
    val  shiftId: LiveData<String> = _shiftId

    fun setShiftId(value: String){
        _shiftId.value = value
    }



    private val _departmentId = MutableLiveData("")
    val  departmentId: LiveData<String> = _departmentId

    fun setDepartmentId(value: String){
        _departmentId.value = value
    }

    private val _doctor = MutableStateFlow<DoctorDetails?>(null)
    val doctor: StateFlow<DoctorDetails?> = _doctor


    private val _slotList = MutableStateFlow<List<ShiftData>>(emptyList())
    val slotList: StateFlow<List<ShiftData>> = _slotList


    fun getDoctorProfile(context: Context, ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "clientID" to prefsCache.getPatient()?.clientId.toString(),
                    "doctorId"  to doctorId.value
                )
                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getDoctorProfile,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getDoctorProfile,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4084.dynamicGet(
                            url = url,
                            params = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, DoctorResponsedata::class.java)

                    _doctor.value = apiResponse.responseValue.firstOrNull()


                    Log.d("LoginViewModel", "OTP SuccessSuccess (API/Cache): $result")
                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }

    fun fetchAvailableSlots(context: Context, scheduleDate: String) {

        viewModelScope.launch {
            _slotList.value=emptyList()
            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "doctorId"  to  doctorId.value.toString(),
                "scheduleDate" to scheduleDate,
                "userId" to "1362",
                    "clientID" to prefsCache.getPatient()?.clientId.toString(),
                )
                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().fetchAvailableSlots,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchAvailableSlots,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(
                            url = url,
                            params = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, ShiftResponse::class.java)

                    _slotList.value = apiResponse.responseValue


                    Log.d("LoginViewModel", "OTP SuccessSuccess (API/Cache): $result")
                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }




    fun bookAppointment(
        context: Context, did: String,

        sTime: String, appointmentDate: String,
        navController: NavController,
        bookingDetails: BookingDetails?
    ) {

        viewModelScope.launch {
            _slotList.value=emptyList()
            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(


                    "departmentId"  to  3,
                "doctorId"  to  did,
                "uhId"  to  prefsCache.getPatient()?.uhId.toString(),
                "isOnline"  to  true,
                "clientId"  to  prefsCache.getPatient()?.clientId.toString(),
                "shiftId"  to  6,
                "slotTime"  to sTime.toString(),
                "appointmentDate" to appointmentDate.toString(),
                )
                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().bookAppointment,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().bookAppointment,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicRawPost(
                            url = url,
                            body = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {

                    val json = Gson().toJson(bookingDetails)
                    navController.navigate(
                        Routes.BOOKINGDETAILS + "/${Uri.encode(json)}"
                    )
                    val apiResponse = Gson().fromJson(result, ShiftResponse::class.java)

                    _slotList.value = apiResponse.responseValue
                    if(apiResponse.status.toString()=="0"){

                        showToast(context,apiResponse.responseValue.toString())
                    }


                    Log.d("LoginViewModel", "OTP SuccessSuccess (API/Cache): $result")
                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }

}
