package com.example.ledgerscanner.feature.scanner.scan.ui.dialog

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ledgerscanner.base.ui.theme.Black
import kotlinx.coroutines.launch

@Composable
fun WarpedImageDialog(
    warpedBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {},
    onSave: () -> Unit = {},
    intermediateBitmaps: Map<String, Bitmap>?
) {
    if (warpedBitmap == null && intermediateBitmaps.isNullOrEmpty()) {
        // If no image, show a simple error dialog
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Preview") },
            text = { Text("No image available") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header row (title + close)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned Preview",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Image area: pinch to zoom & pan
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .heightIn(min = 250.dp, max = 600.dp)
//                        .background(Black)
//                        .padding(8.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Image(
//                        bitmap = warpedBitmap.asImageBitmap(),
//                        contentDescription = "Warped preview",
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .heightIn(min = 250.dp, max = 600.dp)
//                            .clip(RoundedCornerShape(8.dp)),
//                        contentScale = ContentScale.Fit
//                    )
//                }

                ImageCarousel(
                    warpedBitmap = warpedBitmap,
                    intermediate = intermediateBitmaps ?: mapOf(),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // optional: show small helpers/metadata
                Spacer(modifier = Modifier.height(8.dp))

                // Buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rescan")
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onSave) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageCarousel(
    warpedBitmap: Bitmap?,                       // first/primary image (may be null)
    intermediate: Map<String, Bitmap?> = emptyMap(), // other bitmaps keyed by name
    modifier: Modifier = Modifier,
    itemHeightMin: Int = 250, // dp
    itemHeightMax: Int = 600  // dp
) {
    var items by remember { mutableStateOf(listOf<Pair<String, Bitmap>>()) }

    LaunchedEffect(warpedBitmap, intermediate) {
        val list = mutableListOf<Pair<String, Bitmap>>() // pair: key / bitmap
        warpedBitmap?.let { list.add("warped" to it) }
        intermediate.forEach { (k, v) ->
            if (v != null) list.add(k to v)
        }
        items = list
    }

    var selectedIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // full screen preview dialog
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewTitle by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.animateContentSize()) {
        if (items.isEmpty()) {
            // placeholder when no images
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = itemHeightMin.dp, max = itemHeightMax.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Text("No preview available", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            // Horizontal carousel
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = itemHeightMin.dp, max = itemHeightMax.dp)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                itemsIndexed(items) { index, pair ->
                    val (key, bmp) = pair
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                // open full screen preview
                                previewBitmap = bmp
                                previewTitle = if (key == "warped") "Warped" else key
                            }
                    ) {
                        Column {
                            Text("key - ${key}")
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = key,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .width(((300.dp).coerceIn(200.dp, 600.dp)))
                                    .heightIn(min = itemHeightMin.dp, max = itemHeightMax.dp)
                            )
                        }
                    }
                }
            }

            // Dot indicator + optional quick nav (tap dot to scroll)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                for (i in items.indices) {
                    val isSelected = i == selectedIndex
                    val dotSize = if (isSelected) 10.dp else 8.dp
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray

                    // clickable dot to scroll the LazyRow to that index
                    CircleShape
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(i)
                                    selectedIndex = i
                                }
                            }
                    )
                }
            }

            // keep selectedIndex synced with scroll position
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                selectedIndex = listState.firstVisibleItemIndex
            }
        }
    }

    // Full screen preview dialog
    if (previewBitmap != null) {
        Dialog(onDismissRequest = { previewBitmap = null; previewTitle = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 900.dp)
                    .background(Color(0xFF222222))
            ) {
                // Simple close button top-right (optional styling)
                IconButton(onClick = { previewBitmap = null; previewTitle = null }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                // Image centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = previewTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 900.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

/**
 * Utility that returns a soft width suggestion (in dp) for carousel items based on screen width.
 * Kept private & simple — adjust logic if you want different behavior on tablets / phones.
 */
fun localCarouselWidthFraction(): Float {
    // you can base this on LocalConfiguration if needed. For simplicity return 300dp width as float
    return 300f
}
