package com.example.ledgerscanner.base.extensions

import kotlin.math.pow
import kotlin.math.round

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 07/01/26
// ===========================================================================


/**
 * Format Float/Double to remove unnecessary decimals
 * 50.0 -> "50"
 * 50.5 -> "50.5"
 * 50.123 -> "50.123"
 * 50.100 -> "50.1"
 */
fun Float.toCleanString(maxDecimals: Int = 2): String {
    // Round to avoid floating-point precision issues
    val multiplier = 10.0.pow(maxDecimals.toDouble())
    val rounded = round(this * multiplier) / multiplier

    return when {
        rounded % 1.0 == 0.0 -> rounded.toInt().toString()
        else -> {
            val formatted = "%.${maxDecimals}f".format(rounded)
            formatted.trimEnd('0').trimEnd('.')
        }
    }
}

fun Double.toCleanString(maxDecimals: Int = 2): String {
    val multiplier = 10.0.pow(maxDecimals.toDouble())
    val rounded = round(this * multiplier) / multiplier

    return when {
        rounded % 1.0 == 0.0 -> rounded.toInt().toString()
        else -> {
            val formatted = "%.${maxDecimals}f".format(rounded)
            formatted.trimEnd('0').trimEnd('.')
        }
    }
}