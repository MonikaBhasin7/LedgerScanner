package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue600
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.model.ScannedSheetViewMode
import com.example.ledgerscanner.feature.scanner.results.model.SheetFilter
import com.example.ledgerscanner.feature.scanner.results.model.SheetSort

@Composable
fun FilterAndSortControls(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select All",
                style = AppTypography.text15SemiBold,
                color = Blue600,
                modifier = Modifier.clickable { /* TODO */ }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortDropdown(selectedSort, onSortChange)
                ViewModeToggle(viewMode, onViewModeChange)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                "All ($totalCount)",
                selectedFilter == SheetFilter.ALL,
                { onFilterChange(SheetFilter.ALL) },
                Grey900,
                Grey600
            )
            FilterChip(
                "High Score ($highScoreCount)",
                selectedFilter == SheetFilter.HIGH_SCORE,
                { onFilterChange(SheetFilter.HIGH_SCORE) },
                Green600,
                Green600
            )
            FilterChip(
                "Low Score ($lowScoreCount)",
                selectedFilter == SheetFilter.LOW_SCORE,
                { onFilterChange(SheetFilter.LOW_SCORE) },
                Red600,
                Red600
            )
        }
    }
}

@Composable
private fun ViewModeToggle(
    viewMode: ScannedSheetViewMode,
    onViewModeChange: (ScannedSheetViewMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = Icons.Default.GridView,
            contentDescription = "Grid view",
            tint = if (viewMode == ScannedSheetViewMode.GRID) Blue600 else Grey600,
            modifier = Modifier
                .size(24.dp)
                .clickable { onViewModeChange(ScannedSheetViewMode.GRID) }
        )
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = "List view",
            tint = if (viewMode == ScannedSheetViewMode.LIST) Blue600 else Grey600,
            modifier = Modifier
                .size(24.dp)
                .clickable { onViewModeChange(ScannedSheetViewMode.LIST) }
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
            .background(if (isSelected) selectedColor else Color.White)
            .border(1.5.dp, unselectedColor, RoundedCornerShape(24.dp))
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
                    text = { Text(text = sort.displayName, style = AppTypography.text14Regular) },
                    onClick = {
                        onSortChange(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}