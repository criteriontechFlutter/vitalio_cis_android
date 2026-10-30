package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.data.repository.HealthShareOfflineException
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.HealthShareCreated
import com.critetiontech.ctvitalio.model.HealthShareLink
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthShareViewModel : ViewModel() {

    private val deps = AppDependencies
    private val repository = deps.healthShareRepository

    private val _links = MutableStateFlow<List<HealthShareLink>>(emptyList())
    val links: StateFlow<List<HealthShareLink>> = _links

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /** Message when the list couldn't be loaded (as opposed to the patient having none). */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    /** True while a share or revoke is with the server. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    /** The share just created, for the "sent" dialog; cleared with [createdShown]. */
    private val _created = MutableStateFlow<HealthShareCreated?>(null)
    val created: StateFlow<HealthShareCreated?> = _created

    /** One-off message for a toast; the screen clears it with [messageShown]. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init { loadLinks() }

    fun messageShown() { _message.value = null }
    fun createdShown() { _created.value = null }

    private fun string(id: Int) = MyApplication.appContext.getString(id)

    fun loadLinks() {
        viewModelScope.launch {
            _loading.value = true
            _loadError.value = null
            val patient = deps.prefs.getPatient() ?: run {
                _loadError.value = string(R.string.profile_session_expired_long)
                _loading.value = false
                return@launch
            }
            when (val r = repository.fetchLinks(patient.pid, patient.clientId)) {
                // Active first, then revoked; the server already sends newest first within each.
                is DomainResult.Success -> _links.value = r.data.sortedByDescending { it.active }
                is DomainResult.Error -> {
                    _loadError.value = string(if (r.exception is HealthShareOfflineException) R.string.share_offline else R.string.share_load_failed)
                    Log.e("HealthShareVM", r.exception.message.orEmpty())
                }
            }
            _loading.value = false
        }
    }

    /** Emails [email] a link and access code. [onDone] runs only on success. */
    fun share(email: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.value = string(R.string.profile_session_expired_long)
                return@launch
            }
            _saving.value = true
            val name = listOf(patient.firstName, patient.lastName.orEmpty()).joinToString(" ").trim()
            // The patient record has no separate user id; the server's own shares use the pid for it.
            when (val r = repository.createLink(patient.uhId, patient.pid, patient.pid, patient.clientId, email.trim(), name)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _created.value = r.data.copy(recipientEmail = r.data.recipientEmail ?: email.trim())
                    onDone()
                    loadLinks()
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    _message.value = string(if (r.exception is HealthShareOfflineException) R.string.share_offline else R.string.share_create_failed)
                    Log.e("HealthShareVM", r.exception.message.orEmpty())
                }
            }
            _saving.value = false
        }
    }

    fun revoke(link: HealthShareLink, onDone: () -> Unit) {
        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.value = string(R.string.profile_session_expired_long)
                return@launch
            }
            _saving.value = true
            when (val r = repository.revokeLink(link.id, patient.clientId)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _message.value = string(R.string.share_revoked_toast)
                    onDone()
                    loadLinks()
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    _message.value = string(if (r.exception is HealthShareOfflineException) R.string.share_offline else R.string.share_revoke_failed)
                    Log.e("HealthShareVM", r.exception.message.orEmpty())
                }
            }
            _saving.value = false
        }
    }
}
