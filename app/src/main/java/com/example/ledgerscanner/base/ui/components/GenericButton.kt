package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.White

@Composable
fun GenericButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    type: ButtonType = ButtonType.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    shape: Shape = RoundedCornerShape(24.dp),
    textStyle: TextStyle? = null, // Now optional, determined by size
) {
    val (bgColor, contentColor, borderColor) = when (type) {
        ButtonType.PRIMARY -> Triple(
            Blue500,
            White,
            null
        )
        ButtonType.SECONDARY -> Triple(
            Color.Transparent,
            Blue500,
            Blue500
        )
        ButtonType.WARNING -> Triple(
            Color(0xFFFF9800), // Orange
            White,
            null
        )
        ButtonType.SUCCESS -> Triple(
            Green500,
            White,
            null
        )
        ButtonType.NEUTRAL -> Triple(
            Color.Transparent,
            Grey600,
            Grey400
        )
        ButtonType.TERTIARY -> Triple(
            Grey100,
            Grey600,
            null
        )
    }

    val border = borderColor?.let { color ->
        val actualBorderColor = if (enabled) color else Grey400
        BorderStroke(1.dp, actualBorderColor)
    }

    // Size-specific properties
    val (contentPadding, iconSize, spacerWidth, defaultTextStyle) = when (size) {
        ButtonSize.SMALL -> {
            SizeConfig(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                iconSize = 16.dp,
                spacerWidth = 6.dp,
                textStyle = AppTypography.label3Medium
            )
        }
        ButtonSize.MEDIUM -> {
            SizeConfig(
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                iconSize = 20.dp,
                spacerWidth = 8.dp,
                textStyle = AppTypography.label2SemiBold
            )
        }
        ButtonSize.LARGE -> {
            SizeConfig(
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
                iconSize = 24.dp,
                spacerWidth = 10.dp,
                textStyle = AppTypography.label1Bold
            )
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = contentColor,
            disabledContainerColor = Grey200,
            disabledContentColor = Grey500
        ),
        shape = shape,
        border = border,
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.width(spacerWidth))
        }
        Text(
            text = text,
            style = textStyle ?: defaultTextStyle
        )
    }
}

enum class ButtonType {
    PRIMARY,    // Blue filled
    SECONDARY,  // Blue outlined
    WARNING,    // Orange filled
    SUCCESS,    // Green filled
    NEUTRAL,    // Grey outlined
    TERTIARY    // Grey filled
}

enum class ButtonSize {
    SMALL,      // Compact buttons
    MEDIUM,     // Default size
    LARGE       // Prominent buttons
}

private data class SizeConfig(
    val contentPadding: PaddingValues,
    val iconSize: Dp,
    val spacerWidth: Dp,
    val textStyle: TextStyle
)