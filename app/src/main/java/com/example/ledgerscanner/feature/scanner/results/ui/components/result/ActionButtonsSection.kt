package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White

@Composable
fun ActionButtonsSection(
    onSaveAndContinue: () -> Unit,
    onRetryScan: () -> Unit,
    isSaveEnabled: Boolean,
    isActionInProgress: Boolean = false,
    saveState: UiState<Long>,
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
                text = if (isActionInProgress) "Saving..." else "Save & Continue",
                onClick = onSaveAndContinue,
                size = ButtonSize.LARGE,
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled && !isActionInProgress
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActionInProgress
                )
            }

            Spacer(Modifier.height(10.dp))

            when (saveState) {
                is UiState.Loading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Saving result...",
                            style = AppTypography.body3Regular,
                            color = Blue500
                        )
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = saveState.message,
                        style = AppTypography.body3Regular,
                        color = Red500
                    )
                }

                is UiState.Success -> {
                    Text(
                        text = "Saved successfully. Redirecting...",
                        style = AppTypography.body3Regular,
                        color = Green500
                    )
                }

                is UiState.Idle -> {
                    if (!isSaveEnabled) {
                        Text(
                            text = "Scan barcode to enable save.",
                            style = AppTypography.body3Regular,
                            color = Grey600
                        )
                    }
                }
            }
        }
    }
}
