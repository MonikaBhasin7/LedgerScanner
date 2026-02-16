package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.White

@Composable
fun ExamsEmptyState(
        onCreateExamClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Blue100.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Addchart,
                        contentDescription = null,
                        tint = Blue500,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "No exams yet",
                    style = AppTypography.text18SemiBold,
                    color = Grey900
                )
                Text(
                    text = "Create your first exam to start scanning sheets and tracking results.",
                    style = AppTypography.text13Regular,
                    color = Grey600,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Setup takes around 2 minutes. You can edit details anytime.",
                    style = AppTypography.text11Regular,
                    color = Grey500,
                    textAlign = TextAlign.Center
                )

                GenericButton(
                    text = "Create Exam",
                    icon = Icons.Default.AddCircleOutline,
                    type = ButtonType.PRIMARY,
                    size = ButtonSize.LARGE,
                    onClick = onCreateExamClick,
                    modifier = Modifier.fillMaxWidth()
                )

                HowItWorksRow(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                )
            }
        }
    }

@Composable
private fun HowItWorksRow(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HowItWorksStep(number = "1", label = "Create")
            Text(text = "\u2192", style = AppTypography.text11Medium, color = Grey500)
            HowItWorksStep(number = "2", label = "Scan")
            Text(text = "\u2192", style = AppTypography.text11Medium, color = Grey500)
            HowItWorksStep(number = "3", label = "Results")
        }
    }

@Composable
private fun HowItWorksStep(number: String, label: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Blue100.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Blue500),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = AppTypography.text10Medium,
                    color = White
                )
            }
            Text(
                text = label,
                style = AppTypography.text11Medium,
                color = Grey800
            )
        }
    }

@Composable
private fun OnboardingChecklist(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Grey100)
                .border(1.dp, Grey200, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = "Getting started (0/3)",
                style = AppTypography.text12Medium,
                color = Grey800
            )
            ChecklistItem(label = "Create your first exam")
            ChecklistItem(label = "Scan at least one sheet")
            ChecklistItem(label = "Review first result report")
        }
    }

@Composable
private fun ChecklistItem(label: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Grey400, RoundedCornerShape(999.dp))
            )
            Text(
                text = label,
                style = AppTypography.text11Regular,
                color = Grey600
            )
        }
    }
