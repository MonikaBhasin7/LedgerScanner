package com.example.ledgerscanner.feature.scanner.exam.ui.screen

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.feature.scanner.exam.model.AnswerKeyBulkFillType
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel

@Composable
fun AnswerKeyScreen(
    navController: NavHostController,
    createExamViewModel: CreateExamViewModel,
    modifier: Modifier.Companion
) {
    val examEntity by createExamViewModel.examEntity.collectAsState()
    var selectedLabel by rememberSaveable { mutableStateOf<AnswerKeyBulkFillType?>(null) }
    val answerKeys = remember {
        MutableList<Int?>(10) { null }.toMutableStateList()
    }
    Scaffold() { innerPadding ->
        Column {
            Spacer(modifier = Modifier.height(6.dp))
            BulkFillWidget(
                selectedLabel = selectedLabel,
                onSelect = {
                    selectedLabel = it
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AnswerKeyWidget(answerKeys) { index, selectedAnswer ->
                answerKeys[index] = selectedAnswer
            }
        }
    }
}

@Composable
fun AnswerKeyWidget(answerKeys: SnapshotStateList<Int?>, onSelectAnswer: (Int, Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Answer Key", style = AppTypography.body2Regular, color = Grey500)
            Text(
                "Tap a choice to mark correct",
                style = AppTypography.body2Regular,
                color = Grey500
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        AnswerKeyGrid(answerKeys, onSelectAnswer)
    }
}

@Composable
fun AnswerKeyGrid(answerKeys: SnapshotStateList<Int?>, onSelectAnswer: (Int, Int) -> Unit) {
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
                onSelectAnswer = { it ->
                    onSelectAnswer(index, it)
                }
            )
        }
    }
}

@Composable
fun QuestionCard(
    questionNumber: Int,
    answerItemSelected: Int?,
    modifier: Modifier = Modifier,
    onSelectAnswer: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Grey200, RoundedCornerShape(16.dp))
            .background(Grey100)
            .padding(12.dp)
    ) {
        Text(
            text = "Q${questionNumber}",
            style = AppTypography.label1SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionButton(
                    text = "A",
                    selected = answerItemSelected == 1,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectAnswer(1)
                    }
                )
                OptionButton(
                    text = "B",
                    selected = answerItemSelected == 2,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectAnswer(2)
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionButton(
                    text = "C",
                    selected = answerItemSelected == 3,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectAnswer(3)
                    }
                )
                OptionButton(
                    text = "D",
                    selected = answerItemSelected == 4,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectAnswer(4)
                    }
                )
            }
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Green500 else White)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else Grey200,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) White else Black,
            style = AppTypography.label1SemiBold
        )
    }
}

@Composable
fun BulkFillWidget(
    onSelect: (AnswerKeyBulkFillType) -> Unit,
    selectedLabel: AnswerKeyBulkFillType?
) {
    val labels = AnswerKeyBulkFillType.entries
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bulk fill", style = AppTypography.body2Regular, color = Grey500)
            Text("Quickly set patterns", style = AppTypography.body2Regular, color = Grey500)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            labels.forEach { label ->
                GenericFilterChip(
                    label.label,
                    selected = selectedLabel == label,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelect(label)
                    }
                )
            }
        }
    }
}