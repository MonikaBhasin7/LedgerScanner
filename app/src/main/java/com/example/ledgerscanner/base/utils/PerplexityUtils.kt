package com.example.ledgerscanner.base.utils

import android.content.Context
import android.graphics.Bitmap
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.android.Utils
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.pow

private fun matToBitmap(mat: Mat): Bitmap {
    val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    matToBitmap(mat, bmp)
    return bmp
}


/**
 * Utilities for OMR (Optical Mark Recognition) processing.
 *
 * NOTE: Call System.loadLibrary("opencv_java4") once at app startup,
 *       before using this class.
 */
object PerplexityUtils {

    // ------------ MAIN PIPELINE ------------

    fun processOMR(bitmap: Bitmap, sheetId: String): PreprocessResult {
        val debugMap = mutableMapOf<String, Bitmap>()
        // 1. Convert to gray + blur
        val gray = preprocessImage(bitmap)
        debugMap["gray"] = matToBitmap(gray)

        // 2. Find OMR sheet contour
        val contour = findOMRSheet(gray)

        // Draw contours for debug
        val contourMat = gray.clone()
        if (contour != null) {
            Imgproc.drawContours(
                contourMat,
                listOf(MatOfPoint(*contour.toArray())),
                -1,
                Scalar(0.0, 255.0, 0.0),
                3
            )
        }
        debugMap["contours"] = matToBitmap(contourMat)

        // 3. Deskew if contour found, otherwise fallback
        val warped = if (contour != null) deskew(gray, contour) else gray
        debugMap["warped"] = matToBitmap(warped)

        // 4. Crop region where bubbles are expected
        val answerRegion = cropBubbleRegion(warped)
        debugMap["answer_region"] = matToBitmap(answerRegion)

        // 5. Binarize cropped region
        val binImg = binarize(answerRegion)
        debugMap["binarized"] = matToBitmap(binImg)

        // 6. Find bubble contours
        val bubbleContours = findBubbleContours(binImg)
        val bubblesMat = binImg.clone()
        Imgproc.drawContours(bubblesMat, bubbleContours, -1, Scalar(0.0, 0.0, 255.0), 2)
        debugMap["bubbles"] = matToBitmap(bubblesMat)


        // 7. Extract bubbles → results
        val results = extractBubbles(binImg, bubbleContours)
        val resultJson = omrResultToJson(sheetId, results)

        return PreprocessResult(
            intermediate = debugMap,
            ok = true
        )
        // 8. Convert to JSON
//        return omrResultToJson(sheetId, results)
    }

    // ------------ STEP FUNCTIONS ------------

