package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    EmptyState(modifier = Modifier.fillMaxSize())
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .background(White, RoundedCornerShape(18.dp))
                .border(1.dp, Blue100, RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StateIconBadge(
                icon = Icons.Outlined.Description,
                contentDescription = "No sheets",
                tint = Blue600
            )
            Text(
                text = "No sheets scanned yet",
                style = AppTypography.text18SemiBold,
                color = Grey700,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Scan answer sheets and all results will appear here.",
                style = AppTypography.text14Regular,
                color = Grey600,
                textAlign = TextAlign.Center
            )
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
            StateIconBadge(
                icon = Icons.Outlined.FilterList,
                contentDescription = "No filtered results",
                tint = Blue600
            )
            Text(
                text = when (selectedFilter) {
                    SheetFilter.HIGH_SCORE -> "No high score sheets"
                    SheetFilter.LOW_SCORE -> "No low score sheets"
                    else -> "No sheets found"
                },
                style = AppTypography.text16SemiBold,
                color = Grey700,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Try changing the filter",
                style = AppTypography.text14Regular,
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
            StateIconBadge(
                icon = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = Red600
            )
            Text(
                text = message ?: "Something went wrong",
                style = AppTypography.text16SemiBold,
                color = Grey700,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StateIconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(34.dp)
        )
    }
}
