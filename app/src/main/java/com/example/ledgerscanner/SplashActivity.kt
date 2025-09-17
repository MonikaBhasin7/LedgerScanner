package com.example.ledgerscanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.ExamListingActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.ScanOmrWithCamera

class SplashActivity : AppCompatActivity() {

    private var openTheScreen = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            openTheScreen
        }

        openTheScreen = false
        startActivity(Intent(this@SplashActivity, ScanOmrWithCamera::class.java))
        finish()
    }
}