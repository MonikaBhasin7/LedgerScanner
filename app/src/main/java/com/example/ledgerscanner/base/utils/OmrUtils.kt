package com.example.ledgerscanner.base.utils

import com.example.ledgerscanner.feature.scanner.scan.model.Bubble
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object OmrUtils {

    fun drawPoints(
        src: Mat,
        points: List<Point>? = null,
        bubbles: List<Bubble>? = null,
        bubbles2DArray: List<List<Bubble>>? = null,
        color: Scalar = Scalar(0.0, 0.0, 255.0),
    ): Mat {
        val out = src.clone()

        bubbles2DArray?.let {
            var index = 0
            for (row in it) {
                for (p in row) {
                    // Draw small circle
                    Imgproc.circle(out, Point(p.x, p.y), 10, color, -1)

                    // Label point index (TL=0, TR=1, BR=2, BL=3)
                    Imgproc.putText(
                        out,
                        "$index",
                        Point(p.x + 15, p.y),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.8,
                        color,
                        2
                    )
                    index++
                }
            }
        }


        points?.let {
            for ((i, p) in it.withIndex()) {
                // Draw small circle
                Imgproc.circle(out, p, 10, color, -1)

                // Label point index (TL=0, TR=1, BR=2, BL=3)
                Imgproc.putText(
                    out,
                    "$i",
                    Point(p.x + 15, p.y),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    color,
                    2
                )
            }
        }
        bubbles?.let {
            for ((i, p) in it.withIndex()) {
                // Draw small circle
                Imgproc.circle(out, Point(p.x, p.y), 10, color, -1)

                // Label point index (TL=0, TR=1, BR=2, BL=3)
                Imgproc.putText(
                    out,
                    "$i",
                    Point(p.x + 15, p.y),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    color,
                    2
                )
            }
        }
        return out
    }
}