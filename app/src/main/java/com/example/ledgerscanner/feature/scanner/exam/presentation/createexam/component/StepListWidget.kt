package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
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
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            StepItem(
                index = index,
                totalSteps = steps.size,
                label = step.title,
                isActive = currentStep.ordinal == step.ordinal,
                isCompleted = step.ordinal < currentStep.ordinal,
                onClick = { onStepSelected(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepItem(
    index: Int,
    totalSteps: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepIndexText = "${index + 1}"
    val indicatorBackground = when {
        isCompleted -> Green500
        isActive -> Blue500
        else -> White
    }
    val indicatorBorder = when {
        isCompleted -> Green500
        isActive -> Blue500
        else -> Grey200
    }
    val indicatorTextColor = if (isCompleted || isActive) White else Grey500
    val leftConnectorColor = if (index > 0 && (isCompleted || isActive)) Green500 else Grey200
    val rightConnectorColor = if (index < totalSteps - 1 && isCompleted) Green500 else Grey200
    val numberAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "stepNumberAlpha"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "stepCheckAlpha"
    )

    Column(
        modifier = modifier
            .genericClick { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(leftConnectorColor)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(indicatorBackground)
                    .border(1.dp, indicatorBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepIndexText,
                    style = AppTypography.text13SemiBold,
                    color = indicatorTextColor,
                    modifier = Modifier.graphicsLayer(alpha = numberAlpha)
                )
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = indicatorTextColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer(alpha = checkAlpha)
                )
            }

            if (index < totalSteps - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(rightConnectorColor)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AutoResizeSingleLineText(
            text = label,
            style = if (isActive) AppTypography.text13SemiBold else AppTypography.text13Medium,
            color = if (isActive || isCompleted) Black else Grey500,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AutoResizeSingleLineText(
    text: String,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    val initialStyle = remember(style) {
        if (style.fontSize == TextUnit.Unspecified) style.copy(fontSize = 13.sp) else style
    }
    val minFontSize = 1.sp
    var currentStyle by remember(text, initialStyle) { mutableStateOf(initialStyle) }

    Text(
        text = text,
        style = currentStyle,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth && currentStyle.fontSize > minFontSize) {
                val nextSize = (currentStyle.fontSize.value * 0.92f).sp
                val clampedSize = if (nextSize < minFontSize) minFontSize else nextSize
                if (clampedSize != currentStyle.fontSize) {
                    currentStyle = currentStyle.copy(fontSize = clampedSize)
                }
            }
        },
        modifier = modifier
    )
}
