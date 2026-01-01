package com.example.ledgerscanner.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "scan_results",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examId"])]
)
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val examId: Int,

    // Student Info
    val barCode: String?,

    // Scan Info
    val scannedImagePath: String,
    val thumbnailPath: String? = null,
    val scannedAt: Long = System.currentTimeMillis(),

    // ✅ ALL ANSWERS IN ONE MAP
    val studentAnswers: Map<Int, List<Int>>, // {1: 0, 2: 1, 3: 2, ...}

    // ✅ ISSUES IN ONE MAP (optional)
    val multipleMarksDetected: List<Int>? = null, // [7, 12, 23] - question numbers with multiple marks

    // Score Summary
    val score: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val blankCount: Int,
    val scorePercent: Float,

   // NEW: Confidence tracking
    val questionConfidences: Map<Int, Double>? = null,
    val avgConfidence: Double? = null,
    val minConfidence: Double? = null,
    val lowConfidenceQuestions: List<Int>? = null
) : Parcelable