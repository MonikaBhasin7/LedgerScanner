package com.example.ledgerscanner.feature.scanner.scan.model

data class Template(
    val version: String?,
    val sheet_width: Int,
    val sheet_height: Int,
    val options_per_question: Int,
    val bubble_diameter: Int?,
    val grid: Grid?,
    val questions: List<Question>
)

data class Grid(
    val start_x: Int?,
    val start_y: Int?,
    val col_spacing: Int?,
    val row_spacing: Int?,
    val bubble_w: Int?,
    val bubble_h: Int?,
    val padding: Int?
)

data class Question(
    val q_no: Int,
    val options: List<OptionBox>
)

data class OptionBox(
    val option: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int
)

data class BubbleResult(
    val question: Int,
    val option: String,
    val filled: Boolean,
    val confidence: Double
)