package com.example.ledgerscanner.base.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import com.example.ledgerscanner.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericToolbar(title: String) {
    return TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = White,
        )
    )
}