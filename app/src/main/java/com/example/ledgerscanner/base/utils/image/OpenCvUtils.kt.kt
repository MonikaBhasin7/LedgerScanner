package com.example.ledgerscanner.base.utils.image

import android.util.Log
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.results.model.AnswerStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object OpenCvUtils {

    private const val TAG = "OpenCvUtils"

    // Color constants for evaluation
    private val COLOR_CORRECT = Scalar(0.0, 255.0, 0.0)      // Green
    private val COLOR_INCORRECT = Scalar(0.0, 0.0, 255.0)    // Red
    private val COLOR_MULTIPLE = Scalar(255.0, 165.0, 0.0)   // Orange
    private val COLOR_WHITE = Scalar(255.0, 255.0, 255.0)    // White

    /**
     * Existing drawPoints function - kept for backward compatibility
     */
    fun drawPoints(
        src: Mat,
        points: List<AnchorPoint>? = null,
        bubbles: List<Bubble>? = null,
        bubbles2DArray: List<List<Bubble>>? = null,
        enrollmentBubbles: List<List<Bubble>>? = null,
        fillColor: Scalar = Scalar(255.0, 0.0, 0.0),
        textColor: Scalar = Scalar(255.0, 255.0, 0.0),
        radius: Int? = 10,
    ): Mat {
        val out = src.clone()

        fun draw(
            pt: Point,
            label: String? = null,
            filledColor: Scalar = fillColor,
            pointRadius: Int? = radius
        ) {
            Imgproc.circle(
                out,
                pt,
                pointRadius ?: 10,
                filledColor,
                -1
            )
            label?.let {
                Imgproc.putText(
                    out,
                    it,
                    Point(pt.x - 4, pt.y + 2),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    textColor,
                    2
                )
            }
        }

        // 1) Draw enrollment bubbles in cyan/teal color (distinct from answer bubbles)
        val enrollmentFillColor = Scalar(0.0, 255.0, 255.0) // Cyan
        val enrollmentTextColor = Scalar(0.0, 0.0, 0.0)     // Black text
        enrollmentBubbles?.forEachIndexed { colIdx, column ->
            column.forEachIndexed { digitIdx, b ->
                Imgproc.circle(out, Point(b.x, b.y), radius ?: 10, enrollmentFillColor, -1)
                // Label: "C0D3" means Column 0, Digit 3
                val label = "C${colIdx}D${digitIdx}"
                Imgproc.putText(
                    out, label, Point(b.x - 12, b.y + 3),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.35, enrollmentTextColor, 1
                )
            }
        }

        // 2) Draw 2D bubble grid (row-wise), label with running index
        var idx = 0
        bubbles2DArray?.forEach { row ->
            row.forEach { b ->
                draw(Point(b.x, b.y), "${idx++}")
            }
        }

        // 3) Draw simple point list
        points?.forEachIndexed { i, p -> draw(p.toPoint(), "$i") }

        // 4) Draw flat bubble list
        bubbles?.forEachIndexed { i, b -> draw(Point(b.x, b.y), "$i") }
        return out
    }

    /**
     * NEW: Draws detected bubbles with evaluation-based coloring
     *
     * @param src Source Mat (should be in color, BGR format)
     * @param bubbles List of detected bubbles with question/option info
     * @param evaluation Evaluation result containing correctness information
     * @param radius Circle radius for drawing
     * @param thickness Circle border thickness
     * @param showText Whether to show question numbers on bubbles
     * @return Mat with drawn annotations
     */
    fun drawPointsWithEvaluation(
        src: Mat,
        bubbles: List<BubbleResult>,
        evaluation: EvaluationResult,
        radius: Int = 12,
        thickness: Int = 2,
        showText: Boolean = true
    ): Mat {
        if (src.empty()) {
            Log.w(TAG, "Empty Mat provided to drawPointsWithEvaluation")
            return src
        }

        val out = src.clone()

        try {
            // Group bubbles by question to handle multiple marks
            val bubblesByQuestion = bubbles.groupBy { it.questionIndex }

            bubblesByQuestion.forEach { (questionIndex, questionBubbles) ->
                val answerModel = evaluation.answerMap[questionIndex]
                val status = answerModel?.getStatus() ?: AnswerStatus.UNANSWERED

                // Determine color based on status
                val color = when (status) {
                    AnswerStatus.CORRECT -> COLOR_CORRECT
                    AnswerStatus.INCORRECT -> COLOR_INCORRECT
                    AnswerStatus.MULTIPLE_MARKS -> COLOR_MULTIPLE
                    AnswerStatus.UNANSWERED -> COLOR_INCORRECT // Shouldn't happen, but mark red
                }

                // Draw each bubble for this question
                questionBubbles.forEach { bubble ->
                    val point = Point(bubble.point.x, bubble.point.y)

                    // Draw filled circle
                    Imgproc.circle(
                        out,
                        point,
                        radius,
                        color,
                        -1 // Filled
                    )

                    // Draw white border for visibility
                    Imgproc.circle(
                        out,
                        point,
                        radius,
                        COLOR_WHITE,
                        thickness
                    )

                    // Draw question number and option
                    if (showText) {
                        val text = "Q${questionIndex + 1}:${getOptionLetter(bubble.optionIndex)}"
                        val textPoint = Point(
                            point.x + radius + 5,
                            point.y + 5
                        )

                        Imgproc.putText(
                            out,
                            text,
                            textPoint,
                            Imgproc.FONT_HERSHEY_SIMPLEX,
                            0.5,
                            COLOR_WHITE,
                            2
                        )
                    }
                }

                // For multiple marks, add warning indicator
                if (status == AnswerStatus.MULTIPLE_MARKS && questionBubbles.size > 1) {
                    val firstBubble = questionBubbles.first()
                    val warningPoint = Point(
                        firstBubble.point.x - radius - 20,
                        firstBubble.point.y
                    )

                    Imgproc.putText(
                        out,
                        "!",
                        warningPoint,
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        1.2,
                        COLOR_MULTIPLE,
                        3
                    )
                }
            }

            return out

        } catch (e: Exception) {
            Log.e(TAG, "Error drawing points with evaluation", e)
            return out
        }
    }

    /**
     * NEW: Draws evaluation summary overlay on the image
     *
     * @param src Source Mat
     * @param evaluation Evaluation result
     * @param x X coordinate for summary text (top-left)
     * @param y Y coordinate for summary text (top-left)
     * @return Mat with summary overlay
     */
    fun drawEvaluationSummary(
        src: Mat,
        evaluation: EvaluationResult,
        x: Int = 20,
        y: Int = 40
    ): Mat {
        if (src.empty()) {
            Log.w(TAG, "Empty Mat provided to drawEvaluationSummary")
            return src
        }

        val out = src.clone()

        try {
            val summaryLines = listOf(
                "Score: ${evaluation.getMarksFormatted()} (${evaluation.getPercentageFormatted()})",
                "Grade: ${evaluation.getGrade()}",
                "Correct: ${evaluation.correctCount}",
                "Incorrect: ${evaluation.incorrectCount}",
                "Unanswered: ${evaluation.unansweredCount}",
                "Multiple: ${evaluation.multipleMarksQuestions.size}"
            )

            // Draw background rectangle for better visibility
            val bgHeight = summaryLines.size * 35 + 20
            Imgproc.rectangle(
                out,
                Point((x - 10).toDouble(), (y - 30).toDouble()),
                Point((x + 350).toDouble(), (y + bgHeight - 30).toDouble()),
                Scalar(0.0, 0.0, 0.0, 0.7), // Semi-transparent black
                -1
            )

            // Draw text lines
            summaryLines.forEachIndexed { index, line ->
                val textPoint = Point(x.toDouble(), (y + index * 35).toDouble())

                Imgproc.putText(
                    out,
                    line,
                    textPoint,
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    COLOR_WHITE,
                    2
                )
            }

            return out

        } catch (e: Exception) {
            Log.e(TAG, "Error drawing evaluation summary", e)
            return out
        }
    }

    /**
     * Converts option index to letter (0->A, 1->B, 2->C, etc.)
     */
    private fun getOptionLetter(optionIndex: Int): String {
        return ('A' + optionIndex).toString()
    }

    /**
     * Detects a square anchor marker within a Region of Interest.
     * Proper cleanup of ALL OpenCV Mats to prevent memory leaks.
     */
    fun detectAnchorInRoi(roiGray: Mat): Point? {
        val bin = Mat()
        val hierarchy = Mat()
        val contours = mutableListOf<MatOfPoint>()

        try {
            Imgproc.threshold(
                roiGray,
                bin, 60.0,
                255.0,
                Imgproc.THRESH_BINARY_INV
            )

            Imgproc.findContours(
                bin,
                contours,
                hierarchy,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            val roiW = roiGray.cols()
            val roiH = roiGray.rows()
            val roiArea = (roiW * roiH).toDouble()
            val minArea = 0.002 * roiArea
            val maxArea = 0.25 * roiArea

            for (c in contours) {
                // Create and release temp mats in each iteration
                val mp2f = MatOfPoint2f(*c.toArray())
                val peri = Imgproc.arcLength(mp2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(mp2f, approx, 0.04 * peri, true)
                mp2f.release() // Release immediately after use

                if (approx.total() != 4L) {
                    approx.release()
                    continue
                }

                val approxMP = MatOfPoint(*approx.toArray())
                approx.release() // Release approx — data copied to approxMP

                if (!Imgproc.isContourConvex(approxMP)) {
                    approxMP.release()
                    continue
                }

                val rect = Imgproc.boundingRect(approxMP)
                val aspect = rect.width.toDouble() / rect.height.toDouble()
                val area = Imgproc.contourArea(approxMP)
                val rectArea = (rect.width * rect.height).toDouble().coerceAtLeast(1.0)
                val solidity = area / rectArea

                val peri2Mp = MatOfPoint2f(*approxMP.toArray())
                val peri2 = Imgproc.arcLength(peri2Mp, true)
                peri2Mp.release()

                val circularity =
                    if (peri2 > 1e-6) 4.0 * Math.PI * area / (peri2 * peri2) else 0.0

                if (aspect in 0.8..1.25 && solidity > 0.70 && area in minArea..maxArea && circularity < 0.85) {
                    val cx = rect.x + rect.width / 2.0
                    val cy = rect.y + rect.height / 2.0
                    approxMP.release()
                    return Point(cx, cy)
                }

                approxMP.release()
            }

            return null

        } finally {
            bin.release()
            hierarchy.release()
            // Contour Mats are managed by OpenCV findContours — no explicit release needed
        }
    }
}

/**
 * Extension function to convert AnchorPoint to OpenCV Point
 */
fun AnchorPoint.toPoint(): Point = Point(this.x, this.y)