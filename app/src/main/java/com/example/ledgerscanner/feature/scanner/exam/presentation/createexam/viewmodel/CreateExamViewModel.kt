package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.data.repository.ExamRepository
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

    fun updateStepState(step: ExamStep, state: OperationState) {
        _perStepState.value = step to state
    }

    fun changeOperationState(state: OperationState) {
        _perStepState.value = _perStepState.value.first to state
    }

    fun moveToNextStepWithIdleState() {
        updateStepState(_perStepState.value.first.next(), OperationState.Idle)
    }

    fun setExamEntity(examEntity: ExamEntity) {
        _examEntity.value = examEntity
    }

    fun saveBasicInfo(
        examName: String,
        description: String?,
        template: Template,
        numberOfQuestions: Int
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
                        existingExam = _examEntity.value,
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
                    it.index to it.value
                }
                _examEntity.value = repository.saveAnswerKey(
                    examEntity = _examEntity.value
                        ?: throw IllegalStateException("Exam entity not found"),
                    answerKeys = answerKeyMap,
                )
                changeOperationState(OperationState.Success)
            } catch (e: Exception) {
                changeOperationState(OperationState.Error("Error saving answer key: ${e.message}"))
            }
        }
    }

    fun saveMarkingScheme(
        marksPerCorrect: String,
        marksPerWrong: String,
        negativeMarking: Boolean
    ) {
        viewModelScope.launch {
            try {
                changeOperationState(OperationState.Loading)

                val correctMarks = marksPerCorrect.toFloatOrNull()
                val wrongMarks = marksPerWrong.toFloatOrNull()

                if (correctMarks == null || wrongMarks == null) {
                    changeOperationState(OperationState.Error("Invalid marks value"))
                    return@launch
                }

                _examEntity.value = repository.saveMarkingScheme(
                    examEntity = _examEntity.value
                        ?: throw IllegalStateException("Exam entity not found"),
                    marksPerCorrect = correctMarks,
                    marksPerWrong = wrongMarks,
                    negativeMarking = negativeMarking
                )
                changeOperationState(OperationState.Success)
            } catch (e: Exception) {
                changeOperationState(OperationState.Error("Error saving marking scheme: ${e.message}"))
            }
        }
    }

    fun finalizeExam() {
        viewModelScope.launch {
            try {
                changeOperationState(OperationState.Loading)

                val exam = _examEntity.value
                    ?: throw IllegalStateException("Exam entity not found")

                val answerKeySize = exam.answerKey?.size ?: 0
                if (answerKeySize < exam.totalQuestions) {
                    changeOperationState(
                        OperationState.Error(
                            "Answer key incomplete: $answerKeySize of ${exam.totalQuestions} questions configured"
                        )
                    )
                    return@launch
                }

                if (exam.marksPerCorrect == null || exam.marksPerCorrect <= 0f) {
                    changeOperationState(OperationState.Error("Marks per correct answer must be set"))
                    return@launch
                }

                val finalizedExam = exam.copy(status = ExamStatus.ACTIVE)
                repository.saveExam(finalizedExam)
                _examEntity.value = finalizedExam

                changeOperationState(OperationState.Success)
            } catch (e: Exception) {
                changeOperationState(OperationState.Error("Error creating exam: ${e.message}"))
            }
        }
    }
}