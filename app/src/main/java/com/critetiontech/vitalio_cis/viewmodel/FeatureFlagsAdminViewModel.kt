package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.critetiontech.ctvitalio.config.AppFeature
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Admin side of the feature switches. Remote Config can't be written from the app, so a switch
 * is saved to the Firestore doc [COLLECTION]/[DOCUMENT]; the `publishFeatureFlags` Cloud Function
 * (firebase/functions) copies it into Remote Config, which every app is listening to.
 */
class FeatureFlagsAdminViewModel : ViewModel() {

    companion object {
        const val COLLECTION = "cis_app_config"
        const val DOCUMENT = "feature_flags"
        private const val TAG = "FeatureFlagsAdmin"
    }

    private val doc = FirebaseFirestore.getInstance().collection(COLLECTION).document(DOCUMENT)

    /** What the admin has saved; null until the first read (or if nothing was ever saved). */
    private val _saved = MutableStateFlow<Map<AppFeature, Boolean>?>(null)
    val saved: StateFlow<Map<AppFeature, Boolean>?> = _saved

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    /** Switches whose write hasn't been confirmed by Firestore yet. */
    private val _saving = MutableStateFlow<Set<AppFeature>>(emptySet())
    val saving: StateFlow<Set<AppFeature>> = _saving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Reading the saved switches failed (e.g. Firestore rules don't allow it). */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    private val registration: ListenerRegistration = doc.addSnapshotListener { snap, e ->
        _loading.value = false
        if (e != null) {
            Log.w(TAG, "Reading feature flags failed", e)
            _loadError.value = e.localizedMessage
            return@addSnapshotListener
        }
        _saved.value = snap?.takeIf { it.exists() }?.let { s ->
            AppFeature.entries.mapNotNull { f -> s.getBoolean(f.key)?.let { f to it } }.toMap()
        }
    }

    fun clearError() { _error.value = null }

    fun setEnabled(feature: AppFeature, enabled: Boolean) {
        val patient = PrefsManager(MyApplication.appContext).getPatient()
        _saving.update { it + feature }
        _saved.update { (it ?: emptyMap()) + (feature to enabled) }
        doc.set(
            mapOf(
                feature.key to enabled,
                "updatedBy" to (patient?.mobileNo ?: ""),
                "updatedByUhid" to (patient?.uhId ?: ""),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).addOnCompleteListener { task ->
            _saving.update { it - feature }
            if (!task.isSuccessful) {
                Log.w(TAG, "Saving ${feature.key} failed", task.exception)
                _error.value = task.exception?.localizedMessage
                // Undo the optimistic change; the snapshot listener will restore the real value too
                _saved.update { it?.minus(feature) }
            }
        }
    }

    override fun onCleared() {
        registration.remove()
    }
}
