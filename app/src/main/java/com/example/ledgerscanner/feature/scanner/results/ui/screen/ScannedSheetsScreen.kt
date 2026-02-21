package com.example.ledgerscanner.feature.scanner.results.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ErrorDialog
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.LoadingDialog
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.model.ScannedSheetViewMode
import com.example.ledgerscanner.feature.scanner.results.ui.activity.ScanResultActivity
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.DeleteActionButton
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.DeleteConfirmationDialog
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.EmptyState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ErrorState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ExamStatsHeader
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.FilterAndSortControls
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.FilteredEmptyState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.LoadingState
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ScannedSheetCard
import com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets.ScannedSheetGridRow
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.base.ui.theme.Grey50
@Composable
fun ScannedSheetsScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    scanResultViewModel: ScanResultViewModel
) {
    val context = LocalContext.current
    val scannedSheets by scanResultViewModel.scannedSheets.collectAsState()
    val examStats by scanResultViewModel.examStatsCache.collectAsState()
    val selectedFilter by scanResultViewModel.selectedFilter.collectAsState()
    val selectedSort by scanResultViewModel.selectedSort.collectAsState()
    val viewMode by scanResultViewModel.viewMode.collectAsState()
    val selectedSheets by scanResultViewModel.selectedSheets.collectAsState()
    val selectionMode by scanResultViewModel.selectionMode.collectAsState()
    val deleteState by scanResultViewModel.deleteState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val handleBack = rememberBackHandler(navController)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableStateOf(0) }
    val headerCollapseFraction by remember(listState) {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                else -> (listState.firstVisibleItemScrollOffset / 170f).coerceIn(0f, 1f)
            }
        }
    }
    val headerListTopPadding by remember(headerHeightPx, density) {
        derivedStateOf {
            if (headerHeightPx > 0) {
                with(density) { headerHeightPx.toDp() } + 2.dp
            } else {
                172.dp
            }
        }
    }

    LaunchedEffect(Unit) {
        scanResultViewModel.getScannedSheetsByExamId(examEntity.id)
        scanResultViewModel.loadStatsForExam(examEntity.id)
    }

    Scaffold(
        topBar = {
            GenericToolbar(title = "Scanned Sheets", onBackClick = handleBack)
        },
        bottomBar = {
            if (selectionMode && selectedSheets.isNotEmpty()) {
                DeleteActionButton(
                    selectedCount = selectedSheets.size,
                    onDelete = { showDeleteConfirmDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Grey50)
        ) {
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
                    val hasAnySheets = dataHolder?.originalList?.isNotEmpty() == true

                    if (!hasAnySheets) {
                        EmptyState(modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(top = headerListTopPadding)
                            ) {
                                item {
                                    FilterAndSortControls(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 8.dp
                                        ),
                                        selectedFilter = selectedFilter,
                                        selectedSort = selectedSort,
                                        viewMode = viewMode,
                                        sheets = dataHolder.originalList,
                                        selectionMode = selectionMode,
                                        selectedCount = selectedSheets.size,
                                        onFilterChange = { scanResultViewModel.setFilter(it) },
                                        onSortChange = { scanResultViewModel.setSort(it) },
                                        onViewModeChange = { scanResultViewModel.setViewMode(it) },
                                        onSelectAll = {
                                            scanResultViewModel.selectAll(filteredSheets)
                                        },
                                        onDeselectAll = {
                                            scanResultViewModel.deselectAll()
                                        }
                                    )
                                }
                                when {
                                    filteredSheets.isEmpty() -> {
                                        item { FilteredEmptyState(selectedFilter) }
                                    }

                                    else -> {
                                        when (viewMode) {
                                            ScannedSheetViewMode.LIST -> {
                                                items(
                                                    items = filteredSheets,
                                                    key = { it.id }
                                                ) { sheet ->
                                                    ScannedSheetCard(
                                                        sheet = sheet,
                                                        isSelected = selectedSheets.contains(sheet.id),
                                                        selectionMode = selectionMode,
                                                        onCardClick = {
                                                            scanResultViewModel.toggleSheetSelection(sheet.id)
                                                        },
                                                        onLongClick = {
                                                            scanResultViewModel.enterSelectionMode()
                                                            scanResultViewModel.toggleSheetSelection(sheet.id)
                                                        },
                                                        onViewDetails = {
                                                            ScanResultActivity.launchScanResultScreen(
                                                                context,
                                                                examEntity,
                                                                sheet,
                                                                isViewMode = true
                                                            )
                                                        },
                                                        modifier = Modifier.padding(
                                                            horizontal = 16.dp,
                                                            vertical = 6.dp
                                                        )
                                                    )
                                                }
                                            }

                                            ScannedSheetViewMode.GRID -> {
                                                items(
                                                    items = filteredSheets.chunked(2),
                                                    key = { row -> row.firstOrNull()?.id ?: -1 }
                                                ) { rowSheets ->
                                                    ScannedSheetGridRow(
                                                        rowSheets = rowSheets,
                                                        selectedSheets = selectedSheets,
                                                        selectionMode = selectionMode,
                                                        onCardClick = { sheetId ->
                                                            scanResultViewModel.toggleSheetSelection(sheetId)
                                                        },
                                                        onLongClick = { sheetId ->
                                                            scanResultViewModel.enterSelectionMode()
                                                            scanResultViewModel.toggleSheetSelection(sheetId)
                                                        },
                                                        onViewDetails = { sheet ->
                                                            ScanResultActivity.launchScanResultScreen(
                                                                context,
                                                                examEntity,
                                                                sheet,
                                                                isViewMode = true
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.onSizeChanged { size ->
                                    // Keep the largest measured header height to avoid relayout on every scroll tick.
                                    if (size.height > headerHeightPx) {
                                        headerHeightPx = size.height
                                    }
                                }
                            ) {
                                ExamStatsHeader(
                                    examEntity = examEntity,
                                    stats = examStats[examEntity.id],
                                    collapseFraction = headerCollapseFraction
                                )
                            }
                        }
                    }
                }
            }

            // Delete Confirmation Dialog
            if (showDeleteConfirmDialog) {
                DeleteConfirmationDialog(
                    count = selectedSheets.size,
                    onConfirm = {
                        scanResultViewModel.deleteSelectedSheets()
                        showDeleteConfirmDialog = false
                    },
                    onDismiss = {
                        showDeleteConfirmDialog = false
                    }
                )
            }

            // Delete Status Dialog
            when (deleteState) {
                is UiState.Loading -> {
                    LoadingDialog(message = "Deleting sheets...")
                }

                is UiState.Success -> {
                    LaunchedEffect(Unit) {
                        scanResultViewModel.resetDeleteState()
                    }
                }

                is UiState.Error -> {
                    ErrorDialog(
                        message = (deleteState as UiState.Error).message,
                        onDismiss = { scanResultViewModel.resetDeleteState() }
                    )
                }

                else -> {}
            }
        }
    }
}
