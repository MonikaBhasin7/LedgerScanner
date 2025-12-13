package com.example.ledgerscanner.feature.scanner.exam.viewmodel

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateExamViewModel @Inject constructor(val repository: ExamRepository) : ViewModel() {

    private val _examEntity = MutableStateFlow<ExamEntity?>(null)
    val examEntity: StateFlow<ExamEntity?> = _examEntity.asStateFlow()
    private val _perStepState: MutableStateFlow<Pair<ExamStep, OperationState>> =
        MutableStateFlow(Pair(ExamStep.BASIC_INFO, OperationState.Idle))
    val perStepState: StateFlow<Pair<ExamStep, OperationState>> = _perStepState.asStateFlow()

    companion object {
        const val TAG = "CreateExamViewModel"
    }

    private fun updateStepState(step: ExamStep, state: OperationState) {
        _perStepState.value = step to state
    }

    private fun changeOperationState(state: OperationState) {
        _perStepState.value = _perStepState.value.first to state
    }

    fun moveToNextStepWithIdleState() {
        updateStepState(_perStepState.value.first.next(), OperationState.Idle)
    }

    fun saveBasicInfo(
        examName: String,
        description: String?,
        template: Template,
        numberOfQuestions: Int,
        saveInDb: Boolean
    ) {
        viewModelScope.launch {
            try {
                changeOperationState(OperationState.Loading)
                _examEntity.value =
                    repository.saveBasicInfo(
                        examName,
                        description,
                        template,
                        numberOfQuestions,
                        saveInDb
                    )
                changeOperationState(OperationState.Success)
            } catch (e: Exception) {
                changeOperationState(OperationState.Error("Error saving basic info: ${e.message}"))
            }
        }
    }

    fun saveAnswerKey(answerKeys: List<Int>, saveInDb: Boolean) {
        viewModelScope.launch {
            try {
                changeOperationState(OperationState.Loading)
                val answerKeyMap = answerKeys.withIndex().associate {
                    it.index + 1 to it.value
                }
                _examEntity.value = repository.saveAnswerKey(
                    examEntity = _examEntity.value
                        ?: throw IllegalStateException("Exam entity not found"),
                    answerKeys = answerKeyMap,
                    saveInDb = saveInDb
                )
                changeOperationState(OperationState.Success)
            } catch (e: Exception) {
                changeOperationState(OperationState.Error("Error saving basic info: ${e.message}"))
            }
        }
    }
}