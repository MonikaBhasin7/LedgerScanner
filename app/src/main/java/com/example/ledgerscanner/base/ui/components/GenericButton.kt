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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White

@Composable
fun GenericButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    type: ButtonType = ButtonType.PRIMARY,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = RoundedCornerShape(24.dp),
    textStyle: TextStyle = AppTypography.label2SemiBold,
) {
    val (bgColor, contentColor) = when (type) {
        ButtonType.PRIMARY -> MaterialTheme.colorScheme.primary to White
        ButtonType.SECONDARY -> Color.Transparent to MaterialTheme.colorScheme.primary
        ButtonType.TERTIARY -> Grey100 to Grey100
    }

    val border = when (type) {
        ButtonType.SECONDARY -> {
            val borderColor = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                Grey400
            }
            BorderStroke(1.dp, borderColor)
        }

        else -> null
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
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, style = textStyle)
    }
}

enum class ButtonType {
    PRIMARY,
    SECONDARY,
    TERTIARY
}