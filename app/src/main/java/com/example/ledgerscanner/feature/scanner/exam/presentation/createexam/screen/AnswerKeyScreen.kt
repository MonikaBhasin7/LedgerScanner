package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen

import GenericFilterChip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.navigateFromActivity
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.domain.model.AnswerKeyBulkFillType
import com.example.ledgerscanner.feature.scanner.exam.domain.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel.CreateExamViewModel

private const val OPTION_A = 0
private const val OPTION_B = 1
private const val OPTION_C = 2
private const val OPTION_D = 3

@Composable
fun AnswerKeyScreen(
    navController: NavHostController,
    createExamViewModel: CreateExamViewModel,
    modifier: Modifier = Modifier,
    updateBottomBar: (BottomBarConfig) -> Unit,
    viewMode: CreateExamConfig.Mode,
) {
    val context = LocalContext.current
    var selectedBulkFill by remember { mutableStateOf<AnswerKeyBulkFillType?>(null) }
    val examEntity by createExamViewModel.examEntity.collectAsState()
    val questionCount = examEntity?.totalQuestions ?: 0
    val answerKeys = remember {
        MutableList<Int?>(questionCount) { null }.toMutableStateList()
    }

    LaunchedEffect(Unit) {
        examEntity?.answerKey?.let { ak ->
            if (ak.isNotEmpty()) {
                ak.forEach { (key, value) ->
                    if (key in answerKeys.indices) {
                        answerKeys[key] = value
                    }
                }
            }
        }
    }

    val allAnswered by remember {
        derivedStateOf {
            answerKeys.all { it != null }
        }
    }

    LaunchedEffect(allAnswered, answerKeys.toList()) {
        val isViewMode = CreateExamConfig.Mode.VIEW == viewMode
        updateBottomBar(
            BottomBarConfig(
                enabled = allAnswered,
                buttonText = if (isViewMode) "Back" else "Next",
                onNext = {
                    if (isViewMode) {
                        navController.navigateFromActivity(context)
                        return@BottomBarConfig
                    }

                    val hasChanges = answerKeys.indices.any { index ->
                        answerKeys[index] != examEntity?.answerKey?.get(index)
                    }

                    if (hasChanges) {
                        createExamViewModel.saveAnswerKey(
                            answerKeys = answerKeys.filterNotNull(),
                            saveInDb = false
                        )
                    } else {
                        createExamViewModel.changeOperationState(OperationState.Success)
                    }
                },
            )
        )
    }

    fun setAnswerKey() {
        when (selectedBulkFill) {

            AnswerKeyBulkFillType.ALL_A ->
                answerKeys.indices.forEach { index ->
                    answerKeys[index] = OPTION_A
                }

            AnswerKeyBulkFillType.ALL_B ->
                answerKeys.indices.forEach { index ->
                    answerKeys[index] = OPTION_B
                }

            AnswerKeyBulkFillType.AB_ALT ->
                answerKeys.forEachIndexed { index, _ ->
                    answerKeys[index] =
                        if (index % 2 == 0) OPTION_A else OPTION_B
                }

            AnswerKeyBulkFillType.CLEAR ->
                answerKeys.indices.forEach { index ->
                    answerKeys[index] = null
                }

            null -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        BulkFillWidget(
            selectedLabel = selectedBulkFill,
            enabled = viewMode == CreateExamConfig.Mode.EDIT,
            onSelect = {
                selectedBulkFill = it
                setAnswerKey()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnswerKeyWidget(
            answerKeys = answerKeys,
            enabled = viewMode == CreateExamConfig.Mode.EDIT,
            onSelectAnswer = { index, answer ->
                if (answerKeys[index] != answer) {
                    answerKeys[index] = answer
                    if (selectedBulkFill != null) selectedBulkFill = null
                }
            }
        )
    }
}

@Composable
fun AnswerKeyWidget(
    answerKeys: List<Int?>,
    onSelectAnswer: (Int, Int) -> Unit,
    enabled: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Answer Key", style = AppTypography.text14SemiBold, color = Grey500)
            Text(
                "Tap a choice to mark correct",
                style = AppTypography.text14Regular,
                color = Grey500
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        AnswerKeyGrid(answerKeys, onSelectAnswer, enabled)
    }
}

@Composable
fun AnswerKeyGrid(
    answerKeys: List<Int?>,
    onSelectAnswer: (Int, Int) -> Unit,
    enabled: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = answerKeys,
            key = { index, _ -> index }
        ) { index, answer ->
            QuestionCard(
                questionNumber = index + 1,
                answerItemSelected = answer,
                enabled = enabled,
                onSelectAnswer = { selected ->
                    onSelectAnswer(index, selected)
                },
            )
        }
    }
}

@Composable
fun QuestionCard(
    questionNumber: Int,
    answerItemSelected: Int?,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onSelectAnswer: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Grey200, RoundedCornerShape(16.dp))
            .background(Grey100)
            .padding(12.dp)
    ) {
        Text("Q$questionNumber", style = AppTypography.text15SemiBold)

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionButton(
                    "A",
                    answerItemSelected == OPTION_A,
                    enabled,
                    Modifier.weight(1f)
                ) {
                    onSelectAnswer(OPTION_A)
                }
                OptionButton(
                    "B",
                    answerItemSelected == OPTION_B,
                    enabled,
                    Modifier.weight(1f)
                ) {
                    onSelectAnswer(OPTION_B)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionButton(
                    "C",
                    answerItemSelected == OPTION_C,
                    enabled,
                    Modifier.weight(1f)
                ) {
                    onSelectAnswer(OPTION_C)
                }
                OptionButton(
                    "D",
                    answerItemSelected == OPTION_D,
                    enabled,
                    Modifier.weight(1f)
                ) {
                    onSelectAnswer(OPTION_D)
                }
            }
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
//            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Green500 else White)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else Grey200,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .genericClick { if (enabled) onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) White else Black,
            style = AppTypography.text15SemiBold
        )
    }
}

@Composable
fun BulkFillWidget(
    onSelect: (AnswerKeyBulkFillType) -> Unit,
    selectedLabel: AnswerKeyBulkFillType?,
    enabled: Boolean
) {
    if (!enabled) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Bulk fill", style = AppTypography.text14SemiBold, color = Grey500)
            Text("Quickly set patterns", style = AppTypography.text14Regular, color = Grey500)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnswerKeyBulkFillType.entries.forEach { type ->
                GenericFilterChip(
                    label = type.label,
                    selected = selectedLabel == type,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}
