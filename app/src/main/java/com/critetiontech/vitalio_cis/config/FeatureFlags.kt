package com.critetiontech.ctvitalio.config

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.critetiontech.ctvitalio.BuildConfig
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.Patient
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's main modules, each switchable for every user through Firebase Remote Config.
 *
 * [key] is the Remote Config parameter (and the field in the admin's Firestore doc). The
 * Firebase project is shared with other apps, hence the `cis_feature_` prefix.
 */
enum class AppFeature(val key: String, @StringRes val label: Int, @StringRes val group: Int) {
    VITALS("cis_feature_vitals", R.string.ff_vitals, R.string.ff_group_home),
    FLUID("cis_feature_fluid", R.string.ff_fluid, R.string.ff_group_home),
    SYMPTOMS("cis_feature_symptoms", R.string.ff_symptoms, R.string.ff_group_home),
    MEDICINE("cis_feature_medicine", R.string.ff_medicine, R.string.ff_group_home),
    DIET("cis_feature_diet", R.string.ff_diet, R.string.ff_group_home),
    INTERACTION_CHECKER("cis_feature_interaction_checker", R.string.ff_interaction_checker, R.string.ff_group_home),
    APPOINTMENTS("cis_feature_appointments", R.string.ff_appointments, R.string.ff_group_home),
    LAB_REPORTS("cis_feature_lab_reports", R.string.ff_lab_reports, R.string.ff_group_home),
    PRESCRIPTIONS("cis_feature_prescriptions", R.string.ff_prescriptions, R.string.ff_group_home),
    VOICE_ASSISTANT("cis_feature_voice_assistant", R.string.ff_voice_assistant, R.string.ff_group_home),
    ACTIVITY("cis_feature_activity", R.string.ff_activity, R.string.ff_group_tabs),
    SMART_RING("cis_feature_smart_ring", R.string.ff_smart_ring, R.string.ff_group_tabs),
    ALLERGIES("cis_feature_allergies", R.string.ff_allergies, R.string.ff_group_profile),
    EMERGENCY_CONTACTS("cis_feature_emergency_contacts", R.string.ff_emergency_contacts, R.string.ff_group_profile),
    FAMILY_HEALTH_HISTORY("cis_feature_family_health_history", R.string.ff_family_health_history, R.string.ff_group_profile),
    SHARE_HEALTH_RECORDS("cis_feature_share_health_records", R.string.ff_share_health_records, R.string.ff_group_profile),
    FAQ("cis_feature_faq", R.string.ff_faq, R.string.ff_group_profile),
    FEEDBACK("cis_feature_feedback", R.string.ff_feedback, R.string.ff_group_profile),
}

/**
 * Reads the feature switches from Remote Config and keeps them live: a change published in the
 * console (or from the admin page, via the Cloud Function) reaches open apps within seconds.
 * Every feature is ON until Remote Config says otherwise, so a fetch failure never hides anything.
 */
object FeatureFlags {
    private const val TAG = "FeatureFlags"

    private val defaults: Map<String, Any> = AppFeature.entries.associate { it.key to true }

    private val _enabled = MutableStateFlow(AppFeature.entries.associateWith { true })
    val enabled: StateFlow<Map<AppFeature, Boolean>> = _enabled.asStateFlow()

    fun isEnabled(feature: AppFeature): Boolean = _enabled.value[feature] ?: true

    fun init() {
        val rc = FirebaseRemoteConfig.getInstance()
        rc.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                .build()
        )
        rc.setDefaultsAsync(defaults).addOnCompleteListener { publish(rc) }
        rc.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) Log.w(TAG, "Remote Config fetch failed", task.exception)
            publish(rc)
        }
        // Real-time updates, so a switch flipped by the admin applies without an app restart
        rc.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                if (configUpdate.updatedKeys.none { k -> AppFeature.entries.any { it.key == k } }) return
                rc.activate().addOnCompleteListener { publish(rc) }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Remote Config live update failed", error)
            }
        })
    }

    private fun publish(rc: FirebaseRemoteConfig) {
        _enabled.value = AppFeature.entries.associateWith { rc.getBoolean(it.key) }
    }
}

/** Live feature switches for Compose; recomposes when an admin flips one. */
@Composable
fun rememberFeatureFlags(): Map<AppFeature, Boolean> {
    val flags by FeatureFlags.enabled.collectAsState()
    return flags
}

/** Only this login sees the Feature Flags admin page. */
object FeatureAdmin {
    private const val ADMIN_MOBILE = "9807039752"

    fun isAdmin(patient: Patient?): Boolean =
        patient?.mobileNo?.filter(Char::isDigit)?.takeLast(10) == ADMIN_MOBILE
}
