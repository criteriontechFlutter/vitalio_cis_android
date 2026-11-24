package com.critetiontech.ctvitalio.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlin.metadata.KmClassifier
import kotlin.metadata.isNullable
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

/**
 * The app's one Gson. Use it for every server response instead of `Gson()`.
 *
 * Gson fills objects by reflection and ignores Kotlin nullability, so a `"name": null` from the
 * server lands in a `val name: String` and crashes the first `.trim()` on it (seen with medicine
 * and doctor names). After parsing, this Gson puts "" into every non-null String property and an
 * empty list into every non-null List property that came back null. Properties declared nullable
 * (`String?`) are left alone, so `?: fallback` code keeps working.
 */
val AppGson: Gson = GsonBuilder()
    .registerTypeAdapterFactory(NonNullDefaultsFactory)
    .create()

private object NonNullDefaultsFactory : TypeAdapterFactory {

    private sealed interface Fill { val field: Field }
    private class EmptyString(override val field: Field) : Fill
    private class EmptyList(override val field: Field) : Fill

    private val cache = ConcurrentHashMap<Class<*>, List<Fill>>()

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val fills = fillsFor(type.rawType)
        if (fills.isEmpty()) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)

            override fun read(reader: JsonReader): T? {
                val value = delegate.read(reader) ?: return null
                for (fill in fills) {
                    if (fill.field.get(value) != null) continue
                    fill.field.set(value, if (fill is EmptyString) "" else emptyList<Any>())
                }
                return value
            }
        }
    }

    /** The String / List fields of a Kotlin class whose properties are declared non-null. */
    private fun fillsFor(cls: Class<*>): List<Fill> = cache.getOrPut(cls) {
        val metadata = cls.getAnnotation(Metadata::class.java) ?: return@getOrPut emptyList()
        try {
            val kmClass = (KotlinClassMetadata.readLenient(metadata) as? KotlinClassMetadata.Class)?.kmClass
                ?: return@getOrPut emptyList()
            kmClass.properties.mapNotNull { property ->
                val returnType = property.returnType
                if (returnType.isNullable) return@mapNotNull null
                val classifier = (returnType.classifier as? KmClassifier.Class)?.name ?: return@mapNotNull null
                val fieldName = property.fieldSignature?.name ?: return@mapNotNull null
                val field = runCatching { cls.getDeclaredField(fieldName) }.getOrNull() ?: return@mapNotNull null
                field.isAccessible = true
                when (classifier) {
                    "kotlin/String" -> EmptyString(field)
                    "kotlin/collections/List" -> EmptyList(field)
                    else -> null
                }
            }
        } catch (e: Exception) {
            // Unreadable metadata only means no filling for this class, never a parse failure
            Log.w("AppGson", "no nullability info for ${cls.name}: ${e.message}")
            emptyList()
        }
    }
}
