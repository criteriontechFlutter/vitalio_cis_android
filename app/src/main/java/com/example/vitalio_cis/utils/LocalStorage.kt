package com.critetiontech.ctvitalio.utils


import android.content.Context

object LocalStorage {
    private const val PREF_NAME = "local_data_store"

    private fun prefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveString(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String): String? {
        return prefs(context).getString(key, null)
    }


    fun clear(context: Context, key: String) {
        prefs(context).edit().clear()
    }
}