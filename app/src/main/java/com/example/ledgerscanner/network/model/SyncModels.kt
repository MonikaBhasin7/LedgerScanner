package com.example.ledgerscanner.network.model

import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.google.gson.Gson

data class ExamSyncRequest(
    val localExamEntityId: String,
    val instituteId: Long,
    val memberId: Long,
    val examName: String,
    val description: String?,
    val status: String,
    val totalQuestions: Int,
    val templateJson: String,
    val answerKeyJson: String?,
    val marksPerCorrect: Float?,
    val marksPerWrong: Float?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        private val gson = Gson()

        fun from(entity: ExamEntity, instituteId: Long, memberId: Long): ExamSyncRequest {
            return ExamSyncRequest(
                localExamEntityId = entity.id.toString(),
                instituteId = instituteId,
                memberId = memberId,
                examName = entity.examName,
                description = entity.description,
                status = entity.status.name,
                totalQuestions = entity.totalQuestions,
                templateJson = gson.toJson(entity.template),
                answerKeyJson = entity.answerKey?.let { gson.toJson(it) },
                marksPerCorrect = entity.marksPerCorrect,
                marksPerWrong = entity.marksPerWrong,
                createdAt = entity.createdAt,
                updatedAt = entity.createdAt
            )
        }
    }
}

data class ScanResultSyncRequest(
    val localScanResultEntityId: String,
    val instituteId: Long,
    val memberId: Long,
    val localExamEntityId: String,
    val barCode: String?,
    val clickedRawImageFile: String,
    val scannedImageFile: String,
    val thumbnailFile: String?,
    val debugImages: Map<String, String>?,
    val scannedAt: Long,
    val studentAnswersJson: String,
    val multipleMarksJson: String?,
    val score: Float,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val blankCount: Int,
    val scorePercent: Float,
    val questionConfidencesJson: String?,
    val avgConfidence: Double?,
    val minConfidence: Double?,
    val lowConfidenceJson: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        private val gson = Gson()

        fun from(
            entity: ScanResultEntity,
            instituteId: Long,
            memberId: Long,
            clickedRawImageFile: String,
            scannedImageFile: String,
            thumbnailFile: String?,
            debugImages: Map<String, String>?
        ): ScanResultSyncRequest {
            return ScanResultSyncRequest(
                localScanResultEntityId = entity.id.toString(),
                instituteId = instituteId,
                memberId = memberId,
                localExamEntityId = entity.examId.toString(),
                barCode = entity.barCode,
                clickedRawImageFile = clickedRawImageFile,
                scannedImageFile = scannedImageFile,
                thumbnailFile = thumbnailFile,
                debugImages = debugImages,
                scannedAt = entity.scannedAt,
                studentAnswersJson = gson.toJson(entity.studentAnswers),
                multipleMarksJson = entity.multipleMarksDetected?.let { gson.toJson(it) },
                score = entity.score,
                totalQuestions = entity.totalQuestions,
                correctCount = entity.correctCount,
                wrongCount = entity.wrongCount,
                blankCount = entity.blankCount,
                scorePercent = entity.scorePercent,
                questionConfidencesJson = entity.questionConfidences?.let { gson.toJson(it) },
                avgConfidence = entity.avgConfidence,
                minConfidence = entity.minConfidence,
                lowConfidenceJson = entity.lowConfidenceQuestions?.let { gson.toJson(it) },
                createdAt = entity.scannedAt,
                updatedAt = entity.scannedAt
            )
        }
    }
}

data class SyncResponse(
    val localId: String,
    val serverId: Long,
    val status: String? = null,
    val serverUpdatedAt: Long? = null
)
