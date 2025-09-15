package com.example.ledgerscanner.feature.scanner.exam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.feature.scanner.exam.model.ExamItem
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ExamListViewModel : ViewModel() {

    private val _examList = MutableStateFlow<UiState<List<ExamItem>>>(UiState.Loading())
    val examList: MutableStateFlow<UiState<List<ExamItem>>> = _examList

    fun getExamList(examFilter: ExamStatus?) {
        viewModelScope.launch {
            _examList.value = UiState.Loading()
            delay(1000)
            val mockExams = List(10) { index ->
                ExamItem(
                    id = "exam_${index + 1}",
                    title = "Exam ${index + 1}",
                    status = if (index % 2 == 0) ExamStatus.Completed else ExamStatus.Processing,
                    totalQuestions = 50,
                    createdDate = "2025-09-${10 + index}",
                    sheetsCount = (100..200).random(),
                    avgScorePercent = (60..80).random(),
                    topScorePercent = (85..100).random(),
                    medianScorePercent = (55..75).random()
                )
            }
            _examList.value = UiState.Success(mockExams)
        }
    }
}