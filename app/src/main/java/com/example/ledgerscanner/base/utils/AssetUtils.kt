package com.example.ledgerscanner.base.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

object AssetUtils {

    fun listJsonAssets(context: Context): List<String> {
        return context.assets.list("")  // "" means root of assets
            ?.filter { it.endsWith(".json") }
            ?: emptyList()
    }

    inline fun <reified T> loadJsonFromAssets(
        context: Context,
        filename: String,
        gson: Gson = Gson()
    ): T? {
        return try {
            val json = context.assets.open(filename).bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }
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
}