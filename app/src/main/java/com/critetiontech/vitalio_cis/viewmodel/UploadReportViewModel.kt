package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.MediaItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class UploadReportViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList

    private val _responseString = MutableStateFlow("")
    val responseString: StateFlow<String> = _responseString

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    private var pendingFile: File? = null
    private var pendingCategory: String = ""
    private var pendingDateTime: String = ""
    private var pendingSubCategory: String = ""
    private var pendingRemark: String = ""

    fun resetUploadSuccess() { _uploadSuccess.value = false }

    fun fetchMedia() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchMedia()(patient.uhId, patient.clientId.toString())) {
                is DomainResult.Success -> _mediaList.value = r.data
                is DomainResult.Error -> Log.e("UploadReportVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun aiReport(file: File, category: String, dateTime: String, subCategory: String, remark: String) {
        pendingFile = file; pendingCategory = category; pendingDateTime = dateTime; pendingSubCategory = subCategory; pendingRemark = remark
        viewModelScope.launch {
            _isAnalyzing.value = true
            when (val r = deps.aiReport()(file)) {
                is DomainResult.Success -> {
                    _responseString.value = r.data
                    _navigationEvent.emit(Routes.AIREPORT)
                    // After navigation, auto-insert the AI results into InvestigationByPatient
                    val patient = prefs.getPatient()
                    if (patient != null) {
                        when (val insertResult = deps.insertInvestigationResult()(r.data, patient.uhId, patient.clientId)) {
                            is DomainResult.Success -> Log.d("UploadReportVM", "InsertResult: success")
                            is DomainResult.Error   -> Log.e("UploadReportVM", "InsertResult failed: ${insertResult.exception.message}")
                        }
                    } else {
                        Log.e("UploadReportVM", "InsertResult skipped — patient prefs not available")
                    }
                }
                is DomainResult.Error -> Log.e("UploadReportVM", r.exception.message.orEmpty())
            }
            _isAnalyzing.value = false
        }
    }

    fun AddMedia() {
        val file = pendingFile ?: run { Log.e("UploadReportVM", "No pending file"); return }
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.uploadMedia()(patient.uhId, patient.clientId.toString(), pendingCategory, pendingDateTime, pendingSubCategory, pendingRemark, file)) {
                is DomainResult.Success -> _uploadSuccess.value = true
                is DomainResult.Error -> Log.e("UploadReportVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }
}
