package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus

@Composable
fun FilterChips(
        disableClicking: Boolean,
        onSelect: (ExamStatus?) -> Unit
    ) {
        val filters = buildList {
            add(null) // "All" option
            addAll(ExamStatus.entries)
        }
        var selectedIndex by remember { mutableIntStateOf(0) }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .padding(bottom = 8.dp)
                .fillMaxWidth()
        ) {
            filters.forEachIndexed { index, filter ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = {
                        if (!disableClicking && selectedIndex != index) {
                            selectedIndex = index
                            onSelect(filter)
                        }
                    },
                    label = {
                        Text(
                            text = filter?.name ?: "All",
                            style = AppTypography.text11SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Blue100,
                        selectedContainerColor = Blue500,
                        labelColor = Blue500,
                        selectedLabelColor = White,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = null,
                    modifier = Modifier
                        .height(28.dp)
                        .padding(end = 6.dp),
                )
            }
        }
    }

@Composable
fun SearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {

        GenericTextField(
            value = searchQuery,
            placeholder = "Search by exam name",
            onValueChange = { onSearchQueryChange(it) },
            textStyle = AppTypography.text14Medium,
            placeholderStyle = AppTypography.text13Regular,
            prefix = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search Icon",
                    tint = Grey500,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

@Composable
fun ErrorScreen(
        message: String?,
        onRetry: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = message ?: "Something went wrong")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Retry",
                modifier = Modifier.clickable { onRetry() },
                style = AppTypography.body2Medium,
                color = Blue500
            )
        }
    }
