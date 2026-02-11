package com.example.ledgerscanner.feature.scanner.scan.utils

import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import java.util.LinkedList
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Tracks anchor positions across consecutive camera frames to ensure
 * the phone is held steady before triggering the expensive OMR pipeline.
 *
 * Requires [REQUIRED_STABLE_FRAMES] consecutive frames where all 4 anchors
 * have moved less than [MOVEMENT_THRESHOLD_PX] pixels from the previous frame.
 */
class FrameStabilityTracker @Inject constructor() {

    companion object {
        /** Number of consecutive stable frames required (~300ms at 20fps) */
        const val REQUIRED_STABLE_FRAMES = 7

        /** Max allowed per-anchor movement in pixels between consecutive frames */
        const val MOVEMENT_THRESHOLD_PX = 5.0

        /** Discard frames older than this to prevent stale data */
        private const val MAX_FRAME_AGE_MS = 1000L

        /** Expected number of anchors per frame */
        private const val EXPECTED_ANCHORS = 4
    }

    private val frameHistory = LinkedList<AnchorFrame>()

    /**
     * Add a new frame's anchor positions and check stability.
     *
     * @param anchors Must contain exactly 4 anchor points
     * @return StabilityResult indicating whether we've reached the required stable frame count
     */
    fun addFrame(anchors: List<AnchorPoint>): StabilityResult {
        require(anchors.size == EXPECTED_ANCHORS) {
            "Expected $EXPECTED_ANCHORS anchors, got ${anchors.size}"
        }

        val now = System.currentTimeMillis()

        // Expire stale frames
        while (frameHistory.isNotEmpty() && (now - frameHistory.first.timestamp) > MAX_FRAME_AGE_MS) {
            frameHistory.removeFirst()
        }

        val maxMovement = if (frameHistory.isNotEmpty()) {
            calculateMaxMovement(frameHistory.last.anchors, anchors)
        } else {
            0.0
        }

        // If any anchor moved too much, reset history and start over
        if (maxMovement > MOVEMENT_THRESHOLD_PX) {
            frameHistory.clear()
        }

        // Add current frame to history
        frameHistory.addLast(AnchorFrame(anchors, now))

        // Cap history size
        while (frameHistory.size > REQUIRED_STABLE_FRAMES) {
            frameHistory.removeFirst()
        }

        val stableCount = frameHistory.size
        val isStable = stableCount >= REQUIRED_STABLE_FRAMES

        return StabilityResult(
            isStable = isStable,
            stableFrameCount = stableCount,
            requiredFrames = REQUIRED_STABLE_FRAMES,
            maxMovement = maxMovement
        )
    }

    /**
     * Reset tracking. Called when scanning is re-enabled or anchors are lost.
     */
    fun reset() {
        frameHistory.clear()
    }

    /**
     * Calculate the maximum Euclidean distance across all anchor pairs
     * between the previous frame and the current frame.
     */
    private fun calculateMaxMovement(
        previous: List<AnchorPoint>,
        current: List<AnchorPoint>
    ): Double {
        var maxDist = 0.0
        for (i in previous.indices) {
            val dx = current[i].x - previous[i].x
            val dy = current[i].y - previous[i].y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > maxDist) maxDist = dist
        }
        return maxDist
    }
}

/**
 * Snapshot of anchor positions at a specific time.
 */
data class AnchorFrame(
    val anchors: List<AnchorPoint>,
    val timestamp: Long
)

/**
 * Result of stability check for a single frame.
 */
data class StabilityResult(
    val isStable: Boolean,
    val stableFrameCount: Int,
    val requiredFrames: Int,
    val maxMovement: Double
)
