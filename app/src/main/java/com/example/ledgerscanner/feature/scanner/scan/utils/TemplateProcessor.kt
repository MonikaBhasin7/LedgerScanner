package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.base.utils.image.toColoredWarped
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import com.example.ledgerscanner.feature.scanner.scan.model.DigitColumn
import com.example.ledgerscanner.feature.scanner.scan.model.EnrollmentGrid
import com.example.ledgerscanner.feature.scanner.scan.model.Gap
import com.example.ledgerscanner.feature.scanner.scan.model.HoughParams
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

    @WorkerThread
    fun generateTemplateJson(
        inputBitmap: Bitmap,
        questionsPerColumn: Int = 25,
        numberOfColumns: Int = 2,
        optionsPerQuestion: Int = 4,
        enrollmentColumns: Int = 0, // 0 = no enrollment grid, >0 = number of digit columns
        debug: Boolean = true
    ): OmrTemplateResult {
        val debugMap = hashMapOf<String, Bitmap>()
        var srcMat: Mat? = null
        var grayMat: Mat? = null
        try {
            val totalAnswerBubbles = questionsPerColumn * numberOfColumns * optionsPerQuestion
            val totalEnrollmentBubbles = if (enrollmentColumns > 0) enrollmentColumns * 10 else 0
            Log.d(
                TAG, "Starting template generation: " +
                        "$questionsPerColumn questions/column × $numberOfColumns columns × $optionsPerQuestion options = " +
                        "$totalAnswerBubbles answer bubbles" +
                        if (enrollmentColumns > 0) " + $enrollmentColumns enrollment columns × 10 digits = $totalEnrollmentBubbles enrollment bubbles" else ""
            )

            // 1. Convert bitmap to grayscale
            srcMat = Mat().apply {
                Utils.bitmapToMat(inputBitmap, this)
            }
            grayMat = Mat().apply {
                Imgproc.cvtColor(srcMat, this, Imgproc.COLOR_BGR2GRAY)
                if (debug) debugMap["gray"] = this.toBitmapSafe()
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

            // 3. Detect ALL bubbles within anchor region
            val expectedTotalBubbles = totalAnswerBubbles + totalEnrollmentBubbles
            val allBubbles = detectAndFetchBubblesWithinAnchors(
                grayMat = grayMat,
                anchorPoints = anchorPoints,
                questionsPerColumn = questionsPerColumn,
                numberOfColumns = numberOfColumns,
                optionsPerQuestion = optionsPerQuestion,
                totalExpectedBubbles = expectedTotalBubbles,
                debug = debug,
                debugMapAdditionCallback = { title, bitmap ->
                    debugMap[title] = bitmap
                }
            )

            Log.d(TAG, "Total bubbles detected: ${allBubbles.size} (expected: $expectedTotalBubbles)")

            // 4. Split enrollment bubbles from answer bubbles (if enrollment is enabled)
            var enrollmentGrid: EnrollmentGrid? = null
            val answerBubbles: List<Bubble>
            var enrollmentBubblesForDrawing: List<List<Bubble>>? = null

            if (enrollmentColumns > 0 && allBubbles.size > totalAnswerBubbles) {
                val splitResult = splitEnrollmentFromAnswers(
                    grayMat,
                    allBubbles = allBubbles,
                    enrollmentColumns = enrollmentColumns,
                    expectedAnswerBubbles = totalAnswerBubbles,
                    anchorTopLeft = anchorPoints[0],
                    debugMapAdditionCallback = { title, bitmap ->
                        debugMap[title] = bitmap
                    }
                )
                enrollmentGrid = splitResult.first
                answerBubbles = splitResult.second

                val enrollmentBubbleCount = allBubbles.size - answerBubbles.size
                Log.d(TAG, "Split: $enrollmentBubbleCount enrollment bubbles, ${answerBubbles.size} answer bubbles")

                // Build enrollment bubbles grouped by column for debug drawing
                // Convert from EnrollmentGrid (relative coords) back to absolute coords for drawing
                if (enrollmentGrid != null) {
                    enrollmentBubblesForDrawing = enrollmentGrid.digits.map { digitCol ->
                        digitCol.bubbles.map { optBox ->
                            Bubble(
                                x = optBox.x + anchorPoints[0].x,
                                y = optBox.y + anchorPoints[0].y,
                                r = optBox.r
                            )
                        }
                    }
                }

                // Validate enrollment bubble count
                val expectedEnrollmentBubbles = enrollmentColumns * 10
                if (enrollmentBubbleCount != expectedEnrollmentBubbles) {
                    Log.w(TAG, "⚠ Enrollment bubble count mismatch: found $enrollmentBubbleCount, expected $expectedEnrollmentBubbles ($enrollmentColumns columns × 10 digits)")
                    return OmrTemplateResult(
                        success = false,
                        reason = "Enrollment grid: found $enrollmentBubbleCount bubbles, expected $expectedEnrollmentBubbles ($enrollmentColumns columns × 10 digits). Check that enrollment grid is clearly printed and anchors cover it.",
                        debugBitmaps = debugMap
                    )
                }

                // Also validate that each column has exactly 10 bubbles
                enrollmentGrid?.digits?.forEachIndexed { colIdx, digitCol ->
                    if (digitCol.bubbles.size != 10) {
                        Log.w(TAG, "⚠ Enrollment column $colIdx has ${digitCol.bubbles.size} bubbles (expected 10)")
                    }
                }
            } else {
                answerBubbles = allBubbles
                if (enrollmentColumns > 0) {
                    Log.w(TAG, "⚠ Enrollment enabled ($enrollmentColumns columns) but total bubbles ${allBubbles.size} <= expected answer bubbles $totalAnswerBubbles. No enrollment bubbles detected.")
                }
            }

            // 5. Sort answer bubbles into 2D array
            val bubbles2DArray = sortBubblesColumnWise(answerBubbles, optionsPerQuestion)

            val invalidRows = bubbles2DArray.filter { it.size != optionsPerQuestion }

            // 6. Generate template JSON
            val templatePair = generateTemplateJsonSimple(
                anchorPoints,
                bubbles2DArray,
                srcMat.size(),
                enrollmentGrid
            )

            // 7. Create final debug bitmap — includes enrollment bubbles in cyan
            val finalBitmap = OpenCvUtils.drawPoints(
                grayMat.toColoredWarped(),
                points = anchorPoints,
                bubbles2DArray = bubbles2DArray,
                enrollmentBubbles = enrollmentBubblesForDrawing,
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
            Log.e(TAG, "Error generating template", e)
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

    /**
     * Candidate anchor with metadata for filtering
     */
    private data class AnchorCandidate(
        val center: AnchorPoint,
        val area: Double,
        val solidity: Double,
        val aspect: Double,
        val vertices: Long,
        val width: Double,
        val height: Double
    )

    @Throws
    fun detectAnchorPointsImpl(gray: Mat, debug: Boolean = false): List<AnchorPoint> {
        val imgW = gray.cols().toDouble()
        val imgH = gray.rows().toDouble()
        val imageArea = imgH * imgW

        // Anchor area relative to image — supports both small and large anchors
        val minAnchorArea = max(100.0, imageArea * 0.0001)
        val maxAnchorArea = imageArea * 0.02

        Log.d(TAG, "Anchor detection: image ${imgW.toInt()}x${imgH.toInt()}, " +
                "area range: ${minAnchorArea.toInt()}-${maxAnchorArea.toInt()}")

        // Try multiple threshold values for robustness
        val thresholds = listOf(50.0, 80.0, 100.0, 127.0)

        for (threshold in thresholds) {
            val result = tryDetectAnchorsAtThreshold(gray, threshold, minAnchorArea, maxAnchorArea, imgW, imgH)
            if (result.size == 4) {
                Log.d(TAG, "✓ Found 4 anchors at threshold=$threshold")
                return result
            }
            Log.d(TAG, "Threshold $threshold: found ${result.size} anchors, trying next...")
        }

        // None of the thresholds gave exactly 4 — try the first threshold with best-effort corner selection
        Log.w(TAG, "No threshold gave exactly 4 anchors, attempting best-effort with threshold=${thresholds[0]}")
        return tryDetectAnchorsAtThreshold(gray, thresholds[0], minAnchorArea, maxAnchorArea, imgW, imgH, forceCornerSelect = true)
    }

    private fun tryDetectAnchorsAtThreshold(
        gray: Mat,
        threshold: Double,
        minAnchorArea: Double,
        maxAnchorArea: Double,
        imgW: Double,
        imgH: Double,
        forceCornerSelect: Boolean = false
    ): List<AnchorPoint> {
        // 1. Threshold (since anchors are black)
        val bin = Mat()
        Imgproc.threshold(gray, bin, threshold, 255.0, Imgproc.THRESH_BINARY_INV)

        // 2. Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(bin, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        Log.d(TAG, "Threshold=$threshold: ${contours.size} contours found")

        // 3. Filter contours that look like filled squares
        val candidates = mutableListOf<AnchorCandidate>()
        var rejectedTooSmall = 0
        var rejectedTooBig = 0
        var rejectedAspect = 0
        var rejectedShape = 0

        for (contour in contours) {
            val rect = Imgproc.boundingRect(contour)
            val w = rect.width.toDouble()
            val h = rect.height.toDouble()
            if (w <= 0.0 || h <= 0.0) continue

            val rectArea = w * h
            if (rectArea < minAnchorArea) { rejectedTooSmall++; continue }
            if (rectArea > maxAnchorArea) { rejectedTooBig++; continue }

            val aspect = w / h
            if (aspect !in 0.6..1.67) { rejectedAspect++; continue }

            val contourArea = Imgproc.contourArea(contour)
            val solidity = if (rectArea > 0.0) contourArea / rectArea else 0.0

            val mp2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(mp2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(mp2f, approx, 0.04 * peri, true)
            val vertices = approx.total()
            approx.release()
            mp2f.release()

            // Strategy A: 4-vertex polygon with good solidity
            val isPolySquare = vertices == 4L && aspect in 0.75..1.33 && solidity >= 0.7
            // Strategy B: Any vertex count but very high solidity (filled square)
            val isFilledSquare = aspect in 0.75..1.33 && solidity >= 0.82

            if (isPolySquare || isFilledSquare) {
                candidates.add(AnchorCandidate(
                    center = AnchorPoint(rect.x + w / 2.0, rect.y + h / 2.0),
                    area = rectArea,
                    solidity = solidity,
                    aspect = aspect,
                    vertices = vertices,
                    width = w,
                    height = h
                ))
            } else {
                rejectedShape++
            }
        }

        Log.d(TAG, "Candidates: ${candidates.size} passed, rejected: " +
                "tooSmall=$rejectedTooSmall, tooBig=$rejectedTooBig, " +
                "aspect=$rejectedAspect, shape=$rejectedShape")

        for ((i, c) in candidates.withIndex()) {
            Log.d(TAG, "  Candidate $i: center=(${c.center.x.toInt()},${c.center.y.toInt()}) " +
                    "size=${c.width.toInt()}x${c.height.toInt()} area=${c.area.toInt()} " +
                    "aspect=${"%.2f".format(c.aspect)} solidity=${"%.2f".format(c.solidity)} vertices=${c.vertices}")
        }

        bin.release()
        hierarchy.release()

        val anchors = mutableListOf<AnchorPoint>()

        if (candidates.size >= 4) {
            // Filter by size consistency: anchors should all be roughly the same size
            val sizeFilteredCandidates = filterBySizeConsistency(candidates)
            Log.d(TAG, "After size consistency filter: ${sizeFilteredCandidates.size} candidates")

            val toSelect = if (sizeFilteredCandidates.size >= 4) sizeFilteredCandidates else candidates

            if (toSelect.size == 4) {
                anchors.addAll(toSelect.map { it.center })
            } else if (toSelect.size > 4 || forceCornerSelect) {
                // Pick 4 closest to image corners
                val corners = listOf(
                    AnchorPoint(0.0, 0.0),
                    AnchorPoint(imgW, 0.0),
                    AnchorPoint(imgW, imgH),
                    AnchorPoint(0.0, imgH)
                )
                val selected = mutableListOf<AnchorPoint>()
                val used = mutableSetOf<Int>()
                for (corner in corners) {
                    var bestIdx = -1
                    var bestDist = Double.MAX_VALUE
                    for ((idx, c) in toSelect.withIndex()) {
                        if (idx in used) continue
                        val dist = sqrt((c.center.x - corner.x) * (c.center.x - corner.x) +
                                (c.center.y - corner.y) * (c.center.y - corner.y))
                        if (dist < bestDist) { bestDist = dist; bestIdx = idx }
                    }
                    if (bestIdx >= 0) { selected.add(toSelect[bestIdx].center); used.add(bestIdx) }
                }
                anchors.addAll(selected)
            }
        } else if (candidates.isNotEmpty()) {
            Log.w(TAG, "Only ${candidates.size} candidates found (need 4)")
            anchors.addAll(candidates.map { it.center })
        }

        // Sort into TL, TR, BR, BL
        if (anchors.size == 4) {
            val sorted = anchors.sortedBy { it.y }
            val top = sorted.take(2).sortedBy { it.x }
            val bottom = sorted.takeLast(2).sortedBy { it.x }
            anchors.clear()
            anchors.add(top[0])     // TL
            anchors.add(top[1])     // TR
            anchors.add(bottom[1])  // BR
            anchors.add(bottom[0])  // BL

            Log.d(TAG, "Anchors: TL=(${top[0].x.toInt()},${top[0].y.toInt()}) " +
                    "TR=(${top[1].x.toInt()},${top[1].y.toInt()}) " +
                    "BR=(${bottom[1].x.toInt()},${bottom[1].y.toInt()}) " +
                    "BL=(${bottom[0].x.toInt()},${bottom[0].y.toInt()})")
        }

        return anchors
    }

    /**
     * Filter anchor candidates by size consistency.
     * Real anchors are all the same size on the sheet.
     * Other square-like elements (enrollment header boxes, text blocks) vary in size.
     *
     * Groups candidates by similar area, picks the group with exactly 4 (or closest to 4).
     */
    private fun filterBySizeConsistency(candidates: List<AnchorCandidate>): List<AnchorCandidate> {
        if (candidates.size <= 4) return candidates

        // Sort by area
        val sorted = candidates.sortedBy { it.area }

        // Group candidates whose area is within 50% of each other
        val groups = mutableListOf<MutableList<AnchorCandidate>>()
        var currentGroup = mutableListOf(sorted.first())

        for (i in 1 until sorted.size) {
            val prev = currentGroup.last()
            val curr = sorted[i]
            val ratio = curr.area / prev.area

            if (ratio <= 1.5) {
                currentGroup.add(curr)
            } else {
                groups.add(currentGroup)
                currentGroup = mutableListOf(curr)
            }
        }
        groups.add(currentGroup)

        Log.d(TAG, "Size consistency: ${groups.size} groups: ${groups.map { "${it.size} (area~${it.first().area.toInt()})" }}")

        // Prefer group with exactly 4 candidates
        val exactGroup = groups.find { it.size == 4 }
        if (exactGroup != null) return exactGroup

        // Otherwise return the group with 4+ candidates closest to 4
        val viableGroups = groups.filter { it.size >= 4 }.sortedBy { it.size }
        if (viableGroups.isNotEmpty()) return viableGroups.first()

        // No group has 4+, return all candidates
        return candidates
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
        totalExpectedBubbles: Int = 0, // if >0, use this as expected count (includes enrollment)
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
            val exactExpectedBubbles = if (totalExpectedBubbles > 0) {
                totalExpectedBubbles
            } else {
                totalQuestions * optionsPerQuestion
            }

            Log.d(TAG, "Expected: $exactExpectedBubbles bubbles (including enrollment if any)")

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
        enrollmentGrid: EnrollmentGrid? = null,
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
            anchor_bottom_left = anchors[3],
            enrollment_grid = enrollmentGrid
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

    /**
     * Splits all detected bubbles into enrollment grid bubbles and answer bubbles.
     *
     * Strategy: Sort all bubbles by Y. The enrollment grid is in the upper portion
     * of the sheet (above the answers). There's a significant Y-gap between the
     * enrollment grid (10 rows of digit bubbles) and the answer bubbles (25 rows).
     *
     * We find this gap and split at it.
     *
     * @return Pair of (EnrollmentGrid, answerBubbles)
     */
    private fun splitEnrollmentFromAnswers(
        grayMat: Mat,
        allBubbles: List<Bubble>,
        enrollmentColumns: Int,
        expectedAnswerBubbles: Int,
        anchorTopLeft: AnchorPoint,
        debugMapAdditionCallback: (String, Bitmap) -> Unit,
    ): Pair<EnrollmentGrid?, List<Bubble>> {
        if (allBubbles.isEmpty()) return null to allBubbles

        // Group all bubbles into rows by Y
        val sortedByY = allBubbles.sortedBy { it.y }
        val rows = groupIntoRows(sortedByY)


        OpenCvUtils.drawPoints(
            grayMat.toColoredWarped(),
            bubbles2DArray = rows,
        ).let {
            val bitmap = it.toBitmapSafe()
            debugMapAdditionCallback("enrollment-debug", bitmap)
            it.release()
            bitmap
        }

        Log.d(TAG, "splitEnrollmentFromAnswers: ${rows.size} rows total")

        if (rows.size <= 10) {
            // Not enough rows for enrollment + answers, return all as answers
            Log.w(TAG, "Not enough rows (${rows.size}) for enrollment grid, skipping")
            return null to allBubbles
        }

        // Find the largest Y-gap between consecutive rows — this is the boundary
        // between enrollment grid and answer area
        var maxGap = 0.0
        var splitRowIndex = -1
        for (i in 0 until rows.size - 1) {
            val currentRowY = rows[i].map { it.y }.average()
            val nextRowY = rows[i + 1].map { it.y }.average()
            val gap = nextRowY - currentRowY

            if (gap > maxGap) {
                maxGap = gap
                splitRowIndex = i
            }
        }

        if (splitRowIndex < 0) {
            Log.w(TAG, "Could not find Y-gap between enrollment and answers")
            return null to allBubbles
        }

        Log.d(TAG, "Enrollment/answer split: row $splitRowIndex, gap=${maxGap.roundToInt()}px")

        // Enrollment rows = rows 0..splitRowIndex
        val enrollmentRows = rows.subList(0, splitRowIndex + 1)
        // Answer rows = remaining
        val answerBubbles = rows.subList(splitRowIndex + 1, rows.size).flatten()

        // Build enrollment grid: group enrollment bubbles into columns by X
        val enrollmentBubbles = enrollmentRows.flatten().sortedBy { it.x }

        // Group into columns by X
        val enrollmentColumnsList = groupIntoColumns(enrollmentBubbles, enrollmentColumns)
//        val enrollmentColumnsList = enrollmentRows

        Log.d(TAG, "Enrollment: ${enrollmentRows.size} rows, ${enrollmentColumnsList.size} columns detected")

        if (enrollmentColumnsList.size != enrollmentColumns) {
            Log.w(TAG, "Expected $enrollmentColumns enrollment columns, got ${enrollmentColumnsList.size}")
        }

        // Convert to EnrollmentGrid with relative positions
        val digitColumns = enrollmentColumnsList.mapIndexed { colIdx, columnBubbles ->
            val sortedColumn = columnBubbles.sortedBy { it.y } // sort top to bottom (0-9)
            val options = sortedColumn.mapIndexed { digitIdx, bubble ->
                OptionBox(
                    option = "$digitIdx", // "0", "1", ..., "9"
                    x = bubble.x - anchorTopLeft.x,
                    y = bubble.y - anchorTopLeft.y,
                    r = bubble.r
                )
            }
            DigitColumn(column_index = colIdx, bubbles = options)
        }

        val grid = EnrollmentGrid(
            columns = digitColumns.size,
            digits = digitColumns
        )

        return grid to answerBubbles
    }

    /**
     * Groups bubbles into columns based on X coordinate.
     * Used for enrollment grid where bubbles in the same column share similar X values.
     */
    private fun groupIntoColumns(
        sortedBubbles: List<Bubble>,
        expectedColumns: Int
    ): List<List<Bubble>> {
        if (sortedBubbles.isEmpty()) return emptyList()

        // Sort by X
        val byX = sortedBubbles.sortedBy { it.x }

        val columns = mutableListOf<MutableList<Bubble>>()

        var count = 0
        for(i in 0 until expectedColumns) {
            val currentColumn = mutableListOf<Bubble>()
            for(j in 0 until 10) {
                currentColumn.add(byX[count])
                count++
            }
            columns.add(currentColumn)
        }

        return columns
    }
}