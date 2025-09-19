package com.example.ledgerscanner.feature.scanner.scan.ui

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.utils.ImageUtils
import com.example.ledgerscanner.base.utils.ImageUtils.findDocumentContour
import com.example.ledgerscanner.base.utils.ImageUtils.warpToRectangle
import com.example.ledgerscanner.base.utils.bitmapToGrayMat
import com.example.ledgerscanner.base.utils.computeBrightness
import com.example.ledgerscanner.base.utils.computeSharpness
import com.example.ledgerscanner.base.utils.denoise
import com.example.ledgerscanner.base.utils.downscaleForDetection
import com.example.ledgerscanner.base.utils.equalizeContrast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.io.File
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.base.utils.ImageUtils.binarizeForBubbles
import com.example.ledgerscanner.base.utils.isImageBlurry
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog

class PreviewImageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri: Uri? = intent.getParcelableExtra("image_uri") ?: run {
            finish(); return
        }


        setContent {
            LedgerScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PreviewImageScreen(imageUri = imageUri!!, onClose = { finish() })
                }
            }
        }
    }

    @Composable
    fun PreviewImageScreen(imageUri: Uri, onClose: () -> Unit, onSubmit: (File) -> Unit = {}) {

        var showFinalProcessedImageDialog by remember { mutableStateOf(false) }
        var preProcessImage by remember { mutableStateOf<PreprocessResult?>(null) }

        val context = LocalContext.current
        // decode once on background thread and remember
        val bitmap by produceState<Bitmap?>(initialValue = null, imageUri) {
            value =
                ImageUtils.loadBitmapCorrectOrientation(
                    context,
                    imageUri,
                    reqWidth = 1080,
                    reqHeight = 1920
                )
        }

        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {

            if (showFinalProcessedImageDialog && preProcessImage?.warpedBitmap != null) {
                WarpedImageDialog(
                    warpedBitmap = preProcessImage?.warpedBitmap,
                    onDismiss = { showFinalProcessedImageDialog = false },
                    onRetry = {
                        showFinalProcessedImageDialog = false
                    },
                    onSave = {
                        showFinalProcessedImageDialog = false
                    }
                )
            } else {
                Column {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Captured image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } ?: run {
                            GenericLoader()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(color = Grey200)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GenericButton(
                                text = "Rescan",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onClose()
                                }
                            )

                            bitmap?.let { bm ->
                                GenericButton(
                                    text = "Submit",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        coroutineScope.launch {
                                            val result = preprocessFile(bm)
                                            if (result.ok) {
                                                preProcessImage = result
                                                showFinalProcessedImageDialog = true
                                            } else {
                                                result.reason?.let {
                                                    Toast.makeText(
                                                        context, it,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } ?: run {
                                                    Toast.makeText(
                                                        context, "Not able to process the image",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }


    }

    /**
     * High-level preprocessing function you call after capture.
     *
     * Steps performed:
     *  1. Convert input Bitmap -> grayscale Mat
     *  2. Denoise / blur to reduce small speckle noise
     *  3. Adaptive threshold to get binary sheet for contour detection
     *  4. Find document contour (largest quadrilateral)
     *  5. Perspective transform (warp) to get top-down sheet
     *  6. Enhance contrast (CLAHE) and return a cleaned Bitmap
     *
     * Returns a Bitmap of the processed, top-down OMR sheet.
     *
     * IMPORTANT: this should run on a background thread. This function is a suspend function
     * and internally switches to Dispatchers.Default for CPU work.
     */
    suspend fun preprocessFile(originalBitmap: Bitmap): PreprocessResult =
        withContext(Dispatchers.Default) {
            // defensive: ensure OpenCV native library loaded (you should do this once on app startup)
            try {
                // ----- 1) Pre-check: rotation / orientation -----
                // Ensure originalBitmap is correctly rotated already. If not, fix EXIF rotation BEFORE calling this function.

                // ----- 2) Downscale for quick checks -----
                val down = originalBitmap.downscaleForDetection() // returns Bitmap
                val isBlurry = down.isImageBlurry()
                if (isBlurry) {
                    return@withContext PreprocessResult(
                        ok = false,
                        reason = "Image is blurred",
                        warpedBitmap = null,
                        transformMatrix = null,
                        confidence = 0.0,
                        intermediate = mapOf()
                    )
                }

                // ----- 3) Convert downscaled bitmap to Mat (grayscale) -----
                // I expect bitmapToGrayMat() returns a Mat (single-channel CV_8UC1).
                val grayDown: Mat = down.bitmapToGrayMat() // ensure this returns CV_8UC1

                // ----- 4) Denoise + equalize -----
                val den = grayDown.denoise()           // Mat CV_8UC1
                val eq = den.equalizeContrast()        // Mat CV_8UC1

                // ----- 5) Quick quality checks -----
                val brightness = eq.computeBrightness() // Double or Int (0..255)
                val sharpness = eq.computeSharpness()   // Double
                if (brightness < 45) {
                    // release mats
                    grayDown.release(); den.release(); eq.release()
                    return@withContext PreprocessResult(
                        ok = false,
                        reason = "Too dark",
                        warpedBitmap = null,
                        transformMatrix = null,
                        confidence = 0.0,
                        intermediate = mapOf()
                    )
                }

                // ----- 6) Find document contour in downscaled image -----
                val corners =
                    findDocumentContour(eq) // should return MatOfPoint2f or null (points in downscale coords)
                if (corners == null) {
                    grayDown.release(); den.release(); eq.release()
                    return@withContext PreprocessResult(
                        ok = false,
                        reason = "Document corners not found",
                        warpedBitmap = null,
                        transformMatrix = null,
                        confidence = 0.2,
                        intermediate = mapOf("preview" to down) // optionally return downscaled preview
                    )
                }

                // ----- 7) Convert original Bitmap -> Mat (use full resolution for warp) -----
                val origMat = Mat()
                // Utils.bitmapToMat converts ARGB_8888 Bitmap into CV_8UC4 (RGBA) by default
                Utils.bitmapToMat(originalBitmap, origMat)

                // Convert RGBA -> GRAY for any checks (if needed)
                val srcGrayOrig = Mat()
                Imgproc.cvtColor(origMat, srcGrayOrig, Imgproc.COLOR_RGBA2GRAY)

                // ----- 8) Map corners from downscaled coords back to original scale -----
                val scaleX = originalBitmap.width.toDouble() / down.width.toDouble()
                val scaleY = originalBitmap.height.toDouble() / down.height.toDouble()

                // corners: MatOfPoint2f in downscale coordinates. Convert to array of Point in original coords.
                val downPoints = corners.toArray() // Array<Point> (points in downscale)
                // ensure corners length == 4
                if (downPoints.size < 4) {
                    // cleanup
                    grayDown.release(); den.release(); eq.release()
                    origMat.release(); srcGrayOrig.release()
                    corners.release()
                    return@withContext PreprocessResult(false, "Insufficient corner points")
                }
                // map to original
                val origPoints =
                    downPoints.map { p -> Point(p.x * scaleX, p.y * scaleY) }.toTypedArray()
                val cornersOrig = MatOfPoint2f(*origPoints)

                // ----- 9) Warp to rectangle (target DPI/size) -----
                // Ensure warpToRectangle expects source MatOfPoint2f in order [tl, tr, br, bl].
                val targetW = 1654 // e.g., A4 at 150 dpi ~ 1654x2339
                val targetH = 2339
                val warped = warpToRectangle(
                    origMat,
                    cornersOrig,
                    targetWidth = targetW,
                    targetHeight = targetH
                )
                // warpToRectangle should return a Mat in BGR or RGBA depending on implementation.
                // I assume returned Mat is BGR (common in OpenCV), convert to RGBA for bitmap.

                val warpedRgba = Mat()
                Imgproc.cvtColor(warped, warpedRgba, Imgproc.COLOR_BGR2RGBA)

                // create bitmap correctly
                val warpedBmp = createBitmap(warpedRgba.cols(), warpedRgba.rows())
                Utils.matToBitmap(warpedRgba, warpedBmp)

                // ----- 10) Prepare binary image for bubble detection -----
                val warpedGray = Mat()
                Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)
                val binary =
                    binarizeForBubbles(warpedGray) // returns single-channel Mat CV_8UC1 (0/255)
                val binaryRgba = Mat()
                Imgproc.cvtColor(binary, binaryRgba, Imgproc.COLOR_GRAY2RGBA)
                val binaryBmp = createBitmap(binaryRgba.cols(), binaryRgba.rows())
                Utils.matToBitmap(binaryRgba, binaryBmp)

                // optional: compute a numeric confidence using corner quality / sharpness etc
//                val confidence = computeConfidenceFrom(corners, sharpness, brightness)

                // ----- 11) release mats to free native memory -----
                grayDown.release()
                den.release()
                eq.release()
                corners.release()
                origMat.release()
                srcGrayOrig.release()
                cornersOrig.release()
                warped.release()
                warpedRgba.release()
                warpedGray.release()
                binary.release()
                binaryRgba.release()

                // ----- 12) return result -----
                PreprocessResult(
                    ok = true,
                    reason = null,
                    warpedBitmap = warpedBmp,
                    transformMatrix = null, // optional: return perspective matrix if you need it
                    confidence = 0.9,
                    intermediate = mapOf("binary" to binaryBmp)
                )
            } catch (e: Exception) {
                return@withContext PreprocessResult(false, reason = e.message ?: "Unknown error")
            }
        }
}

data class PreprocessResult(
    val ok: Boolean,
    val reason: String? = null,
    val warpedBitmap: Bitmap? = null,         // top-down, cropped OMR sheet
    val transformMatrix: Mat? = null,         // 3x3 perspective matrix (optional)
    val confidence: Double = 1.0,             // heuristic confidence (0..1)
    val intermediate: Map<String, Bitmap> = emptyMap() // for debug (gray, edged, thresh)
)