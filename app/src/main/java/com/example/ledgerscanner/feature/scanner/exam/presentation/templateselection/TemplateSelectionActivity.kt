package com.example.ledgerscanner.feature.scanner.exam.presentation.templateselection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen.SelectTemplateScreen
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TemplateSelectionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BackHandler { finish() }
            LedgerScannerTheme {
                Scaffold(
                    topBar = {
                        GenericToolbar(
                            title = "Select Template",
                            onBackClick = { finish() }
                        )
                    }
                ) { innerPadding ->
                    SelectTemplateScreen(
                        onSelect = { template ->
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(EXTRA_SELECTED_TEMPLATE, template)
                            )
                            finish()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SELECTED_TEMPLATE = "extra_selected_template"
    }
}
