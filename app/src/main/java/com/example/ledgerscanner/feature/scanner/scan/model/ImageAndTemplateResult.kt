package com.example.ledgerscanner.feature.scanner.scan.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult
import kotlinx.parcelize.Parcelize

sealed class OmrResult(
    open val success: Boolean,
    open val reason: String?,
    open val finalBitmap: Bitmap?,
    open val debugBitmaps: Map<String, Bitmap>
) : Parcelable

@Parcelize
data class OmrImageProcessResult(
    override val success: Boolean,
    override val reason: String? = null,
    override val finalBitmap: Bitmap? = null,
    override val debugBitmaps: Map<String, Bitmap> = mutableMapOf(),
    val rawBitmap: Bitmap? = null,
    val detectedBubbles: List<BubbleResult>? = null,// Detection results
    val evaluation: EvaluationResult? = null, // Evaluation results (optional)
    val barcodeId: String? = null,
    val enrollmentNumber: String? = null
) : OmrResult(success, reason, finalBitmap, debugBitmaps)

@Parcelize
data class OmrTemplateResult(
    override val success: Boolean,
    override val reason: String? = null,
    override val finalBitmap: Bitmap? = null,
    override val debugBitmaps: Map<String, Bitmap> = mutableMapOf(),
    val templateJson: String? = null,
    val template: Template? = null
) : OmrResult(success, reason, finalBitmap, debugBitmaps)

@Parcelize
data class TemplatePair(
    val templateJson: String? = null,
    val template: Template? = null
) : Parcelable