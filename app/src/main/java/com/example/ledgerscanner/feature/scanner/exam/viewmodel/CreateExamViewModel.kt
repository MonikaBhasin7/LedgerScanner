package com.example.ledgerscanner.feature.scanner.exam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
        MutableStateFlow(Pair(ExamStep.ANSWER_KEY, OperationState.Idle))
    val perStepState: StateFlow<Pair<ExamStep, OperationState>> = _perStepState.asStateFlow()

    companion object {
        const val TAG = "CreateExamViewModel"
    }

    private fun updateStepState(step: ExamStep, state: OperationState) {
        _perStepState.value = step to state
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
        val step = ExamStep.BASIC_INFO
        viewModelScope.launch {
            updateStepState(step, OperationState.Loading)
            delay(3000) //todo monika remove
            try {
                _examEntity.value =
                    repository.saveBasicInfo(
                        examName,
                        description,
                        template,
                        numberOfQuestions,
                        saveInDb
                    )
                updateStepState(step, OperationState.Success)
            } catch (e: Exception) {
                updateStepState(
                    step,
                    OperationState.Error("Error saving basic info: ${e.message}")
                )
            }
        }
    }

    private suspend fun saveExam(
        examName: String,
        description: String?,
        template: Template,
        numberOfQuestions: Int
    ): Long {
        // build the entity
        val exam = ExamEntity(
            id = 0, // autoGenerate
            examName = examName,
            status = ExamStatus.DRAFT,
            totalQuestions = numberOfQuestions,
            template = template,
            answerKey = emptyMap(),
            marksPerCorrect = 1f,
            marksPerWrong = 0f,
            createdAt = System.currentTimeMillis(),
            sheetsCount = 0,
            avgScorePercent = null,
            topScorePercent = null,
            medianScorePercent = null
        )

        // persist via repository (assumes repository.saveExam returns Long)
        return repository.saveExam(exam)
    }
}