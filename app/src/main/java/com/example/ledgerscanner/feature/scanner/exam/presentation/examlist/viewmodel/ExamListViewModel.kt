package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.domain.model.QuickActionButton
import com.example.ledgerscanner.feature.scanner.exam.data.repository.ExamRepository
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _updateExamStatusState = MutableStateFlow<UiState<Unit>>(UiState.Loading())
    val updateExamStatusState = _updateExamStatusState.asStateFlow()

    private val _allExams = MutableStateFlow<List<ExamEntity>>(emptyList())
    private var currentSearchQuery: String = ""
    private var examListCollectionJob: Job? = null
    private var examListRequestToken: Long = 0L

    fun getExamList(examStatus: ExamStatus? = null) {
        examListCollectionJob?.cancel()
        val requestToken = ++examListRequestToken
        _examList.value = UiState.Loading()
        examListCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val source = if (examStatus == null) {
                    repository.getAllExams()
                } else {
                    repository.getExamByStatus(examStatus)
                }
                source.collect { exams ->
                    if (requestToken != examListRequestToken) return@collect
                    _allExams.value = exams
                    applySearch(currentSearchQuery)
                }
            } catch (_: CancellationException) {
                // Expected when switching filters quickly; avoid showing cancellation as an error.
            } catch (e: Exception) {
                if (requestToken != examListRequestToken) return@launch
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

    fun updateExamStatus(examId: Int, status: ExamStatus) {
        viewModelScope.launch {
            _updateExamStatusState.value = UiState.Loading()
            try {
                repository.updateExamStatus(examId, status)
                _updateExamStatusState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _updateExamStatusState.value =
                    UiState.Error(e.message ?: "Failed to update exam status")
            }
        }
    }

    fun resetUpdateExamStatusState() {
        _updateExamStatusState.value = UiState.Loading()
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
                    secondaryAction = if (hasScannedSheets) ExamAction.ViewResults else null,
                    tertiaryAction = if (hasScannedSheets) ExamAction.ViewReport else null
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
            ExamStatus.ARCHIVED -> ExamActionPopupConfig(
                menuItems = listOf(
                    ExamAction.Restore,
                    ExamAction.Duplicate,
                    ExamAction.Delete
                ),
                quickAction = QuickActionButton(
                    action = ExamAction.Restore,
                    style = ButtonType.SECONDARY
                )
            )
        }
    }
}
