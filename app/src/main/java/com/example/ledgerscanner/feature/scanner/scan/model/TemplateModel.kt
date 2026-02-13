package com.example.ledgerscanner.feature.scanner.scan.model

import android.content.Context
import android.os.Parcelable
import com.example.ledgerscanner.base.errors.OmrTemplateErrors
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.utils.AssetUtils
import com.google.gson.JsonSyntaxException
import kotlinx.parcelize.Parcelize
import org.opencv.core.Point
import java.io.FileNotFoundException
import kotlin.math.roundToInt

@Parcelize
data class Template(
    val name: String? = null,
    val imageUrl: String? = null,
    val version: String? = "1.0",
    val notes: String? = null,
    val sheet_width: Double = 0.0,
    val sheet_height: Double = 0.0,
    val options_per_question: Int,
    val questions: List<Question>,
    val anchor_top_left: AnchorPoint,
    val anchor_top_right: AnchorPoint,
    val anchor_bottom_right: AnchorPoint,
    val anchor_bottom_left: AnchorPoint,
    val enrollment_grid: EnrollmentGrid? = null,
) : Parcelable {
    private var totalBubbles: Int? = null
    fun totalBubbles(): Int? {
        if (totalBubbles == null) {
            totalBubbles = questions.size * 4 //todo monika make it generic in future
        }
        return totalBubbles
    }

    fun getTotalQuestions(): Int {
        return questions.size
    }

    fun getAnchorListClockwise(): List<AnchorPoint> {
        return listOf(
            anchor_top_left,
            anchor_top_right,
            anchor_bottom_right,
            anchor_bottom_left,
        )
    }

    fun getAverageRadius(): Int {
        val allRadii = questions
            .flatMap { it.options }
            .map { it.r }

        if (allRadii.isEmpty()) return 0

        return allRadii.average().roundToInt()
    }

    companion object {

        fun loadOmrTemplateSafe(
            context: Context,
            jsonFile: String
        ): OperationResult<Template> {
            return try {
                return OperationResult.Success(
                    AssetUtils.loadJsonFromAssets<Template>(context, jsonFile)
                )
            } catch (e: FileNotFoundException) {
                OperationResult.Error(OmrTemplateErrors.TEMPLATE_NOT_FOUND)
            } catch (e: JsonSyntaxException) {
                OperationResult.Error(OmrTemplateErrors.TEMPLATE_DECODE_FAILED)
            } catch (e: Exception) {
                OperationResult.Error(OmrTemplateErrors.TEMPLATE_LOAD_UNKNOWN_ERROR)
            }
        }
    }
}

@Parcelize
data class Grid(
    val start_x: Int?,
    val start_y: Int?,
    val col_spacing: Int?,
    val row_spacing: Int?,
    val bubble_w: Int?,
    val bubble_h: Int?,
    val padding: Int? = 0
) : Parcelable

@Parcelize
data class Question(
    val q_no: Int,
    val options: List<OptionBox>
) : Parcelable

@Parcelize
data class OptionBox(
    val option: String,
    val x: Double,
    val y: Double,
    val r: Double
) : Parcelable

@Parcelize
data class Bubble(
    val x: Double,
    val y: Double,
    val r: Double,
) : Parcelable

@Parcelize
data class AnchorPoint(
    val x: Double,
    val y: Double
) : Parcelable {
    fun toPoint(): Point {
        return Point(x, y)
    }
}

data class Gap(
    val index: Int,    // Index of bubble before the gap
    val size: Double   // Size of the gap in pixels
)

data class HoughParams(
    val name: String,
    val minDist: Double,
    val param2: Double,
    val minRadius: Int,
    val maxRadius: Int
)

enum class OmrTemplateType(val fileName: String) {
    TEN_QUESTIONS("template_omr_10_ques.json"),
    SIXTEEN_QUESTIONS("template_omr_16_ques.json"),
    SIXTEEN_DISTORTED_QUESTIONS("template_omr_16_distorted_ques.json");
}

/**
 * Enrollment number grid — a grid of columns where each column has 10 bubbles (digits 0-9).
 * Students fill one bubble per column to encode their enrollment/roll number.
 *
 * Example: 10 columns × 10 digits = encodes a 10-digit enrollment number.
 */
@Parcelize
data class EnrollmentGrid(
    val columns: Int,               // number of digit positions (e.g., 10)
    val digits: List<DigitColumn>   // each column's bubble positions
) : Parcelable

/**
 * A single column in the enrollment grid representing one digit position.
 * Contains 10 bubbles (one for each digit 0-9).
 */
@Parcelize
data class DigitColumn(
    val column_index: Int,          // 0-based position in the enrollment number
    val bubbles: List<OptionBox>    // 10 bubbles for digits 0-9, positions relative to TL anchor
) : Parcelable