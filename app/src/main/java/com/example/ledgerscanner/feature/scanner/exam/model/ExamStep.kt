package com.example.ledgerscanner.feature.scanner.exam.model

enum class ExamStep(val title: String) {
    BASIC_INFO("Basic Info"),
    ANSWER_KEY("Answer Key"),
    MARKING("Marking"),
    REVIEW("Review");

    fun next(): ExamStep = when (this) {
        BASIC_INFO -> ANSWER_KEY
        ANSWER_KEY -> MARKING
        MARKING -> REVIEW
        REVIEW -> REVIEW
    }

    fun prev(): ExamStep = when (this) {
        BASIC_INFO -> BASIC_INFO
        ANSWER_KEY -> BASIC_INFO
        MARKING -> ANSWER_KEY
        REVIEW -> MARKING
    }
}