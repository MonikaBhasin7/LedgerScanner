package com.example.ledgerscanner.feature.scanner.scan.utils

import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Validates that the detected 4-anchor quadrilateral is geometrically consistent
 * with a flat sheet viewed under perspective projection.
 *
 * When the OMR sheet is on an uneven/curved surface, the anchors are NOT coplanar
 * and the perspective transform produces incorrect bubble positions. This validator
 * detects such cases and rejects the frame, prompting the user to flatten the sheet.
 *
 * Runs 4 cheap geometric checks (sub-millisecond, pure math):
 * 1. Convexity — quadrilateral must be convex
 * 2. Side ratio — opposite sides must have similar lengths
 * 3. Diagonal ratio — diagonals must have similar lengths
 * 4. Corner angles — each angle must be in a reasonable range
 */
class AnchorGeometryValidator @Inject constructor() {

    companion object {
        /** Max ratio between opposite side lengths (top/bottom or left/right) */
        const val MAX_SIDE_RATIO = 1.4

        /** Max ratio between diagonal lengths */
        const val MAX_DIAGONAL_RATIO = 1.3

        /** Minimum interior angle in degrees */
        const val MIN_CORNER_ANGLE = 50.0

        /** Maximum interior angle in degrees */
        const val MAX_CORNER_ANGLE = 135.0

        /** Expected number of anchor points */
        private const val EXPECTED_ANCHORS = 4
    }

    /**
     * Validate that the 4 detected anchors form a geometrically valid quadrilateral
     * consistent with a flat sheet under perspective projection.
     *
     * @param detectedAnchors Exactly 4 anchor points in clockwise order: TL, TR, BR, BL
     * @return GeometryValidationResult with isValid=true if sheet appears flat
     */
    fun validate(detectedAnchors: List<AnchorPoint>): GeometryValidationResult {
        require(detectedAnchors.size == EXPECTED_ANCHORS) {
            "Expected $EXPECTED_ANCHORS anchors, got ${detectedAnchors.size}"
        }

        val pts = detectedAnchors

        // Check 1: Convexity (cheapest — 4 cross products)
        if (!isConvex(pts)) {
            return GeometryValidationResult(
                isValid = false,
                rejectionReason = "Sheet edges not detected properly"
            )
        }

        // Check 2: Opposite side ratio consistency
        val sideRatioResult = checkSideRatios(pts)
        if (sideRatioResult != null) {
            return sideRatioResult
        }

        // Check 3: Diagonal ratio
        val diagonalResult = checkDiagonalRatio(pts)
        if (diagonalResult != null) {
            return diagonalResult
        }

        // Check 4: Interior angles
        val angleResult = checkAngles(pts)
        if (angleResult != null) {
            return angleResult
        }

        return GeometryValidationResult(isValid = true)
    }

    /**
     * Check that the quadrilateral is convex by verifying all cross products
     * of consecutive edge vectors have the same sign.
     */
    private fun isConvex(pts: List<AnchorPoint>): Boolean {
        var sign = 0
        for (i in 0 until 4) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            val c = pts[(i + 2) % 4]
            val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
            val s = if (cross > 0) 1 else if (cross < 0) -1 else 0
            if (s == 0) continue // collinear edge, skip
            if (sign == 0) sign = s
            else if (s != sign) return false
        }
        return true
    }

    /**
     * Check that opposite sides have similar lengths.
     * For a perspective-projected rectangle, opposite sides shrink/grow
     * together based on depth — the ratio is bounded.
     *
     * @return null if valid, GeometryValidationResult with rejection if invalid
     */
    private fun checkSideRatios(pts: List<AnchorPoint>): GeometryValidationResult? {
        val top = dist(pts[0], pts[1])
        val bottom = dist(pts[3], pts[2])
        val left = dist(pts[0], pts[3])
        val right = dist(pts[1], pts[2])

        val hRatio = maxOf(top, bottom) / minOf(top, bottom)
        val vRatio = maxOf(left, right) / minOf(left, right)

        if (hRatio > MAX_SIDE_RATIO || vRatio > MAX_SIDE_RATIO) {
            return GeometryValidationResult(
                isValid = false,
                rejectionReason = "Sheet appears bent. Please flatten it"
            )
        }
        return null
    }

    /**
     * Check that the two diagonals have similar lengths.
     * Large diagonal ratio difference indicates trapezoidal distortion from curvature.
     *
     * @return null if valid, GeometryValidationResult with rejection if invalid
     */
    private fun checkDiagonalRatio(pts: List<AnchorPoint>): GeometryValidationResult? {
        val d1 = dist(pts[0], pts[2]) // TL to BR
        val d2 = dist(pts[1], pts[3]) // TR to BL
        val ratio = maxOf(d1, d2) / minOf(d1, d2)

        if (ratio > MAX_DIAGONAL_RATIO) {
            return GeometryValidationResult(
                isValid = false,
                rejectionReason = "Sheet appears curved. Please flatten it"
            )
        }
        return null
    }

    /**
     * Check that all interior angles are within a reasonable range.
     * Extreme angles indicate local curvature pushing corners out of position.
     *
     * @return null if valid, GeometryValidationResult with rejection if invalid
     */
    private fun checkAngles(pts: List<AnchorPoint>): GeometryValidationResult? {
        for (i in 0 until 4) {
            val a = pts[(i + 3) % 4]
            val b = pts[i]
            val c = pts[(i + 1) % 4]
            val angle = angleDegrees(a, b, c)

            if (angle < MIN_CORNER_ANGLE || angle > MAX_CORNER_ANGLE) {
                return GeometryValidationResult(
                    isValid = false,
                    rejectionReason = "Sheet corners are distorted. Please flatten it"
                )
            }
        }
        return null
    }

    // --- Geometry helpers ---

    private fun dist(a: AnchorPoint, b: AnchorPoint): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculate the angle at vertex B formed by rays BA and BC, in degrees.
     */
    private fun angleDegrees(a: AnchorPoint, b: AnchorPoint, c: AnchorPoint): Double {
        val bax = a.x - b.x
        val bay = a.y - b.y
        val bcx = c.x - b.x
        val bcy = c.y - b.y

        val dot = bax * bcx + bay * bcy
        val magBA = sqrt(bax * bax + bay * bay)
        val magBC = sqrt(bcx * bcx + bcy * bcy)

        if (magBA == 0.0 || magBC == 0.0) return 0.0

        val cosAngle = (dot / (magBA * magBC)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle))
    }
}

/**
 * Result of anchor geometry validation.
 */
data class GeometryValidationResult(
    val isValid: Boolean,
    val rejectionReason: String? = null
)
