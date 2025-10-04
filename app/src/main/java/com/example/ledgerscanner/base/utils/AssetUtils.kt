package com.example.ledgerscanner.base.utils

import android.content.Context

class AssetUtils {

    fun listJsonAssets(context: Context): List<String> {
        return context.assets.list("")  // "" means root of assets
            ?.filter { it.endsWith(".json") }
            ?: emptyList()
    }

    fun loadJsonAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}