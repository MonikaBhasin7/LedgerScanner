package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.StudentDetailsForScanResult
import com.example.ledgerscanner.feature.scanner.scan.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannedSheetsViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository
) : ViewModel() {

    private val _sheetsCountByExamId = MutableStateFlow<UiState<Int>>(UiState.Loading())
    val sheetsCountByExamId = _sheetsCountByExamId.asStateFlow()

    private val _saveSheetState = MutableStateFlow<UiState<Long>>(UiState.Idle())
    val saveSheetState = _saveSheetState.asStateFlow()

    fun getCountByExamId(examId: Int) {
        viewModelScope.launch {
            try {
                _sheetsCountByExamId.value = UiState.Loading()
                scanResultRepository.getCountByExamId(examId).collect {
                    _sheetsCountByExamId.value = UiState.Success(it)
                }
            } catch (e: Exception) {
                _sheetsCountByExamId.value = UiState.Error("Something went wrong")
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
                android.util.Log.e("ScannedSheetsViewModel", "Validation error", e)
                _saveSheetState.value = UiState.Error(e.message ?: "Invalid data")
            } catch (e: IllegalStateException) {
                android.util.Log.e("ScannedSheetsViewModel", "State error", e)
                _saveSheetState.value = UiState.Error(e.message ?: "Invalid state")
            } catch (e: Exception) {
                android.util.Log.e("ScannedSheetsViewModel", "Error saving sheet", e)
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
}












