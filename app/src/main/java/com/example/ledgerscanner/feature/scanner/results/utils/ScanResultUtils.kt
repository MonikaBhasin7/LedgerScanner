package com.example.ledgerscanner.feature.scanner.results.utils

object ScanResultUtils {
    fun isRecentSheet(scannedAt: Long): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return scannedAt > fiveMinutesAgo
    }
}
