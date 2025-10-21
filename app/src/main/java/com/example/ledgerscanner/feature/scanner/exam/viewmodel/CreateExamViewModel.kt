package com.example.ledgerscanner.feature.scanner.exam.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamDraft
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.model.TemplateSummary
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

    private val _draft = MutableStateFlow<ExamDraft?>(null)
    val draft: StateFlow<ExamDraft?> = _draft.asStateFlow()

    companion object {
        const val TAG = "CreateExamViewModel"
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
                val rowId =
                    repository.saveBasicInfo(examName, description, template, numberOfQuestions)
                _draft.value = ExamDraft(
                    examId = rowId,
                    examName = examName,
                    description = description,
                    numberOfQuestions = numberOfQuestions,
                    templateSummary = TemplateSummary(
                        template.name,
                        template.sheet_width,
                        template.sheet_height,
                        template.options_per_question
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving basic info: ${e.message}")
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