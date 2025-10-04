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