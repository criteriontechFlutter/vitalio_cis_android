package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.EmergencyContact
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmergencyContactsViewModel : ViewModel() {

    private val deps = AppDependencies
    private val repository = deps.emergencyContactRepository

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /** Message when the list couldn't be loaded (as opposed to the patient having none). */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    /** True while an add, edit or remove is with the server. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    /** One-off message for a toast; the screen clears it with [messageShown]. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init { loadContacts() }

    fun messageShown() { _message.value = null }

    fun loadContacts() {
        viewModelScope.launch {
            _loading.value = true
            _loadError.value = null
            val patient = deps.prefs.getPatient() ?: run {
                _loadError.value = MyApplication.appContext.getString(R.string.profile_session_expired_long)
                _loading.value = false
                return@launch
            }
            when (val r = repository.fetchContacts(patient.pid, patient.clientId)) {
                is DomainResult.Success -> _contacts.value = r.data
                is DomainResult.Error -> {
                    _loadError.value = MyApplication.appContext.getString(R.string.profile_emergency_contacts_load_failed)
                    Log.e("EmergencyContactsVM", r.exception.message.orEmpty())
                }
            }
            _loading.value = false
        }
    }

    /** Adds a contact when [existing] is null, otherwise updates it. [onDone] runs only on success. */
    fun saveContact(existing: EmergencyContact?, name: String, number: String, relationship: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.value = MyApplication.appContext.getString(R.string.profile_session_expired_long)
                return@launch
            }
            _saving.value = true
            val contact = (existing ?: EmergencyContact()).copy(
                pid = patient.pid,
                clientId = patient.clientId,
                contactName = name.trim(),
                contactNumber = number.trim(),
                relationship = relationship.trim(),
            )
            when (val r = repository.saveContact(contact)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _message.value = MyApplication.appContext.getString(if (existing == null) R.string.profile_contact_added else R.string.profile_contact_updated)
                    onDone()
                    loadContacts()
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    _message.value = MyApplication.appContext.getString(R.string.profile_contact_save_failed)
                    Log.e("EmergencyContactsVM", r.exception.message.orEmpty())
                }
            }
            _saving.value = false
        }
    }

    fun removeContact(contact: EmergencyContact, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            when (val r = repository.removeContact(contact.id)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _contacts.value = _contacts.value.filterNot { it.id == contact.id }
                    _message.value = MyApplication.appContext.getString(R.string.profile_contact_removed)
                    onDone()
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    _message.value = MyApplication.appContext.getString(R.string.profile_contact_remove_failed)
                    Log.e("EmergencyContactsVM", r.exception.message.orEmpty())
                }
            }
            _saving.value = false
        }
    }
}
