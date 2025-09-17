package com.example.ledgerscanner.base.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BorderStyle {
    Solid,
    Dashed,
    Dotted
}

fun Modifier.customBorder(
    width: Dp,
    color: Color,
    style: BorderStyle = BorderStyle.Solid,
    cornerRadius: Dp = 0.dp
): Modifier = this.then(
    Modifier.drawBehind {
        val strokeWidthPx = width.toPx()

        val pathEffect = when (style) {
            BorderStyle.Solid -> null
            BorderStyle.Dashed -> PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f) // dash-gap
            BorderStyle.Dotted -> PathEffect.dashPathEffect(floatArrayOf(4f, 10f), 0f)   // dot-gap
        }

        drawRoundRect(
            color = color,
            size = Size(size.width, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = strokeWidthPx, pathEffect = pathEffect)
        )
    }
)