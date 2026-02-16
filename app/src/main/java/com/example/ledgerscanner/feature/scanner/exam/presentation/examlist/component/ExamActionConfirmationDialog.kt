package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Red500

@Composable
fun ExamActionConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDanger = confirmText.equals("Delete", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = AppTypography.text18SemiBold,
                color = Black
            )
        },
        text = {
            Text(
                text = message,
                style = AppTypography.text14Regular,
                color = Grey600,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 2.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    style = AppTypography.text14SemiBold,
                    color = if (isDanger) Red500 else Blue500
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = AppTypography.text14Medium,
                    color = Grey600
                )
            }
        }
    )
}
