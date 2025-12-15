package com.example.ledgerscanner.feature.scanner.exam.ui.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Red500

@Composable
fun DeleteConfirmationDialog(
    examName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Exam?",
                style = AppTypography.h3Medium
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$examName\"? This action cannot be undone.",
                style = AppTypography.body2Regular
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Delete",
                    color = Red500,
                    style = AppTypography.body2Medium
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    style = AppTypography.body2Medium
                )
            }
        }
    )
}