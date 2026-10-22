package com.critetiontech.ctvitalio.data.repository

import com.critetiontech.ctvitalio.config.ApiConfig

import com.critetiontech.ctvitalio.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NutriFood(
    @SerializedName("foodID") val foodId: Int = 0,
    @SerializedName("foodName") val foodName: String? = null,
    @SerializedName("foodNameShort") val foodNameShort: String? = null,
    @SerializedName("foodImagePath") val foodImagePath: String? = null,
    @SerializedName("foodCategoryID") val foodCategoryId: Int = 0,
) {
    val displayName: String get() = (foodName ?: foodNameShort).orEmpty().trim()
}

data class NutriUnit(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("unitName") val unitName: String = "",
    @SerializedName("isDefault") val isDefault: Boolean = false,
)

data class NutriNutrient(
    @SerializedName("nutrientName") val nutrientName: String = "",
    @SerializedName("consumed") val consumed: String? = null,
    @SerializedName("unitName") val unitName: String? = null,
) {
    /** The API sends numbers as strings ("34.70000"), sometimes non-numeric ("VALUE"). */
    val amount: Double? get() = consumed?.trim()?.toDoubleOrNull()
}

class NutriException(message: String) : Exception(message)

/**
 * NutriAnalyser external API: food search → units for a food → nutrients for a serving.
 * All calls are JSON POSTs authenticated with the X-App-Id / X-App-Secret headers.
 */
class NutriRepository(
    private val appId: String = BuildConfig.NUTRI_APP_ID,
    private val appSecret: String = BuildConfig.NUTRI_APP_SECRET,
) {
    val isConfigured: Boolean get() = appId.isNotBlank() && appSecret.isNotBlank()

    suspend fun searchFoods(prefix: String): List<NutriFood> =
        post("foodsearch", mapOf("prefixText" to prefix), Array<NutriFood>::class.java).toList()

    suspend fun foodUnits(foodId: Int): List<NutriUnit> =
        post("foodweight", mapOf("foodId" to foodId), Array<NutriUnit>::class.java).toList()

    suspend fun nutrients(foodId: Int, quantity: Double, unitId: Int): List<NutriNutrient> =
        post(
            "foodnutrient",
            mapOf("foodId" to foodId, "foodQuantity" to quantity, "foodUnitId" to unitId),
            Array<NutriNutrient>::class.java,
        ).toList()

    private suspend fun <T> post(path: String, body: Map<String, Any>, type: Class<Array<T>>): Array<T> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) throw NutriException("Nutrition lookup isn't set up yet (missing API key).")
            val request = Request.Builder()
                .url("$BASE_URL/external/$path")
                .header("X-App-Id", appId)
                .header("X-App-Secret", appSecret)
                .post(gson.toJson(body).toRequestBody(JSON))
                .build()
            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                throw NutriException("Couldn't reach the nutrition service. Check your internet and try again.")
            }
            response.use { r ->
                val text = r.body.string()
                when (r.code) {
                    401, 403 -> throw NutriException("The nutrition service rejected the app's API key.")
                }
                if (!r.isSuccessful) throw NutriException("The nutrition service had a problem (${r.code}). Please try again.")
                val json = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull()
                    ?: throw NutriException("Unexpected reply from the nutrition service.")
                if (json.get("isException")?.takeIf { !it.isJsonNull }?.asBoolean == true) {
                    val msg = json.get("exceptionMessage")?.takeIf { !it.isJsonNull }?.asString
                    throw NutriException(msg?.takeIf { it.isNotBlank() } ?: "The nutrition service couldn't answer that.")
                }
                val value = json.get("responseValue")
                gson.fromJson(if (value != null && value.isJsonArray) value else com.google.gson.JsonArray(), type)
            }
        }

    private companion object {
        const val BASE_URL = ApiConfig.NUTRI_ANALYSER
        val JSON = "application/json; charset=utf-8".toMediaType()
        val gson = Gson()
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
