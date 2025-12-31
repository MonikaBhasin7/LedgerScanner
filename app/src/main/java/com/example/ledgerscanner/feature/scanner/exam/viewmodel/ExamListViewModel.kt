package com.example.ledgerscanner.feature.scanner.exam.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.model.QuickActionButton
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import com.example.ledgerscanner.feature.scanner.scan.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExamListViewModel @Inject constructor(
    val repository: ExamRepository,
) : ViewModel() {

    private val _examList = MutableStateFlow<UiState<List<ExamEntity>>>(UiState.Loading())
    val examList: MutableStateFlow<UiState<List<ExamEntity>>> = _examList

    private val _deleteExamState = MutableStateFlow<UiState<Unit>>(UiState.Loading())
    val deleteExamState = _deleteExamState.asStateFlow()

    private val _duplicateExamState = MutableStateFlow<UiState<Unit>>(UiState.Loading())
    val duplicateExamState = _duplicateExamState.asStateFlow()

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

    fun deleteExam(examId: Int) {
        viewModelScope.launch {
            _deleteExamState.value = UiState.Loading()
            try {
                repository.deleteExam(examId)
                _deleteExamState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _deleteExamState.value = UiState.Error(e.message ?: "Failed to delete exam")
            }
        }
    }

    fun duplicateExam(exam: ExamEntity) {
        viewModelScope.launch {
            _duplicateExamState.value = UiState.Loading()
            try {
                repository.duplicateExam(exam)
                _duplicateExamState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _duplicateExamState.value = UiState.Error(e.message ?: "Failed to duplicate exam")
            }
        }
    }

    fun resetDeleteState() {
        _deleteExamState.value = UiState.Loading()
    }

    fun resetDuplicateState() {
        _duplicateExamState.value = UiState.Loading()
    }

    fun getExamActionForStatus(
        status: ExamStatus,
        hasScannedSheets: Boolean = false
    ): ExamActionPopupConfig {
        return when (status) {
            ExamStatus.DRAFT -> ExamActionPopupConfig(
                menuItems = listOf(
                    ExamAction.Duplicate,
                    ExamAction.Delete
                ),
                quickAction = QuickActionButton(
                    action = ExamAction.ContinueSetup,
                    style = ButtonType.WARNING,
                ),
            )

            ExamStatus.ACTIVE -> ExamActionPopupConfig(
                menuItems = buildList {
                    add(ExamAction.MarkCompleted)
                    add(ExamAction.EditExam)
                    add(ExamAction.Duplicate)
                    add(ExamAction.Delete)
                },
                quickAction = QuickActionButton(
                    action = ExamAction.ScanSheets,
                    style = ButtonType.PRIMARY,
                    secondaryAction = if (hasScannedSheets) ExamAction.ViewResults else null
                ),
            )

            ExamStatus.COMPLETED -> ExamActionPopupConfig(
                menuItems = listOf(
                    ExamAction.Duplicate,
                    ExamAction.Archive,
                    ExamAction.Delete
                ),
                quickAction = QuickActionButton(
                    action = ExamAction.ViewResults,
                    style = ButtonType.SECONDARY, // Outlined
                    secondaryAction = ExamAction.ExportResults
                )
            )


//            ExamStatus.ARCHIVED -> ExamActionPopupConfig(
//                menuItems = listOf(
//                    ExamAction.UnarchiveExam,
//                    ExamAction.ViewResults,
//                    ExamAction.ExportResults,
//                    ExamAction.DeleteExam
//                ),
//                quickAction = QuickActionButton(
//                    action = ExamAction.ViewResults,
//                    style = ButtonStyle.NEUTRAL // Grey outlined
//                )
//            )
            ExamStatus.ARCHIVED -> TODO()
        }
    }
}