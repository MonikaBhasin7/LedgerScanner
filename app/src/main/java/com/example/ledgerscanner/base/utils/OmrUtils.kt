package com.example.ledgerscanner.base.utils

import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object OmrUtils {
    fun drawPoints(
        src: Mat,
        points: List<AnchorPoint>? = null,
        bubbles: List<Bubble>? = null,
        bubbles2DArray: List<List<Bubble>>? = null,
        fillColor: Scalar = Scalar(255.0, 0.0, 0.0),
        textColor: Scalar = Scalar(255.0, 255.0, 0.0),
        radius: Int? = 10
    ): Mat {
        val out = src.clone()

        fun draw(pt: Point, label: String) {
            Imgproc.circle(
                out,
                pt,
                radius ?: 10,
                fillColor,
                -1
            )
            Imgproc.putText(
                out,
                label,
                Point(pt.x - 4, pt.y + 2),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.6,
                textColor,
                2
            )
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

        return out
    }
}