package com.example.ledgerscanner.base.utils.image

import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object OpenCvUtils {
    fun drawPoints(
        src: Mat,
        points: List<AnchorPoint>? = null,
        bubbles: List<Bubble>? = null,
        bubbles2DArray: List<List<Bubble>>? = null,
        bubblesWithColor: List<Pair<AnchorPoint, Boolean>>? = null,
        fillColor: Scalar = Scalar(255.0, 0.0, 0.0),
        textColor: Scalar = Scalar(255.0, 255.0, 0.0),
        radius: Int? = 10
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

        // 1) Draw 2D bubble grid (row-wise), label with running index
        var idx = 0
        bubbles2DArray?.forEach { row ->
            row.forEach { b ->
                draw(Point(b.x, b.y), "${idx++}")
            }
        }

        // 2) Draw simple point list
        points?.forEachIndexed { i, p -> draw(p.toPoint(), "$i") }

        // 3) Draw flat bubble list
        bubbles?.forEachIndexed { i, b -> draw(Point(b.x, b.y), "$i") }

        bubblesWithColor?.forEachIndexed { i, b ->
            draw(
                Point(b.first.x, b.first.y),
                pointRadius = 15,
                filledColor = if (b.second) Scalar(0.0, 255.0, 0.0) else Scalar(0.0, 0.0, 255.0)
            )
        }

        return out
    }

    fun detectAnchorInRoi(roiGray: Mat): Point? {
        val bin = Mat()
        // simple threshold (anchors are dark); tweak 50..90 as needed
        Imgproc.threshold(
            roiGray,
            bin, 60.0,
            255.0,
            Imgproc.THRESH_BINARY_INV
        )

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            bin,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        for (c in contours) {
            val peri = Imgproc.arcLength(
                MatOfPoint2f(*c.toArray()),
                true
            )
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*c.toArray()),
                approx,
                0.04 * peri,
                true
            )

            val roiW = roiGray.cols()
            val roiH = roiGray.rows()
            val roiArea = (roiW * roiH).toDouble()
            val minArea = 0.002 * roiArea   // 0.2% of ROI
            val maxArea = 0.25 * roiArea   // 25% of ROI
            if (approx.total() == 4L) {
                val approxMP = MatOfPoint(*approx.toArray())
                // must look like a convex quad
                if (!Imgproc.isContourConvex(approxMP)) {
                    approxMP.release()
                    // keep your existing 'continue' here
                } else {
                    val rect = Imgproc.boundingRect(approxMP)
                    val aspect = rect.width.toDouble() / rect.height.toDouble()

                    val area = Imgproc.contourArea(approxMP)
                    val rectArea = (rect.width * rect.height).toDouble().coerceAtLeast(1.0)
                    val solidity = area / rectArea

                    // reject circles by circularity (circles ≈ 1.0; squares clearly lower)
                    val peri2 = Imgproc.arcLength(MatOfPoint2f(*approxMP.toArray()), true)
                    val circularity =
                        if (peri2 > 1e-6) 4.0 * Math.PI * area / (peri2 * peri2) else 0.0

                    // Scale-robust checks:
                    // - near-square aspect
                    // - filled enough
                    // - area within [minArea, maxArea] fraction of ROI
                    // - not a circle
                    if (aspect in 0.8..1.25 && solidity > 0.70 && area in minArea..maxArea && circularity < 0.85) {
                        val cx = rect.x + rect.width / 2.0
                        val cy = rect.y + rect.height / 2.0

                        bin.release()
                        hierarchy.release()
                        approxMP.release()
                        approx.release()
                        return Point(cx, cy)
                    }
                    approxMP.release()
                }
                // keep your existing approx.release() if you didn't above
            }
        }
        bin.release(); hierarchy.release()
        return null
    }
}