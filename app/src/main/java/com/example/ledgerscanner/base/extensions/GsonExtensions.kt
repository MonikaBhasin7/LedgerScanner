package com.example.ledgerscanner.base.extensions

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

// Load JSON from assets and parse into the requested type T.
inline fun <reified T> Context.loadClassFromAssets(filename: String): T {
    val gson = Gson()
    val json = this.assets.open(filename).bufferedReader().use { it.readText() }
    return gson.fromJson(json, T::class.java)
}

inline fun <reified T> Context.loadJsonFromAssets(
    filename: String,
    gson: Gson = Gson()
): T? {
    return try {
        val json = assets.open(filename).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        // Use TypeToken to correctly handle generic types as well as plain classes
        val type: Type = object : TypeToken<T>() {}.type
        gson.fromJson<T>(json, type)
    } catch (e: IOException) {
        // asset not found or read error
        e.printStackTrace()
        null
    } catch (e: JsonSyntaxException) {
        // malformed JSON
        e.printStackTrace()
        null
    } catch (e: Exception) {
        // fallback for any other unexpected runtime exceptions
        e.printStackTrace()
        null
    }
}