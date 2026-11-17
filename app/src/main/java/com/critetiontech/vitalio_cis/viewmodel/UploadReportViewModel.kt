package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.MediaItem
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

    /** One-shot error for the user; the screen shows it and calls [clearError]. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    fun clearError() { _errorMessage.value = null }
    private fun str(@androidx.annotation.StringRes id: Int, vararg args: Any) =
        com.critetiontech.ctvitalio.utils.MyApplication.appContext.getString(id, *args)

    /** Results already sent for this analysis, so a second tap on Save can't duplicate them. */
    private var resultsSavedFor: String? = null

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
                    // Results are saved only when the user reviews them and taps Save (AddMedia),
                    // not here — otherwise backing out or re-analysing stored them anyway / twice.
                    _responseString.value = r.data
                    resultsSavedFor = null
                    _navigationEvent.emit(Routes.AIREPORT)
                }
                is DomainResult.Error -> {
                    Log.e("UploadReportVM", r.exception.message.orEmpty())
                    _errorMessage.value = r.exception.message?.takeIf { it.isNotBlank() } ?: str(R.string.report_analysis_failed)
                }
            }
            _isAnalyzing.value = false
        }
    }

    fun AddMedia() {
        val file = pendingFile ?: run { Log.e("UploadReportVM", "No pending file"); return }
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient()
            if (patient == null || patient.uhId.isBlank()) {
                _errorMessage.value = str(R.string.report_session_expired)
                _loading.value = false
                return@launch
            }
            // 1) the report file; if it fails, nothing else is saved
            when (val r = deps.uploadMedia()(patient.uhId, patient.clientId.toString(), pendingCategory, pendingDateTime, pendingSubCategory, pendingRemark, file)) {
                is DomainResult.Success -> Unit
                is DomainResult.Error -> {
                    Log.e("UploadReportVM", "AddMedia: ${r.exception.message}")
                    _errorMessage.value = str(R.string.report_upload_failed) +
                        (r.exception.message?.takeIf { !it.startsWith("API Error") }?.let { " ($it)" } ?: "")
                    Haptics.error()
                    _loading.value = false
                    return@launch
                }
            }
            // 2) the values the AI read, once per analysis
            val analysis = _responseString.value
            if (analysis.isNotEmpty() && resultsSavedFor != analysis) {
                val testName = pendingSubCategory.takeUnless { it.equals("General", ignoreCase = true) }.orEmpty()
                when (val r = deps.insertInvestigationResult()(analysis, patient.uhId, patient.clientId, testName, pendingDateTime)) {
                    is DomainResult.Success -> resultsSavedFor = analysis
                    is DomainResult.Error -> {
                        // The file is saved; tell the user the values didn't go in rather than failing silently.
                        Log.e("UploadReportVM", "InsertResult: ${r.exception.message}")
                        _errorMessage.value = str(R.string.report_values_not_saved, r.exception.message.orEmpty())
                    }
                }
            }
            _uploadSuccess.value = true
            Haptics.success()
            _loading.value = false
        }
    }
}
