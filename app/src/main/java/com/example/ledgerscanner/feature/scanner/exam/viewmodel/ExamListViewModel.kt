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
import javax.inject.Inject

@HiltViewModel
class ExamListViewModel @Inject constructor(val repository: ExamRepository) : ViewModel() {

    private val _examList = MutableStateFlow<UiState<List<ExamEntity>>>(UiState.Loading())
    val examList: MutableStateFlow<UiState<List<ExamEntity>>> = _examList

    private val _allExams = MutableStateFlow<List<ExamEntity>>(emptyList())
    private var currentSearchQuery: String = ""

    fun getExamList(examStatus: ExamStatus? = null) {
        viewModelScope.launch {
            _examList.value = UiState.Loading()

            try {
                withContext(Dispatchers.IO) {
                    if (examStatus == null) {
                        repository.getAllExams().collect { exams ->
                            _allExams.value = exams
                            applySearch(currentSearchQuery)
                        }
                    } else {
                        repository.getExamByStatus(examStatus).collect { exams ->
                            _allExams.value = exams
                            applySearch(currentSearchQuery)
                        }
                    }
                }
            } catch (e: Exception) {
                _examList.value = UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun searchExam(query: String) {
        currentSearchQuery = query
        applySearch(query)
    }

    private fun applySearch(query: String) {
        viewModelScope.launch {
            try {
                if (query.isEmpty()) {
                    _examList.value = UiState.Success(_allExams.value)
                } else {
                    val filteredList = _allExams.value.filter { item ->
                        item.examName.contains(query, ignoreCase = true) ||
                                item.description?.contains(query, ignoreCase = true) == true
                    }
                    _examList.value = UiState.Success(filteredList)
                }
            } catch (e: Exception) {
                _examList.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}