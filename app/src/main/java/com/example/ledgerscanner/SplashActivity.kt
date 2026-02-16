package com.example.ledgerscanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.ledgerscanner.auth.AuthState
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.auth.ui.LoginActivity
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.activity.ExamListingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

        lifecycleScope.launch {
            AuthState.loggedOut.collect { loggedOut ->
                if (loggedOut) {
                    AuthState.reset()
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
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
