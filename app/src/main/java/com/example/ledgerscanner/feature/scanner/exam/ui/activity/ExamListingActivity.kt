package com.example.ledgerscanner.feature.scanner.exam.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericEmptyState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.ToolbarAction
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.model.ExamActionDialog
import com.example.ledgerscanner.feature.scanner.exam.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.model.QuickActionButton
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.ExamActionConfirmationDialog
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.ExamActionsPopup
import com.example.ledgerscanner.feature.scanner.exam.ui.dialog.TemplatePickerDialog
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.ExamListViewModel
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.CreateTemplateActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ExamListingActivity : ComponentActivity() {

    private val examListViewModel: ExamListViewModel by viewModels()
    private val scanResultViewModel: ScanResultViewModel by viewModels()

    companion object {
        private const val EXTRA_TEMPLATE = "template"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                ExamListingScreen(examListViewModel)
            }
        }
    }

    @Composable
    private fun ExamListingScreen(viewModel: ExamListViewModel) {
        val context = LocalContext.current
        var showTemplatePicker by remember { mutableStateOf(false) }
        var examFilter by remember { mutableStateOf<ExamStatus?>(null) }
        val examListResponse by viewModel.examList.collectAsState()
        var searchQuery by remember { mutableStateOf("") }

        var showDeleteAndDuplicateDialog by remember { mutableStateOf<ExamActionDialog?>(null) }
        val deleteState by viewModel.deleteExamState.collectAsState()
        val duplicateState by viewModel.duplicateExamState.collectAsState()
        val examStatistics by scanResultViewModel.examStatsCache.collectAsState()


        // Handle filter changes - starts collecting from DB
        LaunchedEffect(examFilter) {
            examListViewModel.getExamList(examFilter)
        }

        // Handle search query changes
        LaunchedEffect(searchQuery) {
            examListViewModel.searchExam(searchQuery)
        }

        // Handle delete state
        LaunchedEffect(deleteState) {
            when (val state = deleteState) {
                is UiState.Success -> {
                    Toast.makeText(context, "Exam deleted successfully", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteState()
                }

                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteState()
                }

                else -> {}
            }
        }

        // Handle duplicate state
        LaunchedEffect(duplicateState) {
            when (val state = duplicateState) {
                is UiState.Success -> {
                    Toast.makeText(context, "Exam duplicated successfully", Toast.LENGTH_SHORT)
                        .show()
                    viewModel.resetDuplicateState()
                }

                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetDuplicateState()
                }

                else -> {}
            }
        }

        Scaffold(
            containerColor = White,
            topBar = {
                GenericToolbar(
                    title = "Exams", actions = listOf(
                        ToolbarAction.IconText(
                            icon = Icons.Default.Addchart,
                            text = "Create Exam",
                            onClick = {
                                startActivity(Intent(context, CreateExamActivity::class.java))
                            }
                        ),
                        ToolbarAction.IconText(
                            icon = Icons.Default.Addchart,
                            text = "Create Template",
                            onClick = {
                                startActivity(Intent(context, CreateTemplateActivity::class.java))
                            }
                        )
                    )
                )
            },
//            bottomBar = {
//                BottomActionBar(
//                    onCreateExam = {
//                        startActivity(Intent(context, CreateExamActivity::class.java))
//                    },
//                    onCreateTemplate = {
//                        startActivity(Intent(context, CreateTemplateActivity::class.java))
//                    },
//                    onSelectExam = {
//                        showTemplatePicker = true
//                    }
//                )
//            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                SearchBar(searchQuery, onSearchQueryChange = {
                    searchQuery = it
                })

                val isLoading = examListResponse is UiState.Loading
                FilterChips(
                    disableClicking = isLoading,
                    onSelect = { selectedFilter ->
                        examFilter = selectedFilter
                    }
                )

                ExamList(
                    examListResponse = examListResponse,
                    examStatistics = examStatistics,
                    onExamClick = { examEntity, examAction ->
                        handleExamAction(
                            context,
                            examEntity,
                            examAction,
                            showDeleteDialog = {
                                showDeleteAndDuplicateDialog = ExamActionDialog.Delete(it)
                            },
                            showDuplicateDialog = {
                                showDeleteAndDuplicateDialog = ExamActionDialog.Duplicate(it)
                            }
                        )
                    },
                    onLoadStats = {
                        scanResultViewModel.loadStatsForExam(it)
                    },
                    onRetry = {
                        examListViewModel.getExamList(examFilter)
                    },
                    onActionClick = { examEntity, examAction ->
                        handleExamAction(
                            context,
                            examEntity,
                            examAction,
                            showDeleteDialog = {
                                showDeleteAndDuplicateDialog = ExamActionDialog.Delete(it)
                            },
                            showDuplicateDialog = {
                                showDeleteAndDuplicateDialog = ExamActionDialog.Duplicate(it)
                            }
                        )
                    }
                )
            }
        }

        if (showTemplatePicker) {
            TemplatePickerDialog(
                onDismiss = { showTemplatePicker = false },
                onSelect = { assetFile ->
                    showTemplatePicker = false
                    handleTemplateSelection(assetFile)
                }
            )
        }

        showDeleteAndDuplicateDialog?.let {
            when (showDeleteAndDuplicateDialog) {
                is ExamActionDialog.Delete -> {
                    val examEntity = (it as ExamActionDialog.Delete).examEntity
                    ExamActionConfirmationDialog(
                        title = "Delete Exam",
                        message = "Are you sure you want to delete \"${examEntity.examName}\"?",
                        confirmText = "Delete",
                        onConfirm = {
                            examListViewModel.deleteExam(examEntity.id)
                            showDeleteAndDuplicateDialog = null
                        },
                        onDismiss = {
                            showDeleteAndDuplicateDialog = null
                        }
                    )
                }

                is ExamActionDialog.Duplicate -> {
                    val examEntity = (it as ExamActionDialog.Duplicate).examEntity
                    ExamActionConfirmationDialog(
                        title = "Duplicate Exam",
                        message = "Create a copy of \"${examEntity.examName}\"?",
                        confirmText = "Duplicate",
                        onConfirm = {
                            examListViewModel.duplicateExam(examEntity)
                            showDeleteAndDuplicateDialog = null
                        },
                        onDismiss = {
                            showDeleteAndDuplicateDialog = null
                        }
                    )
                }

                null -> {}
            }
        }
    }

    private fun handleExamAction(
        context: Context,
        examEntity: ExamEntity,
        examAction: ExamAction,
        showDeleteDialog: (ExamEntity) -> Unit,
        showDuplicateDialog: (ExamEntity) -> Unit
    ) {
        when (examAction) {
            ExamAction.ContinueSetup, ExamAction.EditExam -> {
                context.startActivity(
                    Intent(context, CreateExamActivity::class.java).apply {
                        putExtra(
                            CreateExamActivity.CONFIG, CreateExamConfig(
                                examEntity = examEntity,
                            )
                        )
                    }
                )
            }

            ExamAction.ScanSheets -> {
                startActivity(Intent(context, ScanBaseActivity::class.java).apply {
                    putExtra(ScanBaseActivity.ARG_EXAM_ENTITY, examEntity)
                })
            }

            ExamAction.ViewResults -> {

            }

            ExamAction.MarkCompleted -> {
                // TODO: Show confirmation and update status
                Toast.makeText(context, "Mark Completed - Coming soon", Toast.LENGTH_SHORT).show()
            }

            ExamAction.Duplicate -> {
                showDuplicateDialog(examEntity)
            }

            ExamAction.ExportResults -> {
                // TODO: Export results
                Toast.makeText(context, "Export Results - Coming soon", Toast.LENGTH_SHORT).show()
            }

            ExamAction.Archive -> {
                // TODO: Archive exam
                Toast.makeText(context, "Archive - Coming soon", Toast.LENGTH_SHORT).show()
            }

            ExamAction.Delete -> {
                showDeleteDialog(examEntity)
            }
        }
    }


    @Composable
    private fun ExamList(
        examListResponse: UiState<List<ExamEntity>>,
        examStatistics: Map<Int, ExamStatistics>,
        onExamClick: (ExamEntity, ExamAction) -> Unit,
        onRetry: () -> Unit,
        onActionClick: (ExamEntity, ExamAction) -> Unit,
        onLoadStats: (Int) -> Unit
    ) {
        when (examListResponse) {
            is UiState.Loading -> {
                GenericLoader()
            }

            is UiState.Error -> {
                ErrorScreen(
                    message = examListResponse.message,
                    onRetry = onRetry
                )
            }

            is UiState.Success -> {
                val items = examListResponse.data ?: emptyList()

                if (items.isEmpty()) {
                    GenericEmptyState(text = "No exams found")
                    return
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id }
                    ) { _, item ->
                        // Load stats when item becomes visible
                        LaunchedEffect(item.id) {
                            if (!examStatistics.containsKey(item.id)) {
                                onLoadStats(item.id)
                            }
                        }
                        ExamCardRow(
                            item = item,
                            examStatistics = examStatistics[item.id],
                            onClick = { onExamClick(item, it) },
                            onActionClick = {
                                onActionClick(item, it)
                            }
                        )
                    }
                }
            }

            is UiState.Idle -> {}
        }
    }

    @Composable
    private fun BottomActionBar(
        onCreateExam: () -> Unit,
        onCreateTemplate: () -> Unit,
        onSelectExam: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            GenericButton(
                text = "Create Exam",
                icon = Icons.Default.Addchart,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateExam
            )

            GenericButton(
                text = "Create Template",
                icon = Icons.Default.Addchart,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateTemplate
            )

            GenericButton(
                text = "Select Exam",
                icon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
                onClick = onSelectExam
            )
        }
    }

    @Composable
    private fun ExamCardRow(
        item: ExamEntity,
        examStatistics: ExamStatistics?,
        onClick: (ExamAction) -> Unit,
        onActionClick: (ExamAction) -> Unit,
    ) {
        val actions = examListViewModel.getExamActionForStatus(item.status)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = Grey200,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(Grey100)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .genericClick { actions.quickAction?.action?.let { onClick(it) } }
                ) {
                    ExamIcon()

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        ExamHeader(
                            examEntity = item,
                            actions = actions,
                            onActionClick = onActionClick
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val sheetCount = examStatistics?.sheetsCount ?: 0
                        ExamMetadata(
                            totalQuestions = item.totalQuestions,
                            createdAt = item.createdAt,
                            sheetsCount = sheetCount,
                            status = item.status
                        )

                        // Show stats if available
                        if (examStatistics != null && examStatistics.hasStats() && sheetCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExamStats(
                                avgScore = examStatistics.avgScore?.toInt(),
                                topScore = examStatistics.topScore?.toInt(),
                                lowestScore = examStatistics.lowestScore?.toInt()
                            )
                        }
                    }
                }
                ExamQuickActionButton(
                    actions.quickAction,
                    onActionClick,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }


    @Composable
    fun ExamQuickActionButton(
        config: QuickActionButton?,
        onClick: (ExamAction) -> Unit,
        modifier: Modifier = Modifier
    ) {
        if (config == null) return
        if (config.secondaryAction != null) {
            // Split button (two actions)
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenericButton(
                    text = config.action.label,
                    type = config.style,
                    icon = config.action.icon,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onClick(config.action)
                    }
                )
                GenericButton(
                    text = config.secondaryAction.label,
                    type = ButtonType.SECONDARY,
                    icon = config.secondaryAction.icon,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onClick(config.action)
                    }
                )
            }
        } else {
            // Single button
            GenericButton(
                text = config.action.label,
                type = config.style,
                icon = config.action.icon,
                modifier = modifier.fillMaxWidth(),
                onClick = {
                    onClick(config.action)
                }
            )
        }
    }

    @Composable
    private fun ExamIcon() {
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
    }

    @Composable
    private fun ExamHeader(
        examEntity: ExamEntity,
        actions: ExamActionPopupConfig,
        onActionClick: (ExamAction) -> Unit,
    ) {
        var showMenu by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = examEntity.examName,
                color = Black,
                style = AppTypography.body1Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(status = examEntity.status)

            // Three-dot menu icon
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = Grey500
                )
            }


            // Popup menu - now aligned to actual TopEnd of card
            ExamActionsPopup(
                expanded = showMenu,
                examEntity = examEntity,
                viewModel = examListViewModel,
                actions = actions,
                onActionClick = { action ->
                    onActionClick(action)
                },
                onDismiss = { showMenu = false },
            )
        }
    }

    @Composable
    private fun ExamMetadata(
        totalQuestions: Int,
        createdAt: Long,
        sheetsCount: Int,
        status: ExamStatus
    ) {
        val formattedDate = formatTimestamp(createdAt)

        val infoLine = "$totalQuestions questions • Created $formattedDate"

        val sheetsLine = when {
            status == ExamStatus.DRAFT -> null
            sheetsCount > 0 -> "Sheets: $sheetsCount scanned"
            else -> "No sheets scanned yet"
        }

        val displayText = if (sheetsLine != null) {
            "$infoLine\n$sheetsLine"
        } else {
            infoLine
        }

        Text(
            text = displayText,
            color = Grey500,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.body2Regular
        )
    }

    @Composable
    private fun ExamStats(
        avgScore: Int?,
        topScore: Int?,
        lowestScore: Int?
    ) {
        val validAvg = avgScore?.coerceIn(0, 100) ?: 0
        val validTop = topScore?.coerceIn(0, 100) ?: 0
        val validLow = lowestScore?.coerceIn(0, 100) ?: 0

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Avg",
                color = Grey600,
                style = AppTypography.body4Medium
            )
            Text(
                text = "$validAvg%",
                color = Grey900,
                style = AppTypography.label3Bold
            )

            Text("•", color = Grey400, style = AppTypography.body4Medium)

            Text(
                text = "Top",
                color = Grey600,
                style = AppTypography.body4Medium
            )
            Text(
                text = "$validTop%",
                color = Green600,
                style = AppTypography.label3Bold
            )

            Text("•", color = Grey400, style = AppTypography.body4Medium)

            Text(
                text = "Low",
                color = Grey600,
                style = AppTypography.body4Medium
            )
            Text(
                text = "$validLow%",
                color = Orange600,
                style = AppTypography.label3Bold
            )
        }
    }

    @Composable
    private fun StatusBadge(status: ExamStatus) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Grey200)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status.name,
                color = Grey500,
                style = AppTypography.label4Medium
            )
        }
    }

    @Composable
    private fun FilterChips(
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
                .padding(start = 12.dp, top = 12.dp, end = 16.dp)
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
                            style = AppTypography.label3Medium
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
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }

    @Composable
    private fun SearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {

        GenericTextField(
            value = searchQuery,
            placeholder = "Search by exam name",
            onValueChange = { onSearchQueryChange(it) },
            prefix = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search Icon",
                    tint = Grey500
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }

    @Composable
    private fun ErrorScreen(
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

    private fun handleTemplateSelection(assetFile: String) {
        Template.loadOmrTemplateSafe(this, assetFile).let { result ->
            when (result) {
                is OperationResult.Error -> {
                    Toast.makeText(
                        this,
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is OperationResult.Success -> {
                    startActivity(
                        Intent(this, ScanBaseActivity::class.java).apply {
                            putExtra(EXTRA_TEMPLATE, result.data)
                        }
                    )
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}