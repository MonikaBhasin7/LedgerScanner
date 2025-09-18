package com.example.ledgerscanner.feature.scanner.exam.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericEmptyState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey50
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.ExamListViewModel
import com.example.ledgerscanner.feature.scanner.scan.ui.ScanOmrWithCamera
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExamListingActivity : ComponentActivity() {
    private val examListViewModel: ExamListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    topBar = {
                        GenericToolbar(title = "Exams")
                    },
                    floatingActionButton = {
                        val context = LocalContext.current
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth(),
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        context,
                                        ScanOmrWithCamera::class.java
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Exam",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    content = { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            var examFilter by remember { mutableStateOf<ExamStatus?>(null) }
                            val examListResponse by examListViewModel.examList.collectAsState()

                            SearchBar()

                            // Disable chips while loading
                            val isLoading = examListResponse is UiState.Loading
                            FilterChips(disableClicking = isLoading) { selectedFilter ->
                                examFilter = selectedFilter
                            }

                            ExamList(examFilter, examListResponse)
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun ExamList(examFilter: ExamStatus?, examListResponse: UiState<List<ExamEntity>>) {
        // load whenever filter changes
        LaunchedEffect(examFilter) {
            examListViewModel.getExamList(examFilter)
        }

        val state = examListResponse

        when (state) {
            is UiState.Loading -> {
                GenericLoader()
            }

            is UiState.Error -> {
                ErrorScreen(
                    message = (state as UiState.Error).message,
                    onRetry = { examListViewModel.getExamList(examFilter) }
                )
            }

            is UiState.Success<*> -> {
                val items = (state as UiState.Success<List<ExamEntity>>).data ?: emptyList()

                if (items.isEmpty()) {
                    GenericEmptyState(text = "No exams found")
                    return
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        ExamCardRow(item = item) {
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ExamCardRow(item: ExamEntity, onClick: (() -> Unit)? = null) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(width = 1.dp, color = Grey200, shape = RoundedCornerShape(12.dp))
                .background(color = Grey100, shape = RoundedCornerShape(12.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(13.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Blue100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = "Exam Icon",
                        tint = Blue500,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title ?: "",
                            color = Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        StatusBadge(status = item.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${item.totalQuestions} questions \u2022 Created ${item.createdDate} \u2022 Sheets: ${item.sheetsCount}",
                        color = Grey500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Avg ${item.avgScorePercent}% \u2022 Top ${item.topScorePercent}% \u2022 Median ${item.medianScorePercent}%",
                        color = Grey500
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusBadge(status: ExamStatus?) {
        if (status == null) {
            return Spacer(modifier = Modifier.width(0.dp))
        }
//        val (bg, textColor) = when (status) {
//            ExamStatus.Processing -> Pair(Grey100, Blue500)
//            ExamStatus.Completed -> Pair(Grey100, /* green text - define in Color.kt if needed */ Blue500)
//            ExamStatus.Draft -> Pair(Grey100, Grey500)
//        }

        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Grey200)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = status.name, color = Grey500)
        }
    }

    @Composable
    private fun FilterChips(disableClicking: Boolean, onSelect: (ExamStatus?) -> Unit) {
        val filters: MutableList<ExamStatus?> = ExamStatus.entries.toMutableList()
        filters.add(0, null) // null represents "All"
        var selectedIndex by remember { mutableIntStateOf(0) }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 12.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            for ((i, filter) in filters.withIndex()) {
                FilterChip(
                    selected = (selectedIndex == i),
                    onClick = {
                        if (!disableClicking && selectedIndex != i) {
                            selectedIndex = i
                            onSelect(filters[selectedIndex])
                        }
                    },
                    label = { Text(filter?.name ?: "All") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Blue100,
                        selectedContainerColor = Blue500,
                        labelColor = Blue500,
                        selectedLabelColor = White,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }

    @Composable
    private fun SearchBar() {
        var text by remember { mutableStateOf(TextFieldValue("")) }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Search by exam name") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = "Search Icon")
            },
            trailingIcon = {
                if (text.text.isNotEmpty())
                    Icon(Icons.Filled.ArrowForward, contentDescription = null)
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Grey50,
                focusedContainerColor = White,
                focusedLeadingIconColor = Grey500,
                unfocusedLeadingIconColor = Grey500,
                focusedIndicatorColor = Grey200,
                unfocusedIndicatorColor = Grey200,
                focusedTrailingIconColor = Grey500,
                unfocusedTrailingIconColor = Grey500,
                focusedPlaceholderColor = Grey500,
                unfocusedPlaceholderColor = Grey500
            )
        )
    }

    @Composable
    private fun ErrorScreen(message: String?, onRetry: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = message ?: "Something went wrong")
            Spacer(modifier = Modifier.height(12.dp))
            // simple retry text button
            Text(
                text = "Retry",
                modifier = Modifier.clickable { onRetry() }
            )
        }
    }
}