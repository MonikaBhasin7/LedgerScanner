package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp

@Composable
fun GenericEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: (@Composable (() -> Unit))? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.height(12.dp))
            Text(text = text)
        }
    }
}