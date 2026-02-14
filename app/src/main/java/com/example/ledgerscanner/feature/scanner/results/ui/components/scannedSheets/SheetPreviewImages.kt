package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.base.ui.theme.*
import java.io.File

@Composable
fun SheetPreviewImages(imagePath: String?, thumbnailPath: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetPreviewImage(thumbnailPath ?: imagePath)
        SheetPreviewImage(imagePath ?: thumbnailPath)
    }
}

@Composable
fun SheetPreviewImage(imagePath: String?) {
    Box(
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Grey200)
    ) {
        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(file).crossfade(true).build(),
                    contentDescription = "Sheet preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                SheetPlaceholder()
            }
        } else {
            SheetPlaceholder()
        }
    }
}

@Composable
fun SheetPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Sheet placeholder",
            tint = Grey400,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun NewBadge() {
    Box(
        modifier = Modifier
            .background(Green500, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = "New", color = Color.White, style = AppTypography.text11Bold)
    }
}
