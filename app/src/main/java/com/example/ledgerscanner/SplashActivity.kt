package com.example.ledgerscanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.auth.ui.LoginActivity
import com.example.ledgerscanner.feature.scanner.exam.ui.activity.ExamListingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenStore: TokenStore

    private var openTheScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            openTheScreen
        }

        openTheScreen = false

        val next = if (tokenStore.getAccessToken().isNullOrBlank()) {
            LoginActivity::class.java
        } else {
            ExamListingActivity::class.java
        }

        startActivity(Intent(this@SplashActivity, next))
        finish()
    }
}
