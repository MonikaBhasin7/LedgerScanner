package com.example.ledgerscanner.feature.scanner.scan.ui

import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

class PreviewImageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("image_path") ?: run {
            finish(); return
        }

        setContent {
            LedgerScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PreviewImageScreen(imagePath = path, onClose = { finish() })
                }
            }
        }
    }

    @Composable
    fun PreviewImageScreen(imagePath: String, onClose: () -> Unit, onSubmit: (File) -> Unit = {}) {

        // decode once on background thread and remember
        val bitmap by produceState<Bitmap?>(initialValue = null, imagePath) {
            // run in background automatically
            value =
                ImageUtils.loadCorrectlyOrientedBitmap(imagePath, reqWidth = 1080, reqHeight = 1920)
        }

        val coroutineScope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {
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
                                    println("Pre Process Image - $result")
                                }
                            }
                        )
                    }
                }
            }
        }
    }


    suspend fun preprocessFile(originalBitmap: Bitmap): PreprocessResult =
        withContext(Dispatchers.Default) {
            try {
                val down = originalBitmap.downscaleForDetection()
                val gray = down.bitmapToGrayMat()
                val den = gray.denoise()
                val eq = den.equalizeContrast()

                // quick quality checks
                val brightness = eq.computeBrightness()
                val sharpness = eq.computeSharpness()
                if (brightness < 45) return@withContext PreprocessResult(
                    false,
                    "Too dark",
                    null,
                    null,
                    0.0
                )

                val corners = findDocumentContour(eq)
                if (corners == null) {
                    // fallback: low confidence central crop
                    return@withContext PreprocessResult(
                        false,
                        "Document corners not found",
                        null,
                        null,
                        0.2
                    )
                }

                // for warping, convert orig -> Mat (use higher resolution)
                val origMat = Mat()
                Utils.bitmapToMat(originalBitmap, origMat)
                val srcGrayOrig = Mat()
                Imgproc.cvtColor(origMat, srcGrayOrig, Imgproc.COLOR_RGBA2GRAY)

                // Map corners from downscaled coords back to original scale:
                val scaleX = originalBitmap.width.toDouble() / down.width.toDouble()
                val scaleY = originalBitmap.height.toDouble() / down.height.toDouble()
                val cornersOrig =
                    MatOfPoint2f(*corners.toArray().map { Point(it.x * scaleX, it.y * scaleY) }
                        .toTypedArray())

                val warped =
                    warpToRectangle(origMat, cornersOrig, targetWidth = 1654, targetHeight = 2339)
                val warpedBmp =
                    createBitmap(warped.cols(), warped.rows())
                val warpedRgb = Mat()
                Imgproc.cvtColor(warped, warpedRgb, Imgproc.COLOR_BGR2RGBA)
                Utils.matToBitmap(warpedRgb, warpedBmp)

                // threshold for bubbles
                val warpedGray = Mat()
                Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)
                val binary = binarizeForBubbles(warpedGray)
                val binaryBmp =
                    createBitmap(binary.cols(), binary.rows())
                Imgproc.cvtColor(binary, warpedRgb, Imgproc.COLOR_GRAY2RGBA)
                Utils.matToBitmap(warpedRgb, binaryBmp)

                PreprocessResult(
                    ok = true,
                    warpedBitmap = warpedBmp,
                    transformMatrix = null, // you can return M if needed,
                    confidence = 0.9,
                    intermediate = mapOf("binary" to binaryBmp)
                )
            } catch (e: Exception) {
                PreprocessResult(false, reason = e.message)
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