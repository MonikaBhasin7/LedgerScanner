package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.SheetFilter
import com.example.ledgerscanner.feature.scanner.scan.model.SheetSort
import com.example.ledgerscanner.feature.scanner.scan.model.StudentDetailsForScanResult
import com.example.ledgerscanner.feature.scanner.scan.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannedSheetDataHolder(
    val filterList: MutableList<ScanResultEntity>? = null,
    val originalList: MutableList<ScanResultEntity>? = null,
)

@HiltViewModel
class ScannedSheetsViewModel @Inject constructor(
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

    private val _selectedFilter = MutableStateFlow(SheetFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(SheetSort.DATE_NEWEST)
    val selectedSort = _selectedSort.asStateFlow()

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
        omrImageProcessResult: OmrImageProcessResult?,
        examId: Int
    ) {
        viewModelScope.launch {
            try {
                _saveSheetState.value = UiState.Loading()

                if (omrImageProcessResult == null || !omrImageProcessResult.success) {
                    _saveSheetState.value = UiState.Error("Invalid scan result")
                    return@launch
                }

                val insertedId = scanResultRepository.saveSheet(
                    details = details,
                    omrImageProcessResult = omrImageProcessResult,
                    examId = examId
                )

                if (insertedId > 0) {
                    _saveSheetState.value = UiState.Success(insertedId)
                    val count = scanResultRepository.getCountByExamIdOnce(examId)
                    _sheetsCountByExamId.value = UiState.Success(count)
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
        viewModelScope.launch {
            try {
                scanResultRepository.getStatistics(examId).collect { stats ->
                    _examStatsCache.update { currentCache ->
                        currentCache + (examId to stats)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannedSheetsViewModel", "Error loading stats for exam $examId", e)
            }
        }
    }
}











