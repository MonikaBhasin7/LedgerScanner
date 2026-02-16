package com.example.ledgerscanner.feature.scanner.exam.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
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
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            StepItem(
                label = label.title,
                isActive = currentStep.title == label.title,
                isPast = label.ordinal < currentStep.ordinal,
                onClick = { onStepSelected(index) }
            )
        }
    }
}

@Composable
private fun StepItem(label: String, isActive: Boolean, isPast: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier.Companion
            .genericClick { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Companion.CenterVertically) {
            Box(
                modifier = Modifier.Companion
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Blue500 else if (isPast) Green500 else Grey200)
            )

            Spacer(modifier = Modifier.Companion.width(8.dp))

            Text(
                text = label,
                style = if (isActive) AppTypography.label2Bold else AppTypography.label2SemiBold,
                color = if (isActive) Black else Grey500
            )
        }
    }
}