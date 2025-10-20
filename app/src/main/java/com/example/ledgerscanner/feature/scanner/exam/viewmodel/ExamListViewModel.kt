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

    fun getExamList(examStatus: ExamStatus?) {
        viewModelScope.launch {
            _examList.value = UiState.Loading()

            try {
                withContext(Dispatchers.IO) {
                    if (examStatus == null) {
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