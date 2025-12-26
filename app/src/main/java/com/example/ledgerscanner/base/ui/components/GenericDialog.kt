package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Red500

@Composable
fun GenericDialog(
    dismissable: Boolean = false,
    onDismissRequest: () -> Unit = {},
    content: @Composable (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            content?.invoke()
        }
    }
}

@Composable
fun LoadingDialog(
    message: String = "Please wait..."
) {
    GenericDialog {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Blue500
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = message,
                style = AppTypography.body2Medium,
                color = Grey900,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorDialog(
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = "OK"
) {
    GenericDialog {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error icon
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Red500,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                style = AppTypography.h4Bold,
                color = Grey900,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Message
            Text(
                text = message,
                style = AppTypography.body3Regular,
                color = Grey700,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Button
            GenericButton(
                text = buttonText,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SuccessDialog(
    title: String = "Success",
    message: String,
    onDismiss: () -> Unit,
    buttonText: String = "Continue"
) {
    GenericDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Green500,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                style = AppTypography.h4Bold,
                color = Grey900,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Message
            Text(
                text = message,
                style = AppTypography.body3Regular,
                color = Grey700,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Button
            GenericButton(
                text = buttonText,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}