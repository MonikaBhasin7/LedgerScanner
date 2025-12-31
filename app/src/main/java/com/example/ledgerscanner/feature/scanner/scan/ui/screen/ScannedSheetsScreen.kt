package com.example.ledgerscanner.feature.scanner.scan.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.R
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue50
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue600
import com.example.ledgerscanner.base.ui.theme.Blue700
import com.example.ledgerscanner.base.ui.theme.Blue75
import com.example.ledgerscanner.base.ui.theme.Green50
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Green700
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Orange700
import com.example.ledgerscanner.base.ui.theme.Red50
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.base.ui.theme.Red700
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.scan.model.ScannedSheetViewMode
import com.example.ledgerscanner.feature.scanner.scan.model.SheetFilter
import com.example.ledgerscanner.feature.scanner.scan.model.SheetSort
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.ScannedSheetsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScannedSheetsScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    scannedSheetsViewModel: ScannedSheetsViewModel
) {
    val scannedSheets by scannedSheetsViewModel.scannedSheets.collectAsState()
    val examStats by scannedSheetsViewModel.examStatsCache.collectAsState()
    val selectedFilter by scannedSheetsViewModel.selectedFilter.collectAsState()
    val selectedSort by scannedSheetsViewModel.selectedSort.collectAsState()
    val viewMode by scannedSheetsViewModel.viewMode.collectAsState()
    val handleBack = rememberBackHandler(navController)

    LaunchedEffect(Unit) {
        scannedSheetsViewModel.getScannedSheetsByExamId(examEntity.id)
        scannedSheetsViewModel.loadStatsForExam(examEntity.id)
    }

    Scaffold(topBar = {
        GenericToolbar(title = "Scanned Sheets", onBackClick = handleBack)
    }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (scannedSheets) {
                is UiState.Error -> {
                    ErrorState(message = (scannedSheets as UiState.Error).message)
                }

                is UiState.Idle, is UiState.Loading -> {
                    LoadingState()
                }

                is UiState.Success -> {
                    val dataHolder = (scannedSheets as UiState.Success).data
                    val filteredSheets = dataHolder?.filterList ?: listOf()
                    val hasAnySheets = dataHolder?.originalList?.isNotEmpty()

                    Column {
                        // Only show stats and filters if there are sheets in database
                        if (hasAnySheets == true) {
                            ExamStatsHeader(examEntity, examStats[examEntity.id])

                            FilterAndSortControls(
                                selectedFilter = selectedFilter,
                                selectedSort = selectedSort,
                                viewMode = viewMode,
                                sheets = dataHolder.originalList,
                                onFilterChange = { scannedSheetsViewModel.setFilter(it) },
                                onSortChange = { scannedSheetsViewModel.setSort(it) },
                                onViewModeChange = { scannedSheetsViewModel.setViewMode(it) }
                            )
                        }

                        // Content area
                        when {
                            hasAnySheets == false -> {
                                // True empty state - no sheets at all
                                EmptyState()
                            }
                            filteredSheets.isEmpty() -> {
                                // Filtered empty state
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "🔍",
                                            fontSize = 48.sp
                                        )
                                        Text(
                                            text = when (selectedFilter) {
                                                SheetFilter.HIGH_SCORE -> "No high score sheets"
                                                SheetFilter.LOW_SCORE -> "No low score sheets"
                                                else -> "No sheets found"
                                            },
                                            style = AppTypography.body2Medium,
                                            color = Grey600,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Try changing the filter",
                                            style = AppTypography.body3Regular,
                                            color = Grey500,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            else -> {
                                // Update list rendering
                                when (viewMode) {
                                    ScannedSheetViewMode.LIST -> ScannedSheetsList(sheets = filteredSheets)
                                    ScannedSheetViewMode.GRID -> ScannedSheetsGrid(sheets = filteredSheets)
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun FilterAndSortControls(
    selectedFilter: SheetFilter,
    selectedSort: SheetSort,
    viewMode: ScannedSheetViewMode,
    sheets: List<ScanResultEntity>,
    onFilterChange: (SheetFilter) -> Unit,
    onSortChange: (SheetSort) -> Unit,
    onViewModeChange: (ScannedSheetViewMode) -> Unit
) {
    val totalCount = sheets.size
    val highScoreCount = sheets.count { it.scorePercent >= 75 }
    val lowScoreCount = sheets.count { it.scorePercent < 40 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Row: Select All + Sort + Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select All",
                style = AppTypography.text15SemiBold,
                color = Blue600,
                modifier = Modifier.clickable {
                    // TODO: Implement select all functionality
                }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Dropdown
                SortDropdown(
                    selectedSort = selectedSort,
                    onSortChange = onSortChange
                )

                // Grid View Icon
                IconButton(
                    onClick = { onViewModeChange(ScannedSheetViewMode.GRID) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid view",
                        tint = if (viewMode == ScannedSheetViewMode.GRID) Blue600 else Grey600,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // List View Icon
                IconButton(
                    onClick = { onViewModeChange(ScannedSheetViewMode.LIST) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "List view",
                        tint = if (viewMode == ScannedSheetViewMode.LIST) Blue600 else Grey600,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                label = "All ($totalCount)",
                isSelected = selectedFilter == SheetFilter.ALL,
                onClick = { onFilterChange(SheetFilter.ALL) },
                selectedColor = Grey900,
                unselectedColor = Grey600
            )

            FilterChip(
                label = "High Score ($highScoreCount)",
                isSelected = selectedFilter == SheetFilter.HIGH_SCORE,
                onClick = { onFilterChange(SheetFilter.HIGH_SCORE) },
                selectedColor = Green600,
                unselectedColor = Green600
            )

            FilterChip(
                label = "Low Score ($lowScoreCount)",
                isSelected = selectedFilter == SheetFilter.LOW_SCORE,
                onClick = { onFilterChange(SheetFilter.LOW_SCORE) },
                selectedColor = Red600,
                unselectedColor = Red600
            )
        }
    }
}

@Composable
private fun ScannedSheetsGrid(sheets: List<ScanResultEntity>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sheets) { sheet ->
            ScannedSheetGridItem(sheet = sheet)
        }
    }
}

@Composable
private fun ScannedSheetGridItem(
    sheet: ScanResultEntity,
    onViewDetails: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sheet Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Grey200)
            ) {
                if (sheet.scannedImagePath != null) {
                    val file = File(sheet.scannedImagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Sheet preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // New Badge overlay
                if (isRecentSheet(sheet.scannedAt)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        NewBadge()
                    }
                }
            }

            // Sheet ID
            Text(
                text = "Sheet #${sheet.id}",
                style = AppTypography.text16Bold,
                color = Grey900
            )

            // Student
            Text(
                text = sheet.barCode ?: "Unknown",
                style = AppTypography.text13Regular,
                color = Grey600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Score
            Text(
                text = "${sheet.scorePercent.toInt()}%",
                style = AppTypography.text24Bold,
                color = Blue700
            )

            // Score indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactScoreIndicator(
                    icon = "✓",
                    count = sheet.correctCount,
                    color = Green600
                )

                CompactScoreIndicator(
                    icon = "✕",
                    count = sheet.wrongCount,
                    color = Red600
                )

                CompactScoreIndicator(
                    icon = "—",
                    count = sheet.blankCount,
                    color = Grey600
                )
            }
        }
    }
}

@Composable
private fun CompactScoreIndicator(
    icon: String,
    count: Int,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            color = color,
            style = AppTypography.text14Bold,
            fontSize = 14.sp
        )
        Text(
            text = count.toString(),
            color = color,
            style = AppTypography.text13SemiBold
        )
    }
}
@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) selectedColor else Color.White
            )
            .border(
                width = 1.5.dp,
                color = unselectedColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = AppTypography.text14SemiBold,
            color = if (isSelected) Color.White else unselectedColor
        )
    }
}

