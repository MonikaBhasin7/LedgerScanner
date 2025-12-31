package com.example.ledgerscanner.feature.scanner.results.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.model.ScannedSheetViewMode
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.EmptyState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ErrorState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ExamStatsHeader
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.FilterAndSortControls
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.FilteredEmptyState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.LoadingState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ScannedSheetCard
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ScannedSheetGridRow
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScannedSheetsViewModel

@Composable
fun ScannedSheetsScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    scannedSheetsViewModel: ScannedSheetsViewModel
) {
    val scannedSheets by scannedSheetsViewModel.scannedSheets.collectAsState()
    val examStats by scannedSheetsViewModel.examStatsCache.collectAsState()
    val selectedFilter by scannedSheetsViewModel.selectedFilter.collectAsState()
    val selectedSort by scannedSheetsViewModel.selectedSort.collectAsState()
    val viewMode by scannedSheetsViewModel.viewMode.collectAsState()
    val handleBack = rememberBackHandler(navController)

    LaunchedEffect(Unit) {
        scannedSheetsViewModel.getScannedSheetsByExamId(examEntity.id)
        scannedSheetsViewModel.loadStatsForExam(examEntity.id)
    }

    Scaffold(
        topBar = {
            GenericToolbar(title = "Scanned Sheets", onBackClick = handleBack)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (scannedSheets) {
                is UiState.Error -> {
                    ErrorState(message = (scannedSheets as UiState.Error).message)
                }

                is UiState.Idle, is UiState.Loading -> {
                    LoadingState()
                }

                is UiState.Success -> {
                    val dataHolder = (scannedSheets as UiState.Success).data
                    val filteredSheets = dataHolder?.filterList ?: listOf()
                    val hasAnySheets = dataHolder?.originalList?.isNotEmpty()

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (hasAnySheets == true) {
                            item {
                                ExamStatsHeader(examEntity, examStats[examEntity.id])
                            }
                            item {
                                FilterAndSortControls(
                                    selectedFilter = selectedFilter,
                                    selectedSort = selectedSort,
                                    viewMode = viewMode,
                                    sheets = dataHolder.originalList,
                                    onFilterChange = { scannedSheetsViewModel.setFilter(it) },
                                    onSortChange = { scannedSheetsViewModel.setSort(it) },
                                    onViewModeChange = { scannedSheetsViewModel.setViewMode(it) }
                                )
                            }
                        }

                        when {
                            hasAnySheets == false -> {
                                item { EmptyState() }
                            }

                            filteredSheets.isEmpty() -> {
                                item { FilteredEmptyState(selectedFilter) }
                            }

                            else -> {
                                when (viewMode) {
                                    ScannedSheetViewMode.LIST -> {
                                        items(filteredSheets) { sheet ->
                                            ScannedSheetCard(
                                                sheet = sheet,
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 6.dp
                                                )
                                            )
                                        }
                                    }

                                    ScannedSheetViewMode.GRID -> {
                                        items(filteredSheets.chunked(2)) { rowSheets ->
                                            ScannedSheetGridRow(rowSheets)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}