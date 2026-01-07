package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ledgerscanner.BuildConfig
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.utils.image.ImageUtils

@Composable
fun OmrSheetPreview(
    clickedImagePath: String?,
    finalImagePath: String?,
    debugImagePaths: Map<String, String>?
) {
    val context = LocalContext.current
    var showZoomedImage by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }

    // ✅ Load bitmaps from paths
    val pages: List<Pair<String, Bitmap?>> =
        remember(clickedImagePath, finalImagePath, debugImagePaths) {
            buildList {
                // Add clicked image if available
                clickedImagePath?.let { path ->
                    val bitmap = ImageUtils.loadBitmapFromPath(context, path)
                    bitmap?.let { add("Captured Sheet" to it) }
                }

                // Add final processed image if available
                finalImagePath?.let { path ->
                    val bitmap = ImageUtils.loadBitmapFromPath(context, path)
                    bitmap?.let { add("Scanned Result" to it) }
                }

                // Add debug images
                if (BuildConfig.ENABLE_IMAGE_LOGS == true)
                    debugImagePaths?.forEach { (label, path) ->
                        val bitmap = ImageUtils.loadBitmapFromPath(context, path)
                        bitmap?.let { add(label to it) }
                    }
            }
        }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )

    if (pages.isEmpty()) {
        // Show placeholder if no images available
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No images available",
                style = AppTypography.body3Regular,
                color = Grey600
            )
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = Grey200),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .width(250.dp)
                        .height(300.dp)
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        selectedImageIndex = page
                                        showZoomedImage = true
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        pages[page].second?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = pages[page].first,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: run {
                            Text(
                                text = "Failed to load image",
                                style = AppTypography.body3Regular,
                                color = Grey600
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ✅ Show current page label
                Text(
                    text = pages[pagerState.currentPage].first,
                    style = AppTypography.body3Medium,
                    color = Grey900
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${pagerState.currentPage + 1} / ${pages.size}",
                    style = AppTypography.body3Regular,
                    color = Grey600
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Swipe to view • Tap to zoom",
                    style = AppTypography.body3Regular,
                    color = Grey600
                )
            }
        }
    }

    // ✅ Zoomed image dialog
    if (showZoomedImage) {
        ZoomedImageDialog(
            pages = pages,
            initialPage = selectedImageIndex,
            onDismiss = { showZoomedImage = false }
        )
    }
}

@Composable
private fun ZoomedImageDialog(
    pages: List<Pair<String, Bitmap?>>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Zoomable pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                pages[page].second?.let { bitmap ->
                    ZoomableImage(
                        bitmap = bitmap,
                        contentDescription = pages[page].first
                    )
                }
            }

            // Image info overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = pages[pagerState.currentPage].first,
                        style = AppTypography.body2Medium,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${pagerState.currentPage + 1} / ${pages.size}",
                        style = AppTypography.body3Regular,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    bitmap: Bitmap,
    contentDescription: String?
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        val maxX = (bitmap.width * (scale - 1)) / 2
        val maxY = (bitmap.height * (scale - 1)) / 2

        offset = Offset(
            x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
            y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
        )
    }

    // Reset zoom on double-tap
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2f
                        offset = Offset.Zero
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state)
        )
    }
}