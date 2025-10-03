package com.example.ledgerscanner.base.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import androidx.core.graphics.createBitmap

object ImageUtils {
    @WorkerThread
    fun loadBitmapCorrectOrientation(
        context: Context,
        uri: Uri,
        reqWidth: Int = 1080,
        reqHeight: Int = 1920
    ): Bitmap? {
        try {
            // 1) Read EXIF orientation from the URI via InputStream (works with content://)
            val exifOrientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // On Android 7.0+ we can read EXIF directly from InputStream
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED
                        )
                    } else {
                        // On older Android we need a file path, not stream
                        val path = uri.path
                        if (path != null) {
                            ExifInterface(path).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED
                            )
                        } else {
                            ExifInterface.ORIENTATION_UNDEFINED
                        }
                    }
                } ?: ExifInterface.ORIENTATION_UNDEFINED
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_UNDEFINED
            }

            val rotationDegrees = when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            // 2) Decode bitmap (sampled down to reqWidth/reqHeight to avoid OOM)
            val decoded: Bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // ImageDecoder automatically honors EXIF orientation (applies rotation)
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val origW = info.size.width
                        val origH = info.size.height
                        val scale =
                            maxOf(1f, origW.toFloat() / reqWidth, origH.toFloat() / reqHeight)
                        val targetW = (origW / scale).toInt().coerceAtLeast(1)
                        val targetH = (origH / scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    // Decode bounds first then sample
                    val input1 = context.contentResolver.openInputStream(uri) ?: return null
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input1, null, opts)
                    input1.close()

                    val sample = calculateInSampleSize(opts, reqWidth, reqHeight)
                    val input2 = context.contentResolver.openInputStream(uri) ?: return null
                    val opts2 = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inJustDecodeBounds = false
                    }
                    val bmp = BitmapFactory.decodeStream(input2, null, opts2)
                    input2.close()
                    bmp
                }
            } catch (e: Exception) {
                try {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } catch (ex: Exception) {
                    null
                }
            } ?: return null


            /*
             Strategy summary:
             - On API>=28 ImageDecoder SHOULD already apply EXIF rotation.
               So we generally MUST NOT rotate after decode.
             - On older APIs we MUST rotate according to EXIF.
             - But in practice some camera/provider combos may NOT include EXIF or decoders might behave differently.
             - We'll apply a small heuristic: if API>=28 and EXIF says rotation != 0 but the decoded bitmap
               already looks rotated (dimensions swapped), we assume ImageDecoder handled it and skip manual rotate.
             */

            val needsManualRotate = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    // ImageDecoder usually auto-applies rotation. If rotationDegrees != 0, check if decoded looks rotated
                    // If rotationDegrees is 90/270, then after rotation width/height swap. If decoder already rotated,
                    // decoded.width < decoded.height for portrait expectation. We'll rotate only if it still matches wrong orientation.
                    when (rotationDegrees) {
                        90, 270 -> {
                            // If decoded width > height and rotationDegrees says 90/270, decoder probably DID NOT rotate -> rotate manually.
                            decoded.width > decoded.height
                        }

                        180 -> {
                            // For 180, dimension stays same — rely on EXIF value here (rotate if rotationDegrees==180)
                            true
                        }

                        else -> false
                    }
                }

                else -> {
                    // older devices: ImageDecoder not available -> we must rotate according to EXIF
                    rotationDegrees != 0
                }
            }


            if (!needsManualRotate) {
                // decoded is fine as-is
                return decoded
            }

            // perform rotation
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated =
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            // recycle original if distinct to free memory
            if (rotated !== decoded) decoded.recycle()
            return rotated

        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

fun Mat.toBitmapSafe(): Bitmap {
    val tmp = Mat()
    when (this.channels()) {
        1 -> Imgproc.cvtColor(this, tmp, Imgproc.COLOR_GRAY2RGBA)
        3 -> Imgproc.cvtColor(this, tmp, Imgproc.COLOR_BGR2RGBA)
        4 -> this.copyTo(tmp)
        else -> this.copyTo(tmp)
    }
    val bmp = createBitmap(tmp.cols(), tmp.rows())
    Utils.matToBitmap(tmp, bmp)
    tmp.release()
    return bmp
}

fun Mat.toColoredWarped(): Mat {
    val coloredWarped = Mat()
    Imgproc.cvtColor(this, coloredWarped, Imgproc.COLOR_GRAY2BGR)
    return coloredWarped
}

/**
 * Pre-clean a grayscale Mat:
 * 1) optional CLAHE to flatten lighting
 * 2) light denoise (Gaussian or Bilateral)
 */
fun Mat.preCleanGray(
    useClahe: Boolean = true,
    useBilateral: Boolean = false
): Mat {
    require(this.type() == CvType.CV_8UC1) {
        "preCleanGray expects single-channel 8-bit grayscale."
    }

    val work = this.clone()

    // 1) Contrast-Limited Adaptive Histogram Equalization (good for shadows/uneven light)
    if (useClahe) {
        val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(work, work)
    }

    // 2) Denoise
    if (useBilateral) {
        // Bilateral keeps edges sharper (slower). Good when circles are thin.
        // d: neighborhood diameter, sigmaColor/sigmaSpace tune smoothness.
        Imgproc.bilateralFilter(
            work,
            work, /*d*/
            9, /*sigmaColor*/
            50.0, /*sigmaSpace*/
            50.0
        )
    } else {
        // Gaussian is fast + stable
        Imgproc.GaussianBlur(work, work, Size(5.0, 5.0), 0.0)
    }

    return work
}