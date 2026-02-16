package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus

@Composable
fun ExamList(
    examListResponse: UiState<List<ExamEntity>>,
    examStatistics: Map<Int, ExamStatistics>,
    celebratingExamId: Int?,
    hiddenExamIds: Set<Int>,
    walkthroughExamId: Int?,
    showNoScanWalkthrough: Boolean,
    selectedFilter: ExamStatus?,
    searchQuery: String,
    onDismissNoScanWalkthrough: () -> Unit,
    onCreateExamClick: () -> Unit,
    onClearFilters: () -> Unit,
    onExamClick: (ExamEntity, ExamAction) -> Unit,
    onRetry: () -> Unit,
    onActionClick: (ExamEntity, ExamAction) -> Unit,
    onLoadStats: (Int) -> Unit,
    actionsProvider: (ExamStatus, Boolean) -> ExamActionPopupConfig
) {
    when (examListResponse) {
        is UiState.Loading -> GenericLoader()
        is UiState.Error -> ErrorScreen(message = examListResponse.message, onRetry = onRetry)
        is UiState.Success -> {
            val items = examListResponse.data ?: emptyList()
            val visibleItemCount = items.count { !hiddenExamIds.contains(it.id) }
            if (visibleItemCount == 0) {
                if (selectedFilter != null || searchQuery.isNotBlank()) {
                    FilteredExamsEmptyState(
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        onClearFilters = onClearFilters
                    )
                } else {
                    ExamsEmptyState(onCreateExamClick = onCreateExamClick)
                }
                return
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items = items, key = { _, item -> item.id }) { _, item ->
                    LaunchedEffect(item.id) {
                        if (!examStatistics.containsKey(item.id)) {
                            onLoadStats(item.id)
                        }
                    }

                    AnimatedVisibility(
                        visible = !hiddenExamIds.contains(item.id),
                        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                            animationSpec = tween(220),
                            initialOffsetY = { it / 12 }
                        ),
                        exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(
                            animationSpec = tween(140),
                            targetOffsetY = { -it / 16 }
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            ExamCardRow(
                                item = item,
                                examStatistics = examStatistics[item.id],
                                showCompletionCelebration = celebratingExamId == item.id,
                                showNoScanWalkthrough = showNoScanWalkthrough && walkthroughExamId == item.id,
                                onDismissNoScanWalkthrough = onDismissNoScanWalkthrough,
                                actionsProvider = actionsProvider,
                                onClick = { onExamClick(item, it) },
                                onActionClick = { onActionClick(item, it) }
                            )
                        }
                    }
                }
            }
        }

        is UiState.Idle -> Unit
    }
}
