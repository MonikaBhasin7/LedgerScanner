package com.example.ledgerscanner.feature.scanner.scan.model

import android.graphics.Bitmap

sealed class OmrResult(
    open val success: Boolean,
    open val reason: String?,
    open val finalBitmap: Bitmap?,
    open val debugBitmaps: Map<String, Bitmap>
)
data class OmrImageProcessResult(
    override val success: Boolean,
    override val reason: String? = null,
    override val finalBitmap: Bitmap? = null,
    val confidence: Double? = null, // heuristic confidence
    override val debugBitmaps: Map<String, Bitmap> = emptyMap()
) : OmrResult(success, reason, finalBitmap, debugBitmaps)

data class OmrTemplateResult(
    override val success: Boolean,
    override val reason: String? = null,
    override val finalBitmap: Bitmap? = null,
    override val debugBitmaps: Map<String, Bitmap> = emptyMap(),
    val templateJson: String? = null,
    val template: Template? = null
) : OmrResult(success, reason, finalBitmap, debugBitmaps)

data class TemplatePair(
    val templateJson: String? = null,
    val template: Template? = null
)