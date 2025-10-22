package com.example.ledgerscanner.base.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography

/**
 * Overlay that blocks input and shows a centered rectangular transparent box
 * with a progress indicator and an optional message.
 *
 * @param message optional text to show under the spinner (e.g. "Saving draft...")
 * @param boxWidth/boxHeight: size of the rectangle; it's responsive so feel free to tweak
 */
@Composable
fun GenericRectangularLoader(
    message: String? = null,
    boxWidth: Dp = 280.dp,
    boxHeight: Dp = 120.dp,
    backgroundDim: Float = 0.35f // backdrop darkness
) {
    BackHandler(enabled = true) { /* consume back press */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundDim)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(boxWidth)
                .height(boxHeight)
                .background(Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
                if (!message.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = AppTypography.label2SemiBold
                    )
                }
            }
        }
    }
}