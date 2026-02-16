package com.example.ledgerscanner.feature.scanner.results.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.results.model.ScannedSheetDataHolder
import com.example.ledgerscanner.feature.scanner.results.model.ScannedSheetViewMode
import com.example.ledgerscanner.feature.scanner.results.model.SheetFilter
import com.example.ledgerscanner.feature.scanner.results.model.SheetSort
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanResultViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository
) : ViewModel() {

    private val _sheetsCountByExamId = MutableStateFlow<UiState<Int>>(UiState.Loading())
    val sheetsCountByExamId = _sheetsCountByExamId.asStateFlow()

    private val _saveSheetState = MutableStateFlow<UiState<Long>>(UiState.Idle())
    val saveSheetState = _saveSheetState.asStateFlow()

    private val _scannedSheets =
        MutableStateFlow<UiState<ScannedSheetDataHolder>>(UiState.Loading())
    val scannedSheets = _scannedSheets.asStateFlow()

    private val _examStatsCache = MutableStateFlow<Map<Int, ExamStatistics>>(emptyMap())
    val examStatsCache = _examStatsCache.asStateFlow()
    private val loadingExamStats = mutableSetOf<Int>()

    private val _selectedFilter = MutableStateFlow(SheetFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(SheetSort.DATE_NEWEST)
    val selectedSort = _selectedSort.asStateFlow()

    private val _viewMode = MutableStateFlow(ScannedSheetViewMode.LIST)
    val viewMode = _viewMode.asStateFlow()

    private val _selectedSheets = MutableStateFlow<Set<Int>>(emptySet())
    val selectedSheets = _selectedSheets.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle())
    val deleteState = _deleteState.asStateFlow()

    fun toggleSheetSelection(sheetId: Int) {
        val updatedSelection = _selectedSheets.updateAndGet { currentSet ->
            if (currentSet.contains(sheetId)) {
                currentSet - sheetId
            } else {
                currentSet + sheetId
            }
        }
        if (updatedSelection.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun selectAll(sheets: List<ScanResultEntity>) {
        _selectedSheets.value = sheets.map { it.id }.toSet()
        _selectionMode.value = true
    }

    fun deselectAll() {
        _selectedSheets.value = emptySet()
        _selectionMode.value = false
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedSheets.value = emptySet()
    }

    fun deleteSelectedSheets() {
        viewModelScope.launch {
            try {
                _deleteState.value = UiState.Loading()

                val selectedIds = _selectedSheets.value
                if (selectedIds.size == 1) {
                    scanResultRepository.deleteSheet(selectedIds.first())
                } else {
                    scanResultRepository.deleteMultipleSheets(selectedIds.toList())
                }
                _deleteState.value = UiState.Success(Unit)

                exitSelectionMode()

            } catch (e: Exception) {
                _deleteState.value = UiState.Error(e.message ?: "Failed to delete sheets")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = UiState.Idle()
    }

    fun setViewMode(mode: ScannedSheetViewMode) {
        _viewMode.value = mode
    }

    fun setFilter(filter: SheetFilter) {
        _selectedFilter.value = filter
        applyFilterAndSort()
    }

    fun setSort(sort: SheetSort) {
        _selectedSort.value = sort
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val currentState = _scannedSheets.value
        if (currentState !is UiState.Success) return

        val originalList = currentState.data?.originalList

        // Filter
        val filtered = when (_selectedFilter.value) {
            SheetFilter.ALL -> originalList
            SheetFilter.HIGH_SCORE -> originalList?.filter { it.scorePercent >= 75 }
            SheetFilter.LOW_SCORE -> originalList?.filter { it.scorePercent < 40 }
        }

        // Sort
        val sorted = when (_selectedSort.value) {
            SheetSort.DATE_NEWEST -> filtered?.sortedByDescending { it.scannedAt }
            SheetSort.DATE_OLDEST -> filtered?.sortedBy { it.scannedAt }
            SheetSort.SCORE_HIGH -> filtered?.sortedByDescending { it.scorePercent }
            SheetSort.SCORE_LOW -> filtered?.sortedBy { it.scorePercent }
            SheetSort.STUDENT_NAME -> filtered?.sortedBy { it.barCode ?: "" }
        }

        _scannedSheets.value = UiState.Success(
            ScannedSheetDataHolder(
                originalList = originalList,
                filterList = sorted?.toMutableList()
            )
        )
    }

    fun getScannedSheetsByExamId(examId: Int) {
        viewModelScope.launch {
            try {
                _scannedSheets.value = UiState.Loading()
                scanResultRepository.getAllByExamId(examId).collect { sheets ->
                    _scannedSheets.value = UiState.Success(
                        ScannedSheetDataHolder(
                            originalList = sheets.toMutableList(),
                            filterList = sheets.toMutableList()
                        )
                    )
                    applyFilterAndSort() // Apply current filter and sort
                }
            } catch (e: Exception) {
                _scannedSheets.value = UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun getCountByExamId(examId: Int) {
        viewModelScope.launch {
            try {
                _sheetsCountByExamId.value = UiState.Loading()
                scanResultRepository.getCountByExamId(examId).collect {
                    _sheetsCountByExamId.value = UiState.Success(it)
                }
            } catch (e: Exception) {
                _sheetsCountByExamId.value = UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun saveSheet(
        details: StudentDetailsForScanResult,
        scanResultEntity: ScanResultEntity?,
        examId: Int
    ) {
        viewModelScope.launch {
            try {
                _saveSheetState.value = UiState.Loading()

                if (scanResultEntity == null) {
                    _saveSheetState.value = UiState.Error("Invalid scan result")
                    return@launch
                }

                val insertedId = scanResultRepository.saveSheet(
                    details = details,
                    scanResultEntity = scanResultEntity
                )

                if (insertedId > 0) {
                    _saveSheetState.value = UiState.Success(insertedId)
                    getCountByExamId(examId)
                } else {
                    _saveSheetState.value = UiState.Error("Failed to save scan result")
                }

            } catch (e: IllegalArgumentException) {
                Log.e("ScannedSheetsViewModel", "Validation error", e)
                _saveSheetState.value = UiState.Error(e.message ?: "Invalid data")
            } catch (e: IllegalStateException) {
                Log.e("ScannedSheetsViewModel", "State error", e)
                _saveSheetState.value = UiState.Error(e.message ?: "Invalid state")
            } catch (e: Exception) {
                Log.e("ScannedSheetsViewModel", "Error saving sheet", e)
                _saveSheetState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSaveSheetState() {
        _saveSheetState.value = UiState.Idle()
    }

    fun loadSheetCount(examId: Int) {
        viewModelScope.launch {
            try {
                _sheetsCountByExamId.value = UiState.Loading()
                val count = scanResultRepository.getCountByExamIdOnce(examId)
                _sheetsCountByExamId.value = UiState.Success(count)
            } catch (e: Exception) {
                _sheetsCountByExamId.value = UiState.Error(e.message ?: "Error loading count")
            }
        }
    }

    fun getAllSheetsByExamId(examId: Int): Flow<List<ScanResultEntity>> {
        return scanResultRepository.getAllByExamId(examId)
    }

    fun loadStatsForExam(examId: Int) {
        if (_examStatsCache.value.containsKey(examId)) return
        synchronized(loadingExamStats) {
            if (!loadingExamStats.add(examId)) return
        }
        viewModelScope.launch {
            try {
                val stats = scanResultRepository.getStatistics(examId).first()
                _examStatsCache.update { currentCache -> currentCache + (examId to stats) }
            } catch (e: Exception) {
                Log.e("ScannedSheetsViewModel", "Error loading stats for exam $examId", e)
            } finally {
                synchronized(loadingExamStats) {
                    loadingExamStats.remove(examId)
                }
            }
        }
    }
}
