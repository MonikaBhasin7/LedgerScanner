package com.example.ledgerscanner.database

import androidx.room.TypeConverter
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class TypeConverter {
    private val gson = Gson()

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
    fun fromAnswerKey(map: Map<Int, Int>?): String {
        return Gson().toJson(map ?: emptyMap<Int, Int>())
    }

    @TypeConverter
    fun toAnswerKey(json: String?): Map<Int, Int> {
        if (json.isNullOrEmpty()) return emptyMap()

        val type = object : TypeToken<Map<Int, Int>>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun fromConfidenceMap(value: Map<Int, Float>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toConfidenceMap(value: String?): Map<Int, Float>? {
        return value?.let {
            val type = object : TypeToken<Map<Int, Float>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // List<Int> for multiple marks
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return value?.let {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(it, type)
        }
    }
}