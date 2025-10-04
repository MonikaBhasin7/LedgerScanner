package com.example.ledgerscanner.feature.scanner.scan.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class OmrResult(
    open val success: Boolean,
    open val reason: String?,
    open val finalBitmap: Bitmap?,
    open val debugBitmaps: HashMap<String, Bitmap>
) : Parcelable

@Parcelize
data class OmrImageProcessResult(
    override val success: Boolean,
    override val reason: String? = null,
    override val finalBitmap: Bitmap? = null,
    val confidence: Double? = null, // heuristic confidence
    override val debugBitmaps: HashMap<String, Bitmap> = hashMapOf(),
    val marks: List<Boolean>? = null
) : OmrResult(success, reason, finalBitmap, debugBitmaps)

@Parcelize
data class OmrTemplateResult(
    override val success: Boolean,
    override val reason: String? = null,
    override val finalBitmap: Bitmap? = null,
    override val debugBitmaps: HashMap<String, Bitmap> = hashMapOf(),
    val templateJson: String? = null,
    val template: Template? = null
) : OmrResult(success, reason, finalBitmap, debugBitmaps)

@Parcelize
data class TemplatePair(
    val templateJson: String? = null,
    val template: Template? = null
) : Parcelable