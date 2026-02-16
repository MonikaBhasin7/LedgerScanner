package com.example.ledgerscanner.feature.scanner.statistics.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 08/01/26
// ===========================================================================
@HiltViewModel
class ExamStatisticsViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository
) : ViewModel() {

    private val _statistics = MutableStateFlow(ExamStatistics())
    val statistics: StateFlow<ExamStatistics> = _statistics.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStatistics(examId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            scanResultRepository.getStatistics(examId).collect { stats ->
                _statistics.value = stats
                _isLoading.value = false
            }
        }
    }
}