package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.utils.ScanResultUtils
import java.io.File

@Composable
fun ScannedSheetGridItem(
    sheet: ScanResultEntity,
    modifier: Modifier = Modifier,
    onViewDetails: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onViewDetails() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(Grey200)
            ) {
                if (sheet.scannedImagePath != null) {
                    val file = File(sheet.scannedImagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(file).crossfade(true).build(),
                            contentDescription = "Sheet preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                if (ScanResultUtils.isRecentSheet(sheet.scannedAt)) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        NewBadge()
                    }
                }
            }

            Text(text = "Sheet #${sheet.id}", style = AppTypography.text16Bold, color = Grey900)
            Text(text = sheet.barCode ?: "Unknown", style = AppTypography.text13Regular, color = Grey600, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "${sheet.scorePercent.toInt()}%", style = AppTypography.text24Bold, color = Blue700)

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactScoreIndicator("✓", sheet.correctCount, Green600)
                CompactScoreIndicator("✕", sheet.wrongCount, Red600)
                CompactScoreIndicator("—", sheet.blankCount, Grey600)
            }
        }
    }
}

@Composable
private fun CompactScoreIndicator(icon: String, count: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, color = color, style = AppTypography.text14Bold, fontSize = 14.sp)
        Text(text = count.toString(), color = color, style = AppTypography.text13SemiBold)
    }
}

@Composable
fun ScannedSheetGridRow(rowSheets: List<ScanResultEntity>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rowSheets.forEach { sheet ->
            ScannedSheetGridItem(sheet = sheet, modifier = Modifier.weight(1f))
        }
        if (rowSheets.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}