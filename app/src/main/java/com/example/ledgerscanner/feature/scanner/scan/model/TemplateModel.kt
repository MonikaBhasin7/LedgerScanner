package com.example.ledgerscanner.feature.scanner.scan.model

// Reuse your data classes
data class Template(
    val version: String? = "1.0",
    val notes: String? = null,
    val sheet_width: Int = 0,
    val sheet_height: Int = 0,
    val options_per_question: Int,
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
    val padding: Int? = 0
)

data class Question(
    val q_no: Int,
    val options: List<OptionBox>
)

data class OptionBox(
    val option: String,
    val x: Double,
    val y: Double,
    val r: Double
)

data class Bubble(
    val x: Double,
    val y: Double,
    val r: Double,
)