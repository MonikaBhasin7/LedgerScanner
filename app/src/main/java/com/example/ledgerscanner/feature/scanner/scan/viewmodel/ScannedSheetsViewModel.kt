package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import com.example.ledgerscanner.feature.scanner.scan.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannedSheetsViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository,
    private val examRepository: ExamRepository,
) : ViewModel() {
    private val _sheetsCountByExamId = MutableStateFlow<UiState<Int>>(UiState.Loading())
    val sheetsCountByExamId = _sheetsCountByExamId.asStateFlow()

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
}















