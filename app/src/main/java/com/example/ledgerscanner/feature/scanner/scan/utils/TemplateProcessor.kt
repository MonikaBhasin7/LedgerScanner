package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.OmrUtils
import com.example.ledgerscanner.base.utils.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import com.example.ledgerscanner.feature.scanner.scan.model.OptionBox
import com.example.ledgerscanner.feature.scanner.scan.model.Question
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.model.TemplatePair
import com.example.ledgerscanner.feature.scanner.scan.model.OmrTemplateResult
import com.google.gson.Gson
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.jvm.Throws
import kotlin.math.abs
import kotlin.math.roundToInt

class TemplateProcessor {

    companion object {
        val TAG = "TemplateProcessor"
    }

    @WorkerThread
    fun generateTemplateJson(
        inputBitmap: Bitmap,
        debug: Boolean = true
    ): OmrTemplateResult {
        val debugMap = mutableMapOf<String, Bitmap>()
        try {
            // 1. Convert bitmap to grayscale
            val srcMat = Mat().apply {
                Utils.bitmapToMat(inputBitmap, this)
            }
            val grayMat = Mat().apply {
                Imgproc.cvtColor(srcMat, this, Imgproc.COLOR_BGR2GRAY)
                if (debug) debugMap["gray"] = this.toBitmapSafe()
            }

            // 2. Detect 4 anchor squares
            val anchorPoints: List<Point> = detectAnchorPoints(
                srcMat,
                grayMat,
                debug,
                debugMapAdditionCallback = { title, bitmap ->
                    debugMap[title] = bitmap
                },
                failedCallback = { reason ->
                    return OmrTemplateResult(
                        success = false,
                        reason = reason,
                        debugBitmaps = debugMap
                    )
                },
            ) ?: listOf()

            // 3. Detect bubble centers
            val bubbles = detectAndFetchBubblesWithinAnchors(
                grayMat,
                anchorPoints,
                debug,
                debugMapAdditionCallback = { title, bitmap ->
                    debugMap[title] = bitmap
                },
            )

            // 4. Make grid of bubbles
            val bubbles2DArray = sortBubblesColumnWise(bubbles)

            // 5. generate template json
            val templatePair = generateTemplateJsonSimple(
                anchorPoints,
                bubbles2DArray,
                srcMat.size()
            )

            if (debug) {
                OmrUtils.drawPoints(
                    srcMat,
                    bubbles2DArray = bubbles2DArray,
                    points = anchorPoints,
                    radius = (templatePair.template?.questions?.firstOrNull()?.options?.firstOrNull()?.r)
                        ?.roundToInt()
                ).apply {
                    debugMap["Bubbles - Anchors"] = toBitmapSafe()
                }
            }

            return OmrTemplateResult(
                success = true,
                debugBitmaps = debugMap,
                templateJson = templatePair.templateJson,
                template = templatePair.template
            )
        } catch (e: Exception) {
            return OmrTemplateResult(
                success = false,
                e.toString(),
                debugBitmaps = debugMap,
            )
        }
    }

    @Throws
    private inline fun detectAnchorPoints(
        srcMat: Mat,
        grayMat: Mat,
        debug: Boolean = false,
        debugMapAdditionCallback: (String, Bitmap) -> Unit,
        failedCallback: (String) -> Unit
    ): List<Point>? {
        val anchorPoints = detectAnchorPointsImpl(grayMat, true)
        if (anchorPoints.size != 4) {
            if(debug) {
                OmrUtils.drawPoints(
                    srcMat,
                    points = anchorPoints,
                ).apply {
                    debugMapAdditionCallback("Anchor Points other than 4 - ${anchorPoints.size}", toBitmapSafe())
                }
            }
            failedCallback("Anchor points are ${anchorPoints.size}. It should be 4")
            return null
        }
        return anchorPoints
    }

