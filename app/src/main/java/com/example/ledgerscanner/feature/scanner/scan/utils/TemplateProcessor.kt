package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.OmrUtils
import com.example.ledgerscanner.base.utils.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import com.example.ledgerscanner.feature.scanner.scan.model.OptionBox
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Question
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.google.gson.Gson
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class TemplateProcessor {

    @WorkerThread
    fun generateTemplateJson(
        inputBitmap: Bitmap,
        debug: Boolean = true
    ): PreprocessResult {
        val debugMap = mutableMapOf<String, Bitmap>()

        // 1. Convert bitmap to grayscale
        val srcMat = Mat()
        Utils.bitmapToMat(inputBitmap, srcMat)
        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY)
        if (debug) debugMap["gray"] = grayMat.toBitmapSafe()

        // 2. Detect 4 anchor squares
        val anchorPoints = detectAnchorSquares(grayMat)
        if (anchorPoints.size != 4) {
            return PreprocessResult(
                ok = false,
                reason = "Could not detect 4 anchor points",
                warpedBitmap = null,
                transformMatrix = null,
                confidence = 0.0,
                intermediate = debugMap
            )
        }

        // 3. Debug visualization: draw anchor points
        val anchorOverlay = OmrUtils.drawPoints(
            srcMat,
            points = anchorPoints,
            color = Scalar(0.0, 0.0, 255.0)
        )
        if (debug) debugMap["anchors"] = anchorOverlay.toBitmapSafe()

        // 4. Detect bubble centers
        val bubbleCenters = detectBubbleCenters(grayMat)
        val bubbles2DArray = generate2DArrayOfBubbles(bubbleCenters)
//        val relativeDistanceBubbles2DArray =
//            findDistanceBwAnchorAndBubbles(
//                anchorPoints[0],
//                bubbles2DArray
//            )
        val templateJson = generateTemplateJsonSimple(
            anchorPoints,
            bubbles2DArray,
            srcMat.size()
        )
        println("templateJson - $templateJson")
//        println("relativeDistanceBubbles2DArray - $relativeDistanceBubbles2DArray")
        // 5. Debug visualization: draw bubble centers
        val bubbleOverlay =
            OmrUtils.drawPoints(
                srcMat,
                bubbles = bubbleCenters,
                color = Scalar(0.0, 0.0, 0.0)
            )
        if (debug) debugMap["bubbles"] = bubbleOverlay.toBitmapSafe()




        return PreprocessResult(
            ok = true,
            reason = null,
            warpedBitmap = null,
            transformMatrix = null,
            confidence = 0.0,
            intermediate = debugMap
        )
    }

    fun generateTemplateJsonSimple(
        anchors: List<Point>,
        bubbleGrid: List<List<Bubble>>,
        size: Size,
    ): String {
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

        return gson.toJson(template)
    }

    private fun findDistanceBwAnchorAndBubbles(
        topLeftAnchorPoint: Point,
        bubbles2DArray: MutableList<MutableList<Point>>
    ): MutableList<MutableList<Point>> {
        val relativeDistanceBubbles2DArray =
            bubbles2DArray.map { it.toMutableList() }.toMutableList()
        for (row in 0 until bubbles2DArray.size) {
            for (col in 0 until bubbles2DArray[row].size) {
                val point = bubbles2DArray[row][col]
                val x = point.x - topLeftAnchorPoint.x
                val y = point.y - topLeftAnchorPoint.y
                relativeDistanceBubbles2DArray[row][col] = Point(x, y)
            }
        }
        return relativeDistanceBubbles2DArray
    }

    private fun generate2DArrayOfBubbles(bubbleCenters: List<Bubble>): MutableList<MutableList<Bubble>> {
        var ptr = 0
        val bubbleGrid = mutableListOf<MutableList<Bubble>>()
        if (bubbleCenters.isEmpty()) return bubbleGrid

        var rowPointer = 0
        bubbleGrid.add(mutableListOf())
        while (ptr < bubbleCenters.size) {
            if ((ptr + 1) >= bubbleCenters.size) {
                bubbleGrid[rowPointer].add(bubbleCenters[ptr])
                break
            }
            if (bubbleCenters[ptr].y == bubbleCenters[ptr + 1].y) {
                bubbleGrid[rowPointer].add(bubbleCenters[ptr])
            } else {
                bubbleGrid[rowPointer].add(bubbleCenters[ptr])
                bubbleGrid.add(mutableListOf())
                rowPointer++
            }
            ptr++
        }
        return bubbleGrid
    }


    private fun detectBubbleCenters(gray: Mat): List<Bubble> {
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(9.0, 9.0), 2.0)

        val circles = Mat()
        Imgproc.HoughCircles(
            blurred,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,                 // dp: inverse ratio of resolution
            20.0,                // minDist between centers
            100.0,               // param1: higher Canny threshold
            30.0,                // param2: smaller = more circles, larger = fewer
            8,                  // minRadius
            20                   // maxRadius
        )

        val centers = mutableListOf<Bubble>()
        for (i in 0 until circles.cols()) {
            val data = circles.get(0, i) ?: continue
            val x = data[0]
            val y = data[1]
            val r = data[2]
            centers.add(Bubble(x, y, r))
            // you can draw circle here if you want
        }

        centers.sortWith(compareBy<Bubble> { it.y }.thenBy { it.x })

        blurred.release()
        circles.release()
        return centers
    }

    fun detectAnchorSquares(gray: Mat, debug: Boolean = false): List<Point> {
        val anchors = mutableListOf<Point>()

        // 1. Threshold (since anchors are black)
        val bin = Mat()
        Imgproc.threshold(gray, bin, 50.0, 255.0, Imgproc.THRESH_BINARY_INV)

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
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.04 * peri, true)

            if (approx.total() == 4L) {
                val rect = Imgproc.boundingRect(MatOfPoint(*approx.toArray()))
                val aspect = rect.width.toDouble() / rect.height.toDouble()
                if (aspect in 0.8..1.2) { // close to square
                    val area = rect.width * rect.height
                    if (area in 200..5000) { // filter by size
                        val center = Point(
                            rect.x + rect.width / 2.0,
                            rect.y + rect.height / 2.0
                        )
                        anchors.add(center)
                    }
                }
            }
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

        // 5. Optional debug
        if (debug) {
            val debugMat = Mat()
            Imgproc.cvtColor(gray, debugMat, Imgproc.COLOR_GRAY2BGR)
            anchors.forEachIndexed { i, pt ->
                Imgproc.circle(debugMat, pt, 10, Scalar(0.0, 0.0, 255.0), 2)
                Imgproc.putText(
                    debugMat,
                    "$i",
                    Point(pt.x + 15, pt.y),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    Scalar(255.0, 0.0, 0.0),
                    2
                )
            }
            // save/show debugMat if needed
            debugMat.release()
        }

        bin.release()
        hierarchy.release()

        return anchors
    }
}