package com.example.ledgerscanner.database

import androidx.room.TypeConverter
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class TypeConverter {

    // ---------------- ExamStatus Enum ----------------
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

    // ---------------- Template ----------------
    @TypeConverter
    fun fromTemplate(template: Template?): String {
        return Gson().toJson(template)
    }

    @TypeConverter
    fun toTemplate(templateString: String?): Template? {
        if (templateString.isNullOrEmpty()) return null
        return Gson().fromJson(templateString, Template::class.java)
    }

    // ---------------- Answer Key Map<Int, String> ----------------
    @TypeConverter
    fun fromAnswerKey(map: Map<Int, String>?): String {
        return Gson().toJson(map)
    }

    @TypeConverter
    fun toAnswerKey(json: String?): Map<Int, String> {
        if (json.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<Int, String>>() {}.type
        return Gson().fromJson(json, type)
    }
}