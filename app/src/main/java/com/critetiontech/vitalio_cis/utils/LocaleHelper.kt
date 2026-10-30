package com.critetiontech.ctvitalio.utils

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Languages the app ships strings for. [tag] is the BCP-47 tag; [nativeName] is shown in the picker. */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

/**
 * In-app language switch. Android 13+ uses the system per-app language (so it also shows in system
 * settings); older versions wrap each Activity's context in [wrap] from attachBaseContext.
 * MainActivity is a ComponentActivity with a non-AppCompat theme, so AppCompatDelegate can't do this.
 */
object LocaleHelper {
    private const val PREFS = "app_locale"
    private const val KEY_LANG = "language"

    fun current(context: Context): AppLanguage {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val system = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            if (system != null && !system.isEmpty) return AppLanguage.fromTag(system[0].language)
        }
        return AppLanguage.fromTag(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, null)
        )
    }

    /** Context with the saved language applied. Call from attachBaseContext of every Activity and the Application. */
    fun wrap(base: Context): Context {
        // On 13+ the system already applies the per-app locale to every context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val lang = AppLanguage.fromTag(
            base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, null)
        )
        val locale = Locale.forLanguageTag(lang.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }

    fun setLanguage(activity: Activity, language: AppLanguage) {
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANG, language.tag).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system recreates the activities itself
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(language.tag)
        } else {
            // ViewModels read strings through the application context; refresh it too, not just the activity
            val locale = Locale.forLanguageTag(language.tag)
            Locale.setDefault(locale)
            val appRes = activity.applicationContext.resources
            val config = Configuration(appRes.configuration).apply { setLocales(LocaleList(locale)) }
            @Suppress("DEPRECATION")
            appRes.updateConfiguration(config, appRes.displayMetrics)
            activity.recreate()
        }
    }
}
