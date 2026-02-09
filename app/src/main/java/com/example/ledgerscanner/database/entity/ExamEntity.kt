package com.example.ledgerscanner.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val examName: String,
    val description: String?,
    val status: ExamStatus,
    val totalQuestions: Int,
    val template: Template,
    val answerKey: Map<Int, Int>? = null,
    val marksPerCorrect: Float? = null,
    val marksPerWrong: Float? = null,
    val createdAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
) : Parcelable {
    fun getMaxMarks(): Float {
        return totalQuestions * (marksPerCorrect ?: 0f)
    }
}