    fun preprocessImage(bitmap: Bitmap): Mat {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src) // converts to RGBA
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        src.release()
        return gray
    }

    fun findOMRSheet(gray: Mat): MatOfPoint2f? {
        val edged = Mat()
        Imgproc.Canny(gray, edged, 75.0, 200.0)
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            edged,
            contours,
            Mat(),
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        contours.sortByDescending { Imgproc.contourArea(it) }
        for (contour in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)
            if (approx.toArray().size == 4) {
                val ordered = orderCornersAsTLTRBRBL(approx.toArray())
                return MatOfPoint2f(*ordered)
            }
        }
        return null
    }

    fun deskew(gray: Mat, sheetContour: MatOfPoint2f): Mat {
        val srcCorners = sheetContour.toArray()
        val width = 2100
        val height = 2970
        val dstCorners = arrayOf(
            Point(0.0, 0.0),
            Point(width.toDouble(), 0.0),
            Point(width.toDouble(), height.toDouble()),
            Point(0.0, height.toDouble())
        )
        val transform =
            Imgproc.getPerspectiveTransform(MatOfPoint2f(*srcCorners), MatOfPoint2f(*dstCorners))
        val warped = Mat()
        Imgproc.warpPerspective(gray, warped, transform, Size(width.toDouble(), height.toDouble()))
        return warped
    }

    fun cropBubbleRegion(warped: Mat): Mat {
        val rowStart = (warped.rows() * 0.10).toInt()
        val rowEnd = (warped.rows() * 0.90).toInt()
        val colStart = (warped.cols() * 0.05).toInt()
        val colEnd = (warped.cols() * 0.85).toInt()
        return warped.submat(rowStart, rowEnd, colStart, colEnd)
    }

    fun binarize(region: Mat): Mat {
        val bin = Mat()
        Imgproc.adaptiveThreshold(
            region,
            bin,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            2.0
        )
        return bin
    }

    fun findBubbleContours(binImg: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            binImg,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        return contours.filter {
            val area = Imgproc.contourArea(it)
            val rect = Imgproc.boundingRect(it)
            val aspect = rect.width.toDouble() / rect.height
            val peri = Imgproc.arcLength(MatOfPoint2f(*it.toArray()), true)
            val circularity =
                if (peri > 1e-6) 4 * Math.PI * area / (peri * peri) else 0.0
            area > 100 && area < 1500 && aspect in 0.8..1.2 && circularity > 0.5
        }
    }

    data class BubbleResult(
        val question: Int,
        val option: Char,
        val filled: Boolean,
        val confidence: Double
    )

    fun extractBubbles(binImg: Mat, bubbleContours: List<MatOfPoint>): List<BubbleResult> {
        val sortedByY = bubbleContours.sortedBy { Imgproc.boundingRect(it).y }
        val rowGroups = sortedByY.groupBy {
            Imgproc.boundingRect(it).y / 40 // TODO: calibrate
        }

        val results = mutableListOf<BubbleResult>()
        val options = listOf('A', 'B', 'C', 'D')
        var qIndex = 1

        rowGroups.forEach { (_, bubbles) ->
            val sortedRow = bubbles.sortedBy { Imgproc.boundingRect(it).x }
            sortedRow.forEachIndexed { index, bubble ->
                val mask = Mat.zeros(binImg.size(), CvType.CV_8U)
                Imgproc.drawContours(mask, listOf(bubble), -1, Scalar(255.0), -1)
                val bubbleMasked = Mat()
                Core.bitwise_and(binImg, binImg, bubbleMasked, mask)
                val nonZero = Core.countNonZero(bubbleMasked)
                val rect = Imgproc.boundingRect(bubble)
                val area = rect.width * rect.height
                val fillPercent = if (area > 0) nonZero / area.toDouble() else 0.0
                val filled = fillPercent > 0.3
                val confidence = fillPercent
                results.add(
                    BubbleResult(
                        qIndex,
                        options.getOrElse(index) { '?' },
                        filled,
                        confidence
                    )
                )
                mask.release()
                bubbleMasked.release()
            }
            qIndex++
        }
        return results
    }

    fun omrResultToJson(sheetId: String, results: List<BubbleResult>): JSONObject {
        val obj = JSONObject()
        obj.put("sheet_id", sheetId)
        val questions = JSONArray()
        results.groupBy { it.question }.forEach { (qNo, bubbles) ->
            val filledOptions = bubbles.filter { it.filled }
            val best = filledOptions.maxByOrNull { it.confidence }
            val answer = when {
                filledOptions.size > 1 -> "MULTI"
                best == null -> "BLANK"
                else -> best.option.toString()
            }
            val confidence = best?.confidence ?: 0.0
            val qObj = JSONObject()
            qObj.put("qno", qNo)
            qObj.put("answer", answer)
            qObj.put("confidence", confidence)
            questions.put(qObj)
        }
        obj.put("questions", questions)
        return obj
    }

    // ------------ HELPERS ------------

    private fun orderCornersAsTLTRBRBL(pts: Array<Point>): Array<Point> {
        val tl = pts.minByOrNull { it.x + it.y }!!
        val br = pts.maxByOrNull { it.x + it.y }!!
        val remaining = pts.filter { it != tl && it != br }
        val (p1, p2) = remaining
        val (tr, bl) = if (p1.x > p2.x) Pair(p1, p2) else Pair(p2, p1)
        return arrayOf(tl, tr, br, bl)
    }
}