package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.feature.scanner.results.model.SheetFilter

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Blue600)
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "📋", fontSize = 64.sp)
            Text(text = "No sheets scanned yet", style = AppTypography.body2Medium, color = Grey600)
        }
    }
}

@Composable
fun FilteredEmptyState(selectedFilter: SheetFilter) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "🔍", fontSize = 48.sp)
            Text(
                text = when (selectedFilter) {
                    SheetFilter.HIGH_SCORE -> "No high score sheets"
                    SheetFilter.LOW_SCORE -> "No low score sheets"
                    else -> "No sheets found"
                },
                style = AppTypography.body2Medium,
                color = Grey600,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Try changing the filter",
                style = AppTypography.body3Regular,
                color = Grey500,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorState(message: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "⚠️", fontSize = 64.sp)
            Text(
                text = message ?: "Something went wrong",
                style = AppTypography.body2Medium,
                color = Grey600,
                textAlign = TextAlign.Center
            )
        }
    }
}