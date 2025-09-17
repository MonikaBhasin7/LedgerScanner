package com.example.ledgerscanner.database

import androidx.room.TypeConverter
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import java.util.Date

class TypeConverter {
    @TypeConverter
    fun fromExamStatus(value: ExamStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toExamStatus(value: String?): ExamStatus? {
        return value?.let { ExamStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}