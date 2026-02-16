package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.base.ui.components.GenericSwitch
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey300
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.domain.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel.CreateExamViewModel

@Composable
fun MarkingDefaultsScreen(
    modifier: Modifier = Modifier,
    createExamViewModel: CreateExamViewModel,
    updateBottomBar: (BottomBarConfig) -> Unit,
    viewMode: CreateExamConfig.Mode
) {
    var marksPerCorrect by remember { mutableStateOf("1") }
    var marksPerWrong by remember { mutableStateOf("0.25") }
    var negativeMarking by remember { mutableStateOf(true) }
    val examEntity by createExamViewModel.examEntity.collectAsState()

    val isValid by remember {
        derivedStateOf {
            val marksPerCorrect = marksPerCorrect.toFloatOrNull()
            val marksPerWrong = marksPerWrong.toFloatOrNull()
            marksPerCorrect != null && marksPerCorrect > 0 && (marksPerWrong != null || !negativeMarking)
        }
    }

    LaunchedEffect(Unit) {
        examEntity?.let {
            if (it.marksPerCorrect != null) {
                marksPerCorrect = it.marksPerCorrect.toString()
            }
            if (it.marksPerWrong != null) {
                marksPerWrong = it.marksPerWrong.toString()
                if (it.marksPerWrong > 0) {
                    negativeMarking = true
                } else {
                    negativeMarking = false
                }
            }
        }
    }

    LaunchedEffect(isValid, marksPerCorrect, marksPerWrong, negativeMarking) {
        updateBottomBar(
            BottomBarConfig(
                enabled = isValid,
                onNext = {
                    val currentCorrect = marksPerCorrect.toFloatOrNull()
                    val currentWrong = marksPerWrong.toFloatOrNull()
                    val savedWrong = if (negativeMarking) examEntity?.marksPerWrong else 0f

                    if (currentCorrect != examEntity?.marksPerCorrect
                        || currentWrong != savedWrong
                    ) {
                        createExamViewModel.saveMarkingScheme(
                            marksPerCorrect,
                            marksPerWrong,
                            negativeMarking
                        )
                    } else {
                        createExamViewModel.changeOperationState(OperationState.Success)
                    }
                }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(White)
            .border(1.dp, Grey200, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                GenericTextField(
                    value = marksPerCorrect,
                    onValueChange = {
                        // Allow only numbers and decimal point, max 100
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            val value = it.toFloatOrNull()
                            if (value == null || value <= 100f) {
                                marksPerCorrect = it
                            }
                        }
                    },
                    placeholder = "1",
                    label = "Marks per correct",
                    labelStyle = AppTypography.body3Medium,
                    prefix = {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(White)
                                .border(1.dp, Grey300, CircleShape)
                                .genericClick {
                                    val current = marksPerCorrect.toFloatOrNull() ?: 1f
                                    if (current < 100f) {
                                        marksPerCorrect = (current + 1f).toString()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increment",
                                tint = Grey600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    suffix = {
                        Text(
                            text = "points",
                            style = AppTypography.body2Regular,
                            color = Grey500
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                GenericTextField(
                    value = marksPerWrong,
                    onValueChange = {
                        // Allow only positive numbers and decimal point, max 100
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            val value = it.toFloatOrNull()
                            if (value == null || value <= 100f) {
                                marksPerWrong = it
                            }
                        }
                    },
                    placeholder = "0.25",
                    label = "Marks per wrong",
                    enabled = negativeMarking,
                    labelStyle = AppTypography.body3Medium,
                    prefix = {
                        Row {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(White)
                                    .border(1.dp, Grey300, CircleShape)
                                    .genericClick {
                                        if (negativeMarking) {
                                            val current = marksPerWrong.toFloatOrNull() ?: 0.25f
                                            if (current < 100f) {
                                                marksPerWrong = (current + 0.25f).toString()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increment",
                                    tint = Grey600,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text("-")
                        }
                    },
                    suffix = {
                        Text(
                            text = "points",
                            style = AppTypography.body2Regular,
                            color = Grey500
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            Text(
                text = "Negative marking",
                style = AppTypography.body3Medium,
                color = Grey500,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(1.dp, Grey200, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (negativeMarking) "On" else "Off",
                    style = AppTypography.body2Medium,
                    color = Grey700
                )

                GenericSwitch(
                    checked = negativeMarking,
                    onCheckedChange = {
                        negativeMarking = it
                    }
                )
            }
        }
    }
}