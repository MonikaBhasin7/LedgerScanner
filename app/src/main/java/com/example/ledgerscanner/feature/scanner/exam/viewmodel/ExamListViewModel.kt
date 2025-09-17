package com.example.ledgerscanner.feature.scanner.exam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ExamListViewModel @Inject constructor(val repository: ExamRepository) : ViewModel() {

    private val _examList = MutableStateFlow<UiState<List<ExamEntity>>>(UiState.Loading())
    val examList: MutableStateFlow<UiState<List<ExamEntity>>> = _examList

    //todo monika delete later
    init {
        viewModelScope.launch {
            val mockExams = List(10) { index ->
                ExamEntity(
                    id = 0,
                    title = "Exam ${index + 1}",
                    status = if (index % 2 == 0) ExamStatus.Completed else ExamStatus.Processing,
                    totalQuestions = (30..100).random(),
                    createdDate = Calendar.getInstance().time,
                    sheetsCount = (50..200).random(),
                    avgScorePercent = (40..90).random(),
                    topScorePercent = (70..100).random(),
                    medianScorePercent = (30..80).random()
                )
            }
            repository.saveExams(mockExams) // call insertAll in DAO
        }
    }

    fun getExamList(examStatus: ExamStatus?) {
        viewModelScope.launch {
            _examList.value = UiState.Loading()

            try {
                withContext(Dispatchers.IO) {
                    if(examStatus == null) {
                        repository.getAllExams().collect {
                            _examList.value = UiState.Success(it)
                        }
                    } else {
                        repository.getExamByStatus(examStatus).collect {
                            _examList.value = UiState.Success(it)
                        }
                    }
                }
            } catch (e: Exception) {
                _examList.value = UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }
}