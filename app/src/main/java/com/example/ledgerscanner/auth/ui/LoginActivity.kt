package com.example.ledgerscanner.auth.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ledgerscanner.auth.AuthViewModel
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.feature.scanner.exam.presentation.activity.ExamListingActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                val loginSuccess by viewModel.loginSuccess.collectAsState()

                LaunchedEffect(loginSuccess) {
                    if (loginSuccess) {
                        startActivity(Intent(this@LoginActivity, ExamListingActivity::class.java))
                        finish()
                    }
                }

                LoginScreen(viewModel = viewModel)
            }
        }
    }
}
