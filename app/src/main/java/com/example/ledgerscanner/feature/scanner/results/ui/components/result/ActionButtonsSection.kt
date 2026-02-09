package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.*
import com.example.ledgerscanner.base.ui.theme.*

@Composable
fun ActionButtonsSection(
    onSaveAndContinue: () -> Unit,
    onRetryScan: () -> Unit,
    onScanNext: () -> Unit,
    onViewAllSheets: () -> Unit,
    sheetCount: UiState<Int>,
    isSaveEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            GenericButton(
                text = "Save & Continue",
                onClick = onSaveAndContinue,
                size = ButtonSize.LARGE,
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GenericButton(
                    text = "🔄 Retry Scan",
                    onClick = onRetryScan,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f)
                )

                GenericButton(
                    text = "📷 Scan Next",
                    onClick = onScanNext,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (sheetCount is UiState.Success) {
                Text(
                    text = "View All Scanned Sheets (${sheetCount.data})",
                    style = AppTypography.label3Bold,
                    color = Blue500,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewAllSheets() },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
