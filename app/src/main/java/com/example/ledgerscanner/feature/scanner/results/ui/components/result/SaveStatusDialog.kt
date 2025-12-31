package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.runtime.Composable
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.*

@Composable
fun SaveStatusDialog(
    saveSheetResponse: UiState<Long>,
    onScanNextSelected: Boolean,
    onDismiss: () -> Unit
) {
    when (saveSheetResponse) {
        is UiState.Idle -> { /* No dialog */ }
        
        is UiState.Loading -> {
            LoadingDialog(message = "Saving scan result...")
        }
        
        is UiState.Success -> {
            if (!onScanNextSelected) {
                SuccessDialog(
                    message = "Scan saved successfully!",
                    onDismiss = onDismiss
                )
            } else {
                onDismiss()
            }
        }
        
        is UiState.Error -> {
            ErrorDialog(
                message = saveSheetResponse.message,
                onDismiss = onDismiss
            )
        }
    }
}