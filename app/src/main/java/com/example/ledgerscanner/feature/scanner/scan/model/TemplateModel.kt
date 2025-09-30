package com.example.ledgerscanner.feature.scanner.scan.model

// Reuse your data classes
data class Template(
    val version: String? = "1.0",
    val notes: String? = null,
    val sheet_width: Double = 0.0,
    val sheet_height: Double = 0.0,
    val options_per_question: Int,
    val grid: Grid?,
    val questions: List<Question>,
    val anchor_top_left: org.opencv.core.Point,
    val anchor_top_right: org.opencv.core.Point,
    val anchor_bottom_right: org.opencv.core.Point,
    val anchor_bottom_left: org.opencv.core.Point,
) {
    private var totalBubbles: Int? = null
    fun totalBubbles(): Int? {
        if (totalBubbles == null) {
            totalBubbles = questions.size * 4 //todo monika make it generic in future
        }
        return totalBubbles
    }
}

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