    @Throws
    fun detectAnchorPointsImpl(gray: Mat, debug: Boolean = false): List<Point> {
        val anchors = mutableListOf<Point>()

        // 1. Threshold (since anchors are black)
        val bin = Mat()
        Imgproc.threshold(
            gray,
            bin,
            50.0,
            255.0,
            Imgproc.THRESH_BINARY_INV
        )

        // 2. Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            bin,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // 3. Filter contours that look like squares
        for (contour in contours) {
            val mp2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(mp2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(mp2f, approx, 0.04 * peri, true)

            if (approx.total() == 4L) {
                val approxMp = MatOfPoint(*approx.toArray())
                val rect = Imgproc.boundingRect(approxMp)
                val w = rect.width.toDouble()
                val h = rect.height.toDouble()
                if (w <= 0.0 || h <= 0.0) { approxMp.release(); approx.release(); mp2f.release(); continue }

                val aspect = w / h

                // solidity against axis-aligned box (simple & fast)
                val contourArea = Imgproc.contourArea(contour)
                val rectArea = w * h
                val solidity = if (rectArea > 0.0) contourArea / rectArea else 0.0
                // Optional convexity: Imgproc.isContourConvex(approxMp)

                // Keep near-square, not-too-tiny, reasonably solid, solidity >=0.7, likely a circle, skip
                if (aspect in 0.9..1.1 && rectArea >= 800.0 && solidity >= 0.7) {
                    val center = Point(rect.x + w / 2.0, rect.y + h / 2.0)
                    anchors.add(center)
                }
                approxMp.release()
            }
            approx.release()
            mp2f.release()
        }


        // 4. Sort anchors into LT, RT, RB, LB
        if (anchors.size == 4) {
            // sort top vs bottom by y
            val sorted = anchors.sortedBy { it.y }
            val top = sorted.take(2).sortedBy { it.x }   // left, right
            val bottom = sorted.takeLast(2).sortedBy { it.x } // left, right

            anchors.clear()
            anchors.add(top[0])     // LT
            anchors.add(top[1])     // RT
            anchors.add(bottom[1])  // RB
            anchors.add(bottom[0])  // LB
        }

        bin.release()
        hierarchy.release()

        return anchors
    }

    @Throws
    private fun detectAndFetchBubblesWithinAnchors(
        grayMat: Mat,
        anchorPoints: List<Point>,
        debug: Boolean = false,
        debugMapAdditionCallback: (String, Bitmap) -> Unit,
    ): List<Bubble> {
        // 1. Make a mask same size as gray
        val mask = Mat.zeros(grayMat.size(), CvType.CV_8UC1)

        // 2. Define polygon (anchors are LT, RT, RB, LB)
        val anchorMat = MatOfPoint(*anchorPoints.toTypedArray())
        Imgproc.fillConvexPoly(mask, anchorMat, Scalar(255.0))

        // 3. Apply mask
        val masked = Mat()
        Core.bitwise_and(grayMat, mask, masked)
        if (debug) debugMapAdditionCallback(
            "masked within anchors detecting bubbles",
            masked.toBitmapSafe()
        )


        // 4. Blur
        val blurred = Mat()
        Imgproc.GaussianBlur(masked, blurred, Size(9.0, 9.0), 2.0)

        // 5. Detect circles
        val circles = Mat()
        Imgproc.HoughCircles(
            blurred,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            20.0,
            100.0,
            30.0,
            8,
            20
        )

        val centers = mutableListOf<Bubble>()
        for (i in 0 until circles.cols()) {
            val data = circles.get(0, i) ?: continue
            val x = data[0]
            val y = data[1]
            val r = data[2]
            centers.add(Bubble(x, y, r))
        }

        centers.sortWith(compareBy<Bubble> { it.y }.thenBy { it.x })

        centers.sortWith(
            compareBy({ it.y.roundToInt() }, { it.x })
        )
        mask.release()
        masked.release()
        blurred.release()
        circles.release()
        return centers
    }

    @Throws
    private fun sortBubblesColumnWise(bubbles: List<Bubble>): List<List<Bubble>> {
        if (bubbles.isEmpty()) return emptyList()

        val tol = computeRowToleranceFromBubbles(bubbles)

        // 1. Sort by y first
        val sorted = bubbles.sortedBy { it.y }
        val rows = mutableListOf<MutableList<Bubble>>()

        // 2. Make buckets by Y tolerance
        var currentRow = mutableListOf<Bubble>()
        var currentY = sorted.first().y

        for (b in sorted) {
            if (abs(b.y - currentY) <= tol) {
                currentRow.add(b)
            } else {
                // finalize this row
                rows.add(currentRow.sortedBy { it.x }.toMutableList())
                // start new row
                currentRow = mutableListOf(b)
                currentY = b.y
            }
        }
        // Add last row
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow.sortedBy { it.x }.toMutableList())
        }

