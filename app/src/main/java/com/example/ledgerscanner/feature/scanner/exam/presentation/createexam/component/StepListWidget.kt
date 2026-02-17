package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStep
import kotlin.enums.EnumEntries

@Composable
fun StepListWidget(
    steps: EnumEntries<ExamStep>,
    currentStep: ExamStep,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            StepItem(
                label = label.title,
                isActive = currentStep.title == label.title,
                isPast = label.ordinal < currentStep.ordinal,
                onClick = { onStepSelected(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepItem(
    label: String,
    isActive: Boolean,
    isPast: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .genericClick { onClick() }
            .clip(RoundedCornerShape(999.dp))
            .background(if (isActive) Blue500.copy(alpha = 0.10f) else White)
            .border(1.dp, if (isActive) Blue500.copy(alpha = 0.20f) else Grey200, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Box(
                modifier = Modifier.Companion
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Blue500 else if (isPast) Green500 else Grey200)
            )

            Spacer(modifier = Modifier.Companion.width(8.dp))

            AutoResizeSingleLineText(
                text = label,
                style = if (isActive) AppTypography.text13SemiBold else AppTypography.text13Medium,
                color = if (isActive) Black else Grey500,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AutoResizeSingleLineText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val initialStyle = remember(style) {
        if (style.fontSize == TextUnit.Unspecified) style.copy(fontSize = 13.sp) else style
    }
    val minFontSize = 1.sp
    var currentStyle by remember(text, initialStyle) { mutableStateOf(initialStyle) }
    var readyToDraw by remember(text, initialStyle) { mutableStateOf(false) }

    Text(
        text = text,
        style = currentStyle,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth && currentStyle.fontSize > minFontSize) {
                val nextSize = (currentStyle.fontSize.value * 0.92f).sp
                val clampedSize = if (nextSize < minFontSize) minFontSize else nextSize
                if (clampedSize == currentStyle.fontSize) {
                    readyToDraw = true
                } else {
                    currentStyle = currentStyle.copy(fontSize = clampedSize)
                }
            } else {
                readyToDraw = true
            }
        },
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        }
    )
}
