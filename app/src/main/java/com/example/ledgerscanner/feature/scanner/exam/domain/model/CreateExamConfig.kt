package com.example.ledgerscanner.feature.scanner.exam.domain.model

import android.os.Parcelable
import com.example.ledgerscanner.database.entity.ExamEntity
import kotlinx.parcelize.Parcelize


@Parcelize
data class CreateExamConfig(
    val examEntity: ExamEntity,
    val mode: Mode = Mode.EDIT,
    val targetScreen: ExamStep = ExamStep.BASIC_INFO
) : Parcelable {
    enum class Mode {
        EDIT, VIEW
    }
}