        return rows
    }

    @Throws
    private fun computeRowToleranceFromBubbles(
        bubbles: List<Bubble>,
        factor: Double = 0.6,     // typical choice 0.5..0.8
        minTol: Double = 6.0,     // lower bound in pixels
        maxTol: Double = 60.0     // upper bound (prevent huge merges)
    ): Double {
        if (bubbles.isEmpty()) return minTol

        // compute radii list
        val radii = bubbles.map { it.r }.filter { it.isFinite() && it > 0.0 }
        if (radii.isEmpty()) return minTol

        // median radius (robust to outliers)
        val sorted = radii.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }

        val tol = (median * factor).coerceIn(minTol, maxTol)
        return tol
    }

    @Throws
    private fun generateTemplateJsonSimple(
        anchors: List<Point>,
        bubbleGrid: List<List<Bubble>>,
        size: Size,
    ): TemplatePair {
        val gson = Gson()
        val questions = mutableListOf<Question>()

        var qNo = 1
        var topLeftAnchor = anchors[0]
        for (row in bubbleGrid) {
            val options = mutableListOf<OptionBox>()
            val optionNames = listOf("A", "B", "C", "D")

            for ((idx, pt) in row.withIndex()) {
                val relX = (pt.x - topLeftAnchor.x)
                val relY = (pt.y - topLeftAnchor.y)
                options.add(
                    OptionBox(
                        option = optionNames.getOrElse(idx) { "?" },
                        x = relX,
                        y = relY,
                        r = pt.r
                    )
                )
            }

            questions.add(Question(qNo, options))
            qNo++
        }

        val template = Template(
            version = "1.0",
            sheet_width = size.width,
            sheet_height = size.height,
            options_per_question = 4,
            grid = null,       // skip if not using grid
            questions = questions,
            anchor_top_left = anchors[0],
            anchor_top_right = anchors[1],
            anchor_bottom_right = anchors[2],
            anchor_bottom_left = anchors[3]
        )
        val json = gson.toJson(template)
        println("$TAG - templateJson - $json")
        return TemplatePair(json, template)
    }

    @Throws
    private fun generate2DArrayOfBubbles(
        bubbleCenters: List<Bubble>,
        rowTolerance: Double? = null
    ): MutableList<MutableList<Bubble>> {
        if (bubbleCenters.isEmpty()) return mutableListOf()

        // decide tolerance
        val tol = rowTolerance ?: computeRowToleranceFromBubbles(bubbleCenters)

        // sort by Y then X
        val sorted = bubbleCenters.sortedWith(compareBy<Bubble> { it.y }.thenBy { it.x })

        val bubbleGrid = mutableListOf<MutableList<Bubble>>()
        var currentRow = mutableListOf<Bubble>()
        var lastY = sorted.first().y

        for (bubble in sorted) {
            if (abs(bubble.y - lastY) <= tol) {
                currentRow.add(bubble)
            } else {
                // finalize previous row
                if (currentRow.isNotEmpty()) bubbleGrid.add(currentRow)
                currentRow = mutableListOf(bubble)
            }
            lastY = bubble.y
        }
        if (currentRow.isNotEmpty()) bubbleGrid.add(currentRow)

        return bubbleGrid
    }
}