@Composable
private fun SortDropdown(
    selectedSort: SheetSort,
    onSortChange: (SheetSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by: ${selectedSort.displayName}",
                style = AppTypography.text14Regular,
                color = Grey600
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Sort",
                tint = Grey600,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SheetSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sort.displayName,
                            style = AppTypography.text14Regular
                        )
                    },
                    onClick = {
                        onSortChange(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExamStatsHeader(
    examEntity: ExamEntity,
    stats: ExamStatistics?
) {
    val safeStats = stats ?: ExamStatistics()
    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Blue75,
                        White
                    )
                )
            )
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Exam Title and Info
            Text(
                text = examEntity.examName,
                style = AppTypography.title2ExtraBold,
                color = Grey900
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${examEntity.totalQuestions} Questions",
                style = AppTypography.body3Regular,
                color = Grey600
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${safeStats.sheetsCount}",
                    label = "Total\nsheets",
                    color = Blue500
                )

                StatItem(
                    value = safeStats.getValidAvgScore(),
                    label = "Average\nscore",
                    color = Blue500
                )

                StatItem(
                    value = safeStats.getValidTopScore(),
                    label = "Top\nscore",
                    color = Blue500
                )

                StatItem(
                    value = safeStats.getValidLowestScore(),
                    label = "Lowest\nscore",
                    color = Blue500
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = value,
            style = AppTypography.text36Bold,
            color = color
        )

        Text(
            text = label,
            style = AppTypography.text13Regular,
            color = Grey600,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScannedSheetsList(sheets: List<ScanResultEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sheets) { sheet ->
            ScannedSheetCard(sheet = sheet)
        }
    }
}

@Composable
private fun ScannedSheetCard(
    sheet: ScanResultEntity,
    onViewDetails: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Sheet Preview Images
                SheetPreviewImages(
                    imagePath = sheet.scannedImagePath,
                    thumbnailPath = sheet.thumbnailPath
                )

                // Middle: Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title Row with Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sheet #${sheet.id}",
                            style = AppTypography.text20Bold,
                            color = Grey900
                        )

                        if (isRecentSheet(sheet.scannedAt)) {
                            NewBadge()
                        }
                    }

                    // Student Barcode
                    Text(
                        text = "Student: ${sheet.barCode ?: "Unknown"}",
                        style = AppTypography.text15Regular,
                        color = Grey700
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Score - Large and prominent
                    Text(
                        text = "${sheet.score}/${sheet.totalQuestions} (${sheet.scorePercent.toInt()}%)",
                        style = AppTypography.text28Bold,
                        color = Blue700
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Correct, Wrong, Skipped
                    // Correct, Wrong, Skipped, Multiple
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreIndicator(
                            icon = "✓",
                            count = sheet.correctCount,
                            color = Green600
                        )

                        ScoreIndicator(
                            icon = "✕",
                            count = sheet.wrongCount,
                            color = Red600
                        )

                        ScoreIndicator(
                            icon = "—",
                            count = sheet.blankCount,
                            color = Grey600
                        )

                        // Multiple marks indicator
                        if (!sheet.multipleMarksDetected.isNullOrEmpty()) {
                            ScoreIndicator(
                                icon = "⚠",
                                count = sheet.multipleMarksDetected.size,
                                color = Orange600
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Row: Time + View Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scanned: ${formatTimeAgo(sheet.scannedAt)}",
                    style = AppTypography.text13Regular,
                    color = Grey500
                )

                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    Text(
                        text = "View Details",
                        color = Blue600,
                        style = AppTypography.text15SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreIndicator(
    icon: String,
    count: Int,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            color = color,
            style = AppTypography.label1Bold, // 18sp
            fontSize = 18.sp
        )
        Text(
            text = count.toString(),
            color = color,
            style = AppTypography.body2SemiBold // 16sp
        )
    }
}

@Composable
private fun NewBadge() {
    Box(
        modifier = Modifier
            .background(Green500, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "New",
            color = Color.White,
            style = AppTypography.text11Bold
        )
    }
}

@Composable
private fun SheetPreviewImages(
    imagePath: String?,
    thumbnailPath: String?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First sheet preview (use thumbnail if available, otherwise main image)
        SheetPreviewImage(
            imagePath = thumbnailPath ?: imagePath
        )

        // Second sheet preview (optional - for multi-page sheets)
        // For now, we'll show the same image or a placeholder
        SheetPreviewImage(
            imagePath = thumbnailPath ?: imagePath
        )
    }
}

@Composable
private fun SheetPreviewImage(
    imagePath: String?
) {
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(file)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Sheet preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // File doesn't exist - show placeholder
                SheetPlaceholder()
            }
        } else {
            // No image path - show placeholder
            SheetPlaceholder()
        }
    }
}

@Composable
private fun SheetPlaceholder() {
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

// Helper functions
private fun isRecentSheet(scannedAt: Long): Boolean {
    val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
    return scannedAt > fiveMinutesAgo
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000} mins ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Blue600)
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📋",
                fontSize = 64.sp
            )
            Text(
                text = "No sheets scanned yet",
                style = AppTypography.body2Medium,
                color = Grey600
            )
        }
    }
}

@Composable
private fun ErrorState(message: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp
            )
            Text(
                text = message ?: "Something went wrong",
                style = AppTypography.body2Medium,
                color = Grey600,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Helper functions
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getScoreBackgroundColor(score: Float): Color {
    return when {
        score >= 75 -> Green50
        score >= 50 -> Blue50
        score >= 40 -> Orange50
        else -> Red50
    }
}

private fun getScoreTextColor(score: Float): Color {
    return when {
        score >= 75 -> Green700
        score >= 50 -> Blue700
        score >= 40 -> Orange700
        else -> Red700
    }
}