package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.base.utils.image.toColoredWarped
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import com.example.ledgerscanner.feature.scanner.scan.model.Gap
import com.example.ledgerscanner.feature.scanner.scan.model.HoughParams
import com.example.ledgerscanner.feature.scanner.scan.model.OmrSheetType
import com.example.ledgerscanner.feature.scanner.scan.model.OmrTemplateResult
import com.example.ledgerscanner.feature.scanner.scan.model.OptionBox
import com.example.ledgerscanner.feature.scanner.scan.model.Question
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.model.TemplatePair
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
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class TemplateProcessor @Inject constructor() {

    companion object {
        const val TAG = "TemplateProcessor"
    }

    /**
     * Unified template generation that supports both bubble and grid-based OMR sheets
     *
     * @param sheetType Type of OMR sheet (BUBBLE_BASED or GRID_BASED)
     * @param questionsPerColumn For BUBBLE_BASED: questions per column
     * @param numberOfColumns For BUBBLE_BASED: number of columns
     * @param totalQuestions For GRID_BASED: total questions
     * @param optionsPerQuestion Options per question (default 4)
     */
    @WorkerThread
    fun generateTemplateJson(
        inputBitmap: Bitmap,
        sheetType: OmrSheetType,
        questionsPerColumn: Int = 25,
        numberOfColumns: Int = 2,
        optionsPerQuestion: Int = 4,
        totalQuestions: Int = 108,
        debug: Boolean = true
    ): OmrTemplateResult {
        return when (sheetType) {
            OmrSheetType.BUBBLE_BASED -> generateBubbleTemplateJson(
                inputBitmap = inputBitmap,
                questionsPerColumn = questionsPerColumn,
                numberOfColumns = numberOfColumns,
                optionsPerQuestion = optionsPerQuestion,
                debug = debug
            )

            OmrSheetType.GRID_BASED -> generateGridTemplateJson(
                inputBitmap = inputBitmap,
                totalQuestions = totalQuestions,
                optionsPerQuestion = optionsPerQuestion,
                debug = debug
            )
        }
    }

    /**
     * Generates template for bubble-based OMR sheets (circular bubbles)
     * This is your existing generateTemplateJson renamed
     */
    @WorkerThread
    private fun generateBubbleTemplateJson(
        inputBitmap: Bitmap,
        questionsPerColumn: Int = 25,
        numberOfColumns: Int = 2,
        optionsPerQuestion: Int = 4,
        debug: Boolean = true
    ): OmrTemplateResult {
        val debugMap = hashMapOf<String, Bitmap>()
        var srcMat: Mat? = null
        var grayMat: Mat? = null
        try {
            Log.d(
                TAG,
                "Starting BUBBLE template generation: $questionsPerColumn questions/column × " +
                        "$numberOfColumns columns × $optionsPerQuestion options = " +
                        "${questionsPerColumn * numberOfColumns * optionsPerQuestion} total bubbles"
            )

            // 1. Convert bitmap to grayscale
            srcMat = Mat().apply {
                Utils.bitmapToMat(inputBitmap, this)
            }
            grayMat = Mat().apply {
                Imgproc.cvtColor(srcMat, this, Imgproc.COLOR_BGR2GRAY)
                if (debug) debugMap["1_gray"] = this.toBitmapSafe()
            }

            // 2. Detect anchors
            val anchorPoints: List<AnchorPoint> = detectAnchorPoints(
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

            // 3. Detect bubbles (circular)
            val bubbles = detectAndFetchBubblesWithinAnchors(
                grayMat = grayMat,
                anchorPoints = anchorPoints,
                questionsPerColumn = questionsPerColumn,
                numberOfColumns = numberOfColumns,
                optionsPerQuestion = optionsPerQuestion,
                debug = debug,
                debugMapAdditionCallback = { title, bitmap ->
                    debugMap[title] = bitmap
                }
            )

            // 4. Validate bubble count
            val expectedBubbles = questionsPerColumn * numberOfColumns * optionsPerQuestion
            if (bubbles.size != expectedBubbles) {
                return OmrTemplateResult(
                    success = false,
                    reason = "Expected exactly $expectedBubbles bubbles " +
                            "($questionsPerColumn questions/column × $numberOfColumns columns × $optionsPerQuestion options), " +
                            "but found ${bubbles.size} bubbles.\n\n" +
                            "Please ensure:\n" +
                            "• The OMR sheet has exactly $questionsPerColumn questions per column\n" +
                            "• There are exactly $numberOfColumns columns\n" +
                            "• Each question has exactly $optionsPerQuestion options\n" +
                            "• All bubbles are clearly visible and well-lit",
                    debugBitmaps = debugMap
                )
            }

            // 5. Sort bubbles into 2D array (handles multiple columns automatically)
            val bubbles2DArray = sortBubblesColumnWise(bubbles, optionsPerQuestion)

            // 6. Validate structure
            val totalQuestions = questionsPerColumn * numberOfColumns
            if (bubbles2DArray.size != totalQuestions) {
                return OmrTemplateResult(
                    success = false,
                    reason = "Expected $totalQuestions questions after sorting, got ${bubbles2DArray.size}",
                    debugBitmaps = debugMap
                )
            }
            val invalidRows = bubbles2DArray.filter { it.size != optionsPerQuestion }
            if (invalidRows.isNotEmpty()) {
                return OmrTemplateResult(
                    success = false,
                    reason = "Found ${invalidRows.size} questions with incorrect bubble count " +
                            "(expected $optionsPerQuestion per question)",
                    debugBitmaps = debugMap
                )
            }

            // 7. Generate template JSON
            val templatePair = generateTemplateJsonSimple(
                anchorPoints,
                bubbles2DArray,
                srcMat.size()
            )

            // 8. Create final debug bitmap
            val finalBitmap = OpenCvUtils.drawPoints(
                grayMat.toColoredWarped(),
                points = anchorPoints,
                bubbles2DArray = bubbles2DArray,
                radius = templatePair.template?.questions?.firstOrNull()?.options?.firstOrNull()?.r?.roundToInt(),
            ).let {
                val bitmap = it.toBitmapSafe()
                debugMap["9_final"] = bitmap
                it.release()
                bitmap
            }

            return OmrTemplateResult(
                success = true,
                debugBitmaps = debugMap,
                finalBitmap = finalBitmap,
                templateJson = templatePair.templateJson,
                template = templatePair.template
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating bubble template", e)
            return OmrTemplateResult(
                success = false,
                reason = "${e.javaClass.simpleName}: ${e.message}",
                debugBitmaps = debugMap
            )
        } finally {
            srcMat?.release()
            grayMat?.release()
        }
    }

    /**
     * Generates template for grid-based OMR sheets (rectangular cells)
     */
    @WorkerThread
    private fun generateGridTemplateJson(
        inputBitmap: Bitmap,
        totalQuestions: Int = 108,
        optionsPerQuestion: Int = 4,
        debug: Boolean = true
    ): OmrTemplateResult {
        val debugMap = hashMapOf<String, Bitmap>()
        var srcMat: Mat? = null
        var grayMat: Mat? = null

        try {
            Log.d(
                TAG,
                "Starting GRID template generation: $totalQuestions questions × $optionsPerQuestion options = ${totalQuestions * optionsPerQuestion} total cells"
            )

            // 1. Convert bitmap to grayscale
            srcMat = Mat().apply {
                Utils.bitmapToMat(inputBitmap, this)
            }
            grayMat = Mat().apply {
                Imgproc.cvtColor(srcMat, this, Imgproc.COLOR_BGR2GRAY)
                if (debug) debugMap["1_gray"] = this.toBitmapSafe()
            }

            // 2. Detect anchors
            val anchorPoints: List<AnchorPoint> = detectAnchorPoints(
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

            // 3. Detect cells (rectangular) instead of bubbles (circular)
            val cells = detectAndFetchCellsWithinAnchors(
                grayMat = grayMat,
                anchorPoints = anchorPoints,
                totalQuestions = totalQuestions,
                optionsPerQuestion = optionsPerQuestion,
                debug = debug,
                debugMapAdditionCallback = { title, bitmap ->
                    debugMap[title] = bitmap
                }
            )

            // 4. Validate cell count
            val expectedCells = totalQuestions * optionsPerQuestion
//            if (cells.size != expectedCells) {
//                return OmrTemplateResult(
//                    success = false,
//                    reason = "Expected exactly $expectedCells cells " +
//                            "($totalQuestions questions × $optionsPerQuestion options), " +
//                            "but found ${cells.size} cells.\n\n" +
//                            "Please ensure:\n" +
//                            "• The OMR sheet has exactly $totalQuestions questions\n" +
//                            "• Each question has exactly $optionsPerQuestion options\n" +
//                            "• All cells are clearly visible with distinct borders\n" +
//                            "• The sheet is well-lit and properly aligned",
//                    debugBitmaps = debugMap
//                )
//            }

            // 5. Sort cells into 2D array
            val cells2DArray = sortCellsSequential(cells, optionsPerQuestion)

            // 6. Validate structure
//            if (cells2DArray.size != totalQuestions) {
//                return OmrTemplateResult(
//                    success = false,
//                    reason = "Expected $totalQuestions questions after sorting, got ${cells2DArray.size}",
//                    debugBitmaps = debugMap
//                )
//            }

            val invalidRows = cells2DArray.filter { it.size != optionsPerQuestion }
//            if (invalidRows.isNotEmpty()) {
//                return OmrTemplateResult(
//                    success = false,
//                    reason = "Found ${invalidRows.size} questions with incorrect cell count " +
//                            "(expected $optionsPerQuestion per question)",
//                    debugBitmaps = debugMap
//                )
//            }

            // 7. Generate template JSON
            val templatePair = generateTemplateJsonSimple(
                anchorPoints,
                cells2DArray,
                srcMat.size()
            )

            // 8. Create final debug bitmap
            val finalBitmap = OpenCvUtils.drawPoints(
                grayMat.toColoredWarped(),
                points = anchorPoints,
                bubbles2DArray = cells2DArray,
                radius = templatePair.template?.questions?.firstOrNull()?.options?.firstOrNull()?.r?.roundToInt(),
            ).let {
                val bitmap = it.toBitmapSafe()
                debugMap["9_final"] = bitmap
                it.release()
                bitmap
            }

            return OmrTemplateResult(
                success = true,
                debugBitmaps = debugMap,
                finalBitmap = finalBitmap,
                templateJson = templatePair.templateJson,
                template = templatePair.template
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating grid template", e)
            return OmrTemplateResult(
                success = false,
                reason = "${e.javaClass.simpleName}: ${e.message}",
                debugBitmaps = debugMap
            )
        } finally {
            srcMat?.release()
            grayMat?.release()
        }
    }

    /**
     * Detects rectangular cells within the anchored frame (for grid-based OMR sheets)
     * Similar to bubble detection but looks for rectangles instead of circles
     */
    private fun detectAndFetchCellsWithinAnchors(
        grayMat: Mat,
        anchorPoints: List<AnchorPoint>,
        totalQuestions: Int,
        optionsPerQuestion: Int,
        debug: Boolean = false,
        debugMapAdditionCallback: (String, Bitmap) -> Unit,
    ): List<Bubble> {
        var mask: Mat? = null
        var masked: Mat? = null
        var binary: Mat? = null
        var anchorMat: MatOfPoint? = null

        try {
            // 1. Make mask
            mask = Mat.zeros(grayMat.size(), CvType.CV_8UC1)
            anchorMat = MatOfPoint(*anchorPoints.map { Point(it.x, it.y) }.toTypedArray())
            Imgproc.fillConvexPoly(mask, anchorMat, Scalar(255.0))

            // 2. Apply mask
            masked = Mat()
            Core.bitwise_and(grayMat, mask, masked)
            if (debug) debugMapAdditionCallback("2_masked_region", masked.toBitmapSafe())

            // 3. Apply threshold to get binary image
            binary = Mat()
            Imgproc.threshold(masked, binary, 127.0, 255.0, Imgproc.THRESH_BINARY_INV)
            if (debug) debugMapAdditionCallback("3_binary", binary.toBitmapSafe())

            // 4. Calculate expected cell count
            val exactExpectedCells = totalQuestions * optionsPerQuestion
            Log.d(
                TAG,
                "Expected: $exactExpectedCells cells ($totalQuestions questions × $optionsPerQuestion options)"
            )

            // 5. Calculate estimated cell size
            val maskedArea = Imgproc.contourArea(anchorMat)
            val estimatedCellArea = maskedArea / (exactExpectedCells * 1.5) // 1.5 accounts for gaps
            val estimatedCellSize = sqrt(estimatedCellArea)

            Log.d(TAG, "Estimated cell size: ${estimatedCellSize.roundToInt()}")

            // 6. Find contours
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(
                binary,
                contours,
                hierarchy,
                Imgproc.RETR_TREE,
                Imgproc.CHAIN_APPROX_SIMPLE
            )

            Log.d(TAG, "Found ${contours.size} total contours")

            // 7. Filter contours for valid cells
            val detectedCells = mutableListOf<Bubble>()

            for (contour in contours) {
                val rect = Imgproc.boundingRect(contour)

                if (isValidCell(rect, estimatedCellSize)) {
                    // Store as Bubble (reusing existing structure)
                    // Center point and "radius" (half of average dimension)
                    val centerX = rect.x + rect.width / 2.0
                    val centerY = rect.y + rect.height / 2.0
                    val avgDimension = (rect.width + rect.height) / 2.0
                    val pseudoRadius = avgDimension / 2.0

                    detectedCells.add(Bubble(centerX, centerY, pseudoRadius))
                }
            }

            Log.d(TAG, "Detected ${detectedCells.size} valid cells (expected $exactExpectedCells)")

            // 8. Draw detected cells for debugging
            if (debug) {
                val debugMat = masked.toColoredWarped()
                for (cell in detectedCells) {
                    // Draw rectangle around each detected cell
                    val size = (cell.r * 2).toInt()
                    val topLeft = Point(cell.x - cell.r, cell.y - cell.r)
                    val bottomRight = Point(cell.x + cell.r, cell.y + cell.r)
                    Imgproc.rectangle(debugMat, topLeft, bottomRight, Scalar(0.0, 255.0, 0.0), 2)
                }
                debugMapAdditionCallback("4_detected_cells", debugMat.toBitmapSafe())
                debugMat.release()
            }

            hierarchy.release()

            return detectedCells.sortedWith(compareBy({ it.y.roundToInt() }, { it.x }))

        } finally {
            mask?.release()
            masked?.release()
            binary?.release()
            anchorMat?.release()
        }
    }

    /**
     * Validates if a rectangle is likely a response cell
     */
    private fun isValidCell(rect: org.opencv.core.Rect, estimatedSize: Double): Boolean {
        val area = rect.width * rect.height

        // Filter by area (should be close to estimated size)
        val minArea = (estimatedSize * estimatedSize) * 0.3
        val maxArea = (estimatedSize * estimatedSize) * 3.0

        if (area < minArea || area > maxArea) {
            return false
        }

        // Filter by aspect ratio (cells should be roughly square)
        val aspectRatio = rect.width.toFloat() / rect.height.toFloat()
        if (aspectRatio < 0.6 || aspectRatio > 1.7) {
            return false
        }

        // Filter by minimum size (avoid noise)
        if (rect.width < 10 || rect.height < 10) {
            return false
        }

        return true
    }

    /**
     * Sorts cells sequentially for grid-based layouts
     * Questions flow down each section, then move to next section
     *
     * Example: Q1-27 in left section, Q28-54 in middle, Q55-108 in right
     */
    private fun sortCellsSequential(
        cells: List<Bubble>,
        optionsPerQuestion: Int = 4
    ): List<List<Bubble>> {
        if (cells.isEmpty()) return emptyList()

        // Sort all cells by X coordinate first (to identify sections/columns)
        val sortedByX = cells.sortedBy { it.x }

        // Group cells into vertical sections based on X-coordinate gaps
        val sections = groupIntoVerticalSections(sortedByX, optionsPerQuestion)

        Log.d(TAG, "Identified ${sections.size} vertical sections")

        // Process each section: sort by Y, then group by rows
        val allQuestions = mutableListOf<List<Bubble>>()

        for ((sectionIndex, sectionCells) in sections.withIndex()) {
            // Sort this section by Y coordinate (top to bottom)
            val sortedByY = sectionCells.sortedBy { it.y }

            // Group into rows
            val rows = groupIntoRows(sortedByY)

            Log.d(TAG, "Section $sectionIndex: ${rows.size} questions")

            // Each row should have optionsPerQuestion cells
            for (row in rows) {
                if (row.size == optionsPerQuestion) {
                    allQuestions.add(row.sortedBy { it.x }) // Sort options left-to-right
                } else {
                    Log.w(
                        TAG,
                        "Section $sectionIndex: Row has ${row.size} cells (expected $optionsPerQuestion)"
                    )
                }
            }
        }

        return allQuestions
    }

    /**
     * Groups cells into vertical sections (columns) based on X-coordinate clustering
     */
    private fun groupIntoVerticalSections(
        sortedByX: List<Bubble>,
        optionsPerQuestion: Int
    ): List<List<Bubble>> {
        if (sortedByX.isEmpty()) return emptyList()

        val sections = mutableListOf<MutableList<Bubble>>()
        var currentSection = mutableListOf(sortedByX.first())

        // Calculate average cell spacing
        val xGaps = mutableListOf<Double>()
        for (i in 0 until sortedByX.size - 1) {
            xGaps.add(sortedByX[i + 1].x - sortedByX[i].x)
        }
        val avgGap = xGaps.average()
        val sectionThreshold = avgGap * 3.0 // Large gap indicates section boundary

        for (i in 1 until sortedByX.size) {
            val gap = sortedByX[i].x - sortedByX[i - 1].x

            if (gap > sectionThreshold) {
                // New section detected
                sections.add(currentSection)
                currentSection = mutableListOf(sortedByX[i])
            } else {
                currentSection.add(sortedByX[i])
            }
        }

        if (currentSection.isNotEmpty()) {
            sections.add(currentSection)
        }

        return sections
    }

    @Throws
    inline fun detectAnchorPoints(
        srcMat: Mat,
        grayMat: Mat,
        debug: Boolean = false,
        debugMapAdditionCallback: (String, Bitmap) -> Unit,
        failedCallback: (String) -> Unit
    ): List<AnchorPoint>? {
        val anchorPoints = detectAnchorPointsImpl(grayMat, debug)
        if (debug) {
            OpenCvUtils.drawPoints(
                srcMat,
                points = anchorPoints,
            ).apply {
                debugMapAdditionCallback(
                    if (anchorPoints.size != 4) "Anchor Points other than 4 - ${anchorPoints.size}" else "4 Anchor points",
                    toBitmapSafe()
                )
            }
        }
        if (anchorPoints.size != 4) {
            failedCallback("Anchor points are ${anchorPoints.size}. It should be 4")
            return null
        }
        return anchorPoints
    }

    @Throws
    fun detectAnchorPointsImpl(gray: Mat, debug: Boolean = false): List<AnchorPoint> {
        val anchors = mutableListOf<AnchorPoint>()

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
                if (w <= 0.0 || h <= 0.0) {
                    approxMp.release(); approx.release(); mp2f.release(); continue
                }

                val aspect = w / h

                // solidity against axis-aligned box (simple & fast)
                val contourArea = Imgproc.contourArea(contour)
                val rectArea = w * h
                val solidity = if (rectArea > 0.0) contourArea / rectArea else 0.0
                // Optional convexity: Imgproc.isContourConvex(approxMp)

                // Keep near-square, not-too-tiny, reasonably solid, solidity >=0.7, likely a circle, skip
                if (aspect in 0.9..1.1 && rectArea >= 800.0 && solidity >= 0.7) {
                    val center = AnchorPoint(rect.x + w / 2.0, rect.y + h / 2.0)
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

    /**
     * Detect bubbles with adaptive multi-pass strategy
     * Keeps trying until expected count is reached
     */
    private fun detectAndFetchBubblesWithinAnchors(
        grayMat: Mat,
        anchorPoints: List<AnchorPoint>,
        questionsPerColumn: Int,
        numberOfColumns: Int,
        optionsPerQuestion: Int,
        debug: Boolean = false,
        debugMapAdditionCallback: (String, Bitmap) -> Unit,
    ): List<Bubble> {
        var mask: Mat? = null
        var masked: Mat? = null
        var blurred: Mat? = null
        var circles: Mat? = null
        var anchorMat: MatOfPoint? = null

        try {
            // 1. Make mask
            mask = Mat.zeros(grayMat.size(), CvType.CV_8UC1)
            anchorMat = MatOfPoint(*anchorPoints.map { Point(it.x, it.y) }.toTypedArray())
            Imgproc.fillConvexPoly(mask, anchorMat, Scalar(255.0))

            // 2. Apply mask
            masked = Mat()
            Core.bitwise_and(grayMat, mask, masked)
            if (debug) debugMapAdditionCallback(
                "4_masked_region",
                masked.toBitmapSafe()
            )

            // 3. Blur
            blurred = Mat()
            Imgproc.GaussianBlur(masked, blurred, Size(5.0, 5.0), 1.5)

            // 4. Calculate expected bubble count
            val totalQuestions = questionsPerColumn * numberOfColumns
            val exactExpectedBubbles = totalQuestions * optionsPerQuestion

            Log.d(
                TAG, "Expected: $exactExpectedBubbles bubbles " +
                        "($questionsPerColumn questions/column × $numberOfColumns columns × $optionsPerQuestion options)"
            )

            // 5. Calculate base parameters
            val maskedArea = Imgproc.contourArea(anchorMat)
            val estimatedBubbleRadius = sqrt(maskedArea / (exactExpectedBubbles * 10.0))

            Log.d(TAG, "Estimated bubble radius: ${estimatedBubbleRadius.roundToInt()}")

            // 6. Try detection with increasing aggressiveness
            val attempts = listOf(
                // Attempt 1: Moderate (your working parameters)
                HoughParams(
                    name = "Moderate",
                    minDist = max(12.0, estimatedBubbleRadius * 1.0),
                    param2 = 30.0,
                    minRadius = max(5, (estimatedBubbleRadius * 0.5).roundToInt()),
                    maxRadius = min(60, (estimatedBubbleRadius * 1.8).roundToInt())
                ),
                // Attempt 2: Aggressive
                HoughParams(
                    name = "Aggressive",
                    minDist = max(10.0, estimatedBubbleRadius * 0.8),
                    param2 = 25.0,
                    minRadius = max(4, (estimatedBubbleRadius * 0.4).roundToInt()),
                    maxRadius = min(70, (estimatedBubbleRadius * 2.0).roundToInt())
                ),
                // Attempt 3: Very Aggressive
                HoughParams(
                    name = "Very Aggressive",
                    minDist = max(8.0, estimatedBubbleRadius * 0.6),
                    param2 = 20.0,
                    minRadius = max(3, (estimatedBubbleRadius * 0.3).roundToInt()),
                    maxRadius = min(80, (estimatedBubbleRadius * 2.2).roundToInt())
                )
            )

            var closestBubbles = emptyList<Bubble>()
            var closestCount = 0
            var closestDiff = Int.MAX_VALUE

            for ((index, params) in attempts.withIndex()) {
                circles?.release()
                circles = Mat()

                Imgproc.HoughCircles(
                    blurred,
                    circles,
                    Imgproc.HOUGH_GRADIENT,
                    1.0,
                    params.minDist,
                    100.0,
                    params.param2,
                    params.minRadius,
                    params.maxRadius
                )

                val detectedCount = circles.cols()
                val diff = abs(detectedCount - exactExpectedBubbles)

                Log.d(
                    TAG, "Attempt ${index + 1}/${attempts.size} (${params.name}): " +
                            "detected=$detectedCount, expected=$exactExpectedBubbles, diff=$diff"
                )

                // Extract bubbles
                val currentBubbles = mutableListOf<Bubble>()
                for (i in 0 until circles.cols()) {
                    val data = circles.get(0, i) ?: continue
                    val x = data[0]
                    val y = data[1]
                    val r = data[2]
                    if (r.isFinite() && r > 0) {
                        currentBubbles.add(Bubble(x, y, r))
                    }
                }

                // Keep track of closest attempt
                if (diff < closestDiff) {
                    closestBubbles = currentBubbles
                    closestCount = currentBubbles.size
                    closestDiff = diff
                }

                // Check if we found EXACTLY the expected number
                if (currentBubbles.size == exactExpectedBubbles) {
                    Log.d(TAG, "✓ Perfect match! Found exactly $exactExpectedBubbles bubbles")

                    return currentBubbles.sortedWith(compareBy({ it.y.roundToInt() }, { it.x }))
                }
            }

            // No perfect match found, return closest attempt
            Log.w(
                TAG, "⚠ Could not find exact match. Closest: $closestCount bubbles " +
                        "(expected $exactExpectedBubbles, difference: $closestDiff)"
            )

            return closestBubbles.sortedWith(compareBy({ it.y.roundToInt() }, { it.x }))

        } finally {
            mask?.release()
            masked?.release()
            blurred?.release()
            circles?.release()
            anchorMat?.release()
        }
    }

    /**
     * Detection strategy configuration
     */
    private data class DetectionStrategy(
        val name: String,
        val minDistFactor: Double,
        val param2: Double,
        val minRadiusFactor: Double,
        val maxRadiusFactor: Double,
        val minRadiusAbs: Int,
        val maxRadiusAbs: Int
    )

    /**
     * Sorts bubbles into a 2D array for multi-column layouts
     *
     * Algorithm:x
     * 1. Sort bubbles by Y coordinate (group into rows)
     * 2. For each row, find the largest X gap
     * 3. If gap is significant, split row at that point
     * 4. Each split becomes a separate question
     *
     * @param bubbles List of detected bubble centers
     * @param optionsPerQuestion Expected number of options per question
     * @return 2D array where each inner list represents one question's options
     */
    @Throws
    private fun sortBubblesColumnWise(
        bubbles: List<Bubble>,
        optionsPerQuestion: Int = 4
    ): List<List<Bubble>> {
        if (bubbles.isEmpty()) return emptyList()

        // Step:1 - Sort all bubbles by y coordinated then by x
        val sortedBubbles = bubbles.sortedWith(compareBy({ it.y.roundToInt() }, { it.x }))

        // Step 2: Group bubbles into rows (same Y coordinate)
        val rows = groupIntoRows(sortedBubbles)
        Log.d(TAG, "Grouped into ${rows.size} rows")

        // Step 3: Process each row and split at large gaps
        val allQuestions = mutableListOf<MutableList<MutableList<Bubble>>>()

        rows.forEachIndexed { rowIndex, rowBubbles ->
            val questions = splitRowAtGaps(rowBubbles, optionsPerQuestion)

            Log.d(
                TAG, "Row $rowIndex (Y= ${rowBubbles.first().y.roundToInt()}): " +
                        "${rowBubbles.size} bubbles → ${questions.size} questions"
            )

            questions.forEachIndexed { columnIndex, bubbles ->
                // Ensure allQuestions has enough space for this column
                while (allQuestions.size <= columnIndex) {
                    allQuestions.add(mutableListOf())
                }

                // Add bubbles to the corresponding column
                allQuestions[columnIndex].add(bubbles.toMutableList())
            }
        }

        // Step 4: Flatten - all questions from column 0, then column 1, etc.
        val finalQuestions = allQuestions.flatMapIndexed { columnIndex, columnQuestions ->
            Log.d(TAG, "Column $columnIndex: ${columnQuestions.size} questions")
            columnQuestions
        }


        // Step 5: Validate
        val invalidQuestions = finalQuestions.filter { it.size != optionsPerQuestion }
        if (invalidQuestions.isNotEmpty()) {
            Log.w(TAG, "⚠️ Found ${invalidQuestions.size} questions with incorrect bubble count")
            invalidQuestions.forEach { question ->
                Log.w(TAG, "  Question has ${question.size} bubbles (expected $optionsPerQuestion)")
            }
        }

        return finalQuestions
    }

    /**
     * Splits a row of bubbles at large X gaps
     * Each split represents a question in a different column
     *
     * @param rowBubbles All bubbles in a single row (same Y coordinate)
     * @param optionsPerQuestion Expected options per question
     * @return List of questions, each with its bubbles
     */
    private fun splitRowAtGaps(
        rowBubbles: List<Bubble>,
        optionsPerQuestion: Int
    ): List<List<Bubble>> {
        if (rowBubbles.isEmpty()) return emptyList()
        if (rowBubbles.size <= optionsPerQuestion) {
            // Single question, no split needed
            return listOf(rowBubbles)
        }

        // Already sorted by X (from parent sort)
        val sortedByX = rowBubbles.sortedBy { it.x }

        // Calculate gaps between consecutive bubbles
        val gaps = mutableListOf<Gap>()
        for (i in 0 until sortedByX.size - 1) {
            val gap = sortedByX[i + 1].x - sortedByX[i].x
            gaps.add(Gap(index = i, size = gap))
        }

        if (gaps.isEmpty()) return listOf(rowBubbles)

        // Find the largest gap
        val largestGap = gaps.maxByOrNull { it.size } ?: return listOf(rowBubbles)

        // Calculate average gap (normal spacing between options)
        val avgGap = gaps.map { it.size }.average()

        Log.v(
            TAG, "Row Y=${sortedByX.first().y.roundToInt()}: " +
                    "avgGap=${avgGap.roundToInt()}, largestGap=${largestGap.size.roundToInt()} " +
                    "at index ${largestGap.index}"
        )

        // Check if largest gap is significantly larger (indicates column boundary)
        val isSignificantGap = largestGap.size > avgGap * 2.0

        if (!isSignificantGap) {
            // No significant gap, treat as single question
            return listOf(rowBubbles)
        }

        // Split at the largest gap
        val questions = mutableListOf<List<Bubble>>()
        var startIndex = 0

        // Split point is after the bubble at largestGap.index
        val splitIndex = largestGap.index + 1

        // First question (before gap)
        questions.add(sortedByX.subList(startIndex, splitIndex))

        // Remaining bubbles (after gap) - might contain more questions
        val remaining = sortedByX.subList(splitIndex, sortedByX.size)

        // Recursively split remaining bubbles if needed
        val remainingQuestions = splitRowAtGaps(remaining, optionsPerQuestion)
        questions.addAll(remainingQuestions)

        return questions
    }

    /**
     * Groups bubbles into rows based on Y coordinate
     * Bubbles with similar Y (within tolerance) are grouped together
     */
    private fun groupIntoRows(sortedBubbles: List<Bubble>): List<List<Bubble>> {
        if (sortedBubbles.isEmpty()) return emptyList()

        val rows = mutableListOf<MutableList<Bubble>>()
        val yTolerance = 10 // Pixels tolerance for considering bubbles in same row

        var currentRow = mutableListOf(sortedBubbles.first())
        var currentY = sortedBubbles.first().y

        for (i in 1 until sortedBubbles.size) {
            val bubble = sortedBubbles[i]
            val bubbleY = bubble.y

            if (abs(bubbleY - currentY) >= yTolerance) {
                rows.add(currentRow)
                currentRow = mutableListOf(bubble)
                currentY = bubbleY
            } else {
                currentRow.add(bubble)
            }
        }

        // Add last row
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
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
        anchors: List<AnchorPoint>,
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

    @Throws
    fun mapTemplateBubblesToImagePoints(
        template: Template,
    ): List<AnchorPoint> {
        val points = mutableListOf<AnchorPoint>()
        for (q in template.questions) {
            for (o in q.options) {
                points.add(
                    AnchorPoint(
                        template.anchor_top_left.x + o.x,
                        template.anchor_top_left.y + o.y
                    )
                )
            }
        }
        return points
    }
}