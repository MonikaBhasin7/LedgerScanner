package com.example.ledgerscanner.feature.scanner.statistics.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import com.example.ledgerscanner.feature.scanner.statistics.viewModel.ExportResultsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@AndroidEntryPoint
class ExportResultsActivity : ComponentActivity() {

    private val viewModel: ExportResultsViewModel by viewModels()

    private val examEntity: ExamEntity by lazy {
        intent.getParcelableExtra<ExamEntity>(EXTRA_EXAM_ENTITY)
            ?: throw IllegalStateException("ExamEntity is required")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                ExportResultsScreen(
                    examEntity = examEntity,
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_EXAM_ENTITY = "extra_exam_entity"

        fun launch(context: Context, examEntity: ExamEntity): Intent {
            return Intent(context, ExportResultsActivity::class.java).apply {
                putExtra(EXTRA_EXAM_ENTITY, examEntity)
            }
        }
    }
}

private enum class ExportFormat {
    CSV,
    PDF_INDIVIDUAL,
    PDF_COMBINED
}

private enum class Destination {
    DEVICE,
    EMAIL,
    DRIVE,
    SHARE
}

private data class ExportOptionState(
    val includeIdentity: Boolean = true,
    val includeScores: Boolean = true,
    val includeAnswers: Boolean = true,
    val includeTimestamps: Boolean = false,
    val includeHeaders: Boolean = true
)

@Composable
private fun ExportResultsScreen(
    examEntity: ExamEntity,
    viewModel: ExportResultsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sheetCount by viewModel.sheetCount.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()

    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var exportOptions by remember { mutableStateOf(ExportOptionState()) }
    var fileName by remember { mutableStateOf("${examEntity.examName}_Results") }
    var destination by remember { mutableStateOf(Destination.DEVICE) }
    var selectedSort by remember {
        mutableStateOf(ScanResultRepository.ExportSortBy.ENROLLMENT_ASC)
    }
    var showSortMenu by remember { mutableStateOf(false) }
    var pendingExportPath by remember { mutableStateOf<String?>(null) }
    var pendingExportName by remember { mutableStateOf<String?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val path = pendingExportPath
        if (uri == null || path.isNullOrBlank()) {
            pendingExportPath = null
            pendingExportName = null
            return@rememberLauncherForActivityResult
        }
        val sourceFile = File(path)
        val result = saveFileToUri(context, sourceFile, uri)
        Toast.makeText(
            context,
            if (result) "Export saved to device" else "Failed to save file",
            Toast.LENGTH_SHORT
        ).show()
        pendingExportPath = null
        pendingExportName = null
    }

    LaunchedEffect(examEntity.id) {
        viewModel.loadSheetCount(examEntity.id)
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is UiState.Success -> {
                state.data?.let { payload ->
                    when (destination) {
                        Destination.DEVICE -> {
                            pendingExportPath = payload.path
                            val targetName =
                                "${sanitizeForDisplayFileName(fileName)}.${payload.suggestedExtension}"
                            pendingExportName = targetName
                            createDocumentLauncher.launch(targetName)
                        }

                        else -> {
                            shareExportedFile(
                                context = context,
                                filePath = payload.path,
                                examName = payload.examName,
                                destination = destination,
                                mimeType = payload.mimeType
                            )
                        }
                    }
                    viewModel.resetExportState()
                } ?: run {
                    Toast.makeText(context, "Export failed: empty payload", Toast.LENGTH_SHORT)
                        .show()
                    viewModel.resetExportState()
                }
            }

            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetExportState()
            }

            else -> Unit
        }
    }

    LaunchedEffect(previewState) {
        when (val state = previewState) {
            is UiState.Success -> {
                val previewData = state.data
                if (previewData != null && !previewData.previewFilePath.isNullOrBlank()) {
                    openPreviewFile(
                        context = context,
                        filePath = previewData.previewFilePath,
                        mimeType = previewData.previewMimeType.orEmpty()
                    )
                    viewModel.resetPreviewState()
                } else if (previewData != null) {
                    showPreviewDialog = true
                }
            }

            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetPreviewState()
            }

            else -> Unit
        }
    }

    Scaffold(
        containerColor = Grey100,
        topBar = {
            GenericToolbar(
                title = "Export Results",
                onBackClick = onBack
            )
        },
        bottomBar = {
            ExportBottomBar(
                isLoading = exportState is UiState.Loading,
                onPreview = {
                    if (!hasAtLeastOneExportOption(exportOptions)) {
                        Toast.makeText(
                            context,
                            "Select at least one export option",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ExportBottomBar
                    }
                    viewModel.loadPreview(
                        examEntity = examEntity,
                        config = buildExportConfig(exportOptions, selectedSort, fileName),
                        format = selectedFormat.toPreviewFormat()
                    )
                },
                onExport = {
                    if (!hasAtLeastOneExportOption(exportOptions)) {
                        Toast.makeText(
                            context,
                            "Select at least one export option",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ExportBottomBar
                    }

                    val config = buildExportConfig(exportOptions, selectedSort, fileName)
                    when (selectedFormat) {
                        ExportFormat.CSV -> viewModel.exportCsv(examEntity, config)
                        ExportFormat.PDF_COMBINED -> viewModel.exportPdfCombined(examEntity, config)
                        ExportFormat.PDF_INDIVIDUAL -> viewModel.exportPdfIndividual(examEntity, config)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExportSummaryHeader(
                    examName = examEntity.examName,
                    sheetCount = sheetCount
                )
            }

            item {
                ExportFormatCard(
                    selectedFormat = selectedFormat,
                    onSelect = { selectedFormat = it }
                )
            }

            item {
                ExportOptionsCard(
                    options = exportOptions,
                    onChange = { exportOptions = it }
                )
            }

            item {
                SortAndNamingCard(
                    selectedSort = selectedSort,
                    onSortClick = { showSortMenu = true },
                    fileName = fileName,
                    onFileNameChange = { fileName = it }
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    ScanResultRepository.ExportSortBy.values().forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(text = sort.toReadableLabel()) },
                            onClick = {
                                selectedSort = sort
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            item {
                DestinationCard(
                    selected = destination,
                    onSelect = { destination = it }
                )
            }

            item {
                InfoBanner(sheetCount = sheetCount)
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (showPreviewDialog) {
            val previewData = (previewState as? UiState.Success)?.data
            AlertDialog(
                onDismissRequest = {
                    showPreviewDialog = false
                    viewModel.resetPreviewState()
                },
                title = {
                    Text(
                        text = "${selectedFormat.toPreviewLabel()} Preview (${previewData?.totalRows ?: 0} rows)",
                        style = AppTypography.text16Bold
                    )
                },
                text = {
                    Text(
                        text = previewData?.previewText.orEmpty().ifBlank { "No preview available" },
                        style = AppTypography.text12Regular,
                        color = Grey800
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPreviewDialog = false
                            viewModel.resetPreviewState()
                        }
                    ) { Text("Close") }
                }
            )
        }
    }
}

@Composable
private fun ExportSummaryHeader(
    examName: String,
    sheetCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFE9F3FF), White)
                )
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            tint = Blue500,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = examName,
            style = AppTypography.text20Bold,
            color = Grey900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$sheetCount sheets selected for export",
            style = AppTypography.text14Regular,
            color = Grey700
        )
    }
}

@Composable
private fun ExportFormatCard(
    selectedFormat: ExportFormat,
    onSelect: (ExportFormat) -> Unit
) {
    ExportCard(title = "Export Format", subtitle = "Choose output file format") {
        FormatOption(
            selected = selectedFormat == ExportFormat.CSV,
            title = "CSV File",
            subtitle = "Spreadsheet format. Best for Excel and analysis.",
            icon = Icons.Default.FilePresent,
            onClick = { onSelect(ExportFormat.CSV) }
        )
        FormatOption(
            selected = selectedFormat == ExportFormat.PDF_INDIVIDUAL,
            title = "PDF - Individual Reports",
            subtitle = "One PDF per student packaged as ZIP.",
            icon = Icons.Default.Description,
            onClick = { onSelect(ExportFormat.PDF_INDIVIDUAL) }
        )
        FormatOption(
            selected = selectedFormat == ExportFormat.PDF_COMBINED,
            title = "PDF - Combined Report",
            subtitle = "Single PDF report with all students.",
            icon = Icons.Default.Description,
            onClick = { onSelect(ExportFormat.PDF_COMBINED) }
        )
    }
}

@Composable
private fun ExportOptionsCard(
    options: ExportOptionState,
    onChange: (ExportOptionState) -> Unit
) {
    ExportCard(title = "Export Options", subtitle = "Customize what to include") {
        OptionCheckboxRow(
            label = "Student identity (Enrollment + Barcode)",
            checked = options.includeIdentity,
            onToggle = { onChange(options.copy(includeIdentity = !options.includeIdentity)) }
        )
        OptionCheckboxRow(
            label = "Scores & percentages",
            checked = options.includeScores,
            onToggle = { onChange(options.copy(includeScores = !options.includeScores)) }
        )
        OptionCheckboxRow(
            label = "Individual answers (Q1..Qn)",
            checked = options.includeAnswers,
            onToggle = { onChange(options.copy(includeAnswers = !options.includeAnswers)) }
        )
        OptionCheckboxRow(
            label = "Scan timestamps",
            checked = options.includeTimestamps,
            onToggle = { onChange(options.copy(includeTimestamps = !options.includeTimestamps)) }
        )
        OptionCheckboxRow(
            label = "Include column headers",
            checked = options.includeHeaders,
            onToggle = { onChange(options.copy(includeHeaders = !options.includeHeaders)) }
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Select All",
                style = AppTypography.text12Bold,
                color = Blue500,
                modifier = Modifier.clickable {
                    onChange(
                        ExportOptionState(
                            includeIdentity = true,
                            includeScores = true,
                            includeAnswers = true,
                            includeTimestamps = true,
                            includeHeaders = true
                        )
                    )
                }
            )
            Text(text = "|", color = Grey500)
            Text(
                text = "Deselect All",
                style = AppTypography.text12Bold,
                color = Blue500,
                modifier = Modifier.clickable {
                    onChange(
                        options.copy(
                            includeIdentity = false,
                            includeScores = false,
                            includeAnswers = false,
                            includeTimestamps = false
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SortAndNamingCard(
    selectedSort: ScanResultRepository.ExportSortBy,
    onSortClick: () -> Unit,
    fileName: String,
    onFileNameChange: (String) -> Unit
) {
    ExportCard(title = "Sort & File Name", subtitle = null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSortClick),
            shape = RoundedCornerShape(10.dp),
            color = White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Grey200, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sort by: ${selectedSort.toReadableLabel()}",
                    style = AppTypography.text14Regular,
                    color = Grey900,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = Grey700)
            }
        }

        Spacer(Modifier.height(12.dp))

        GenericTextField(
            value = fileName,
            label = "File name",
            onValueChange = onFileNameChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${fileName.ifBlank { "results_export" }}.csv",
            style = AppTypography.text12Regular,
            color = Grey700
        )
    }
}

@Composable
private fun DestinationCard(
    selected: Destination,
    onSelect: (Destination) -> Unit
) {
    ExportCard(title = "Save To", subtitle = null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinationButton(
                    label = "Device",
                    icon = Icons.Default.Smartphone,
                    selected = selected == Destination.DEVICE,
                    onClick = { onSelect(Destination.DEVICE) },
                    modifier = Modifier.weight(1f)
                )
                DestinationButton(
                    label = "Email",
                    icon = Icons.Default.Email,
                    selected = selected == Destination.EMAIL,
                    onClick = { onSelect(Destination.EMAIL) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DestinationButton(
                    label = "Drive",
                    icon = Icons.Default.Folder,
                    selected = selected == Destination.DRIVE,
                    onClick = { onSelect(Destination.DRIVE) },
                    modifier = Modifier.weight(1f)
                )
                DestinationButton(
                    label = "Share",
                    icon = Icons.Default.Share,
                    selected = selected == Destination.SHARE,
                    onClick = { onSelect(Destination.SHARE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InfoBanner(sheetCount: Int) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(Color(0xFFF3F6FA), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = Grey700, modifier = Modifier.size(16.dp))
        Text(
            text = "Estimated size: ${estimateCsvSize(sheetCount)} • Time: ~2 sec",
            style = AppTypography.text12Regular,
            color = Grey700
        )
    }
}

@Composable
private fun ExportBottomBar(
    isLoading: Boolean,
    onPreview: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPreview,
            enabled = !isLoading,
            modifier = Modifier.weight(0.35f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Blue500),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue500)
        ) {
            Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = "Preview", style = AppTypography.text14SemiBold)
        }

        Button(
            onClick = onExport,
            enabled = !isLoading,
            modifier = Modifier.weight(0.65f),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue500)
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = White)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isLoading) "Exporting..." else "Export",
                style = AppTypography.text14SemiBold,
                color = White
            )
        }
    }
}

@Composable
private fun ExportCard(
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = AppTypography.text18SemiBold, color = Grey900)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = subtitle, style = AppTypography.text12Regular, color = Grey700)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FormatOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Blue500 else Grey200
    val background = when {
        !enabled -> Grey100
        selected -> Blue100.copy(alpha = 0.35f)
        else -> White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioDot(selected = selected)
            Spacer(Modifier.width(10.dp))
            Icon(icon, null, tint = Grey800, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = title, style = AppTypography.text14SemiBold, color = Grey900)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = AppTypography.text12Regular,
            color = Grey700,
            modifier = Modifier.padding(start = 30.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(2.dp, if (selected) Blue500 else Grey500, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(Blue500, CircleShape)
            )
        }
    }
}

@Composable
private fun OptionCheckboxRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (checked) Blue500 else White, RoundedCornerShape(4.dp))
                .border(2.dp, if (checked) Blue500 else Grey500, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(text = "✓", color = White, style = AppTypography.text12Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = AppTypography.text14Regular,
            color = Grey900
        )
    }
}

@Composable
private fun DestinationButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .background(
                if (selected) Blue100.copy(alpha = 0.35f) else White,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Blue500 else Grey200,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Blue500 else Grey700,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = AppTypography.text12SemiBold,
            color = Grey900
        )
    }
}

private fun ScanResultRepository.ExportSortBy.toReadableLabel(): String {
    return when (this) {
        ScanResultRepository.ExportSortBy.SCANNED_AT_DESC -> "Scanned Time (Newest)"
        ScanResultRepository.ExportSortBy.SCANNED_AT_ASC -> "Scanned Time (Oldest)"
        ScanResultRepository.ExportSortBy.SCORE_DESC -> "Score (High to Low)"
        ScanResultRepository.ExportSortBy.SCORE_ASC -> "Score (Low to High)"
        ScanResultRepository.ExportSortBy.ENROLLMENT_ASC -> "Enrollment (A-Z)"
        ScanResultRepository.ExportSortBy.ENROLLMENT_DESC -> "Enrollment (Z-A)"
    }
}

private fun ExportFormat.toPreviewLabel(): String {
    return when (this) {
        ExportFormat.CSV -> "CSV"
        ExportFormat.PDF_COMBINED -> "PDF (Combined)"
        ExportFormat.PDF_INDIVIDUAL -> "PDF (Individual ZIP)"
    }
}

private fun ExportFormat.toPreviewFormat(): ExportResultsViewModel.PreviewFormat {
    return when (this) {
        ExportFormat.CSV -> ExportResultsViewModel.PreviewFormat.CSV
        ExportFormat.PDF_COMBINED -> ExportResultsViewModel.PreviewFormat.PDF_COMBINED
        ExportFormat.PDF_INDIVIDUAL -> ExportResultsViewModel.PreviewFormat.PDF_INDIVIDUAL
    }
}

private fun estimateCsvSize(sheetCount: Int): String {
    val kb = (sheetCount * 2).coerceAtLeast(10)
    return "~${kb} KB"
}

private fun openPreviewFile(
    context: Context,
    filePath: String,
    mimeType: String
) {
    val previewFile = File(filePath)
    if (!previewFile.exists()) {
        Toast.makeText(context, "Preview file not found", Toast.LENGTH_SHORT).show()
        return
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        previewFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType.ifBlank { "application/pdf" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(Intent.createChooser(intent, "Preview export"))
    }.onFailure {
        Toast.makeText(context, "No app available to preview file", Toast.LENGTH_SHORT).show()
    }
}

private fun shareExportedFile(
    context: Context,
    filePath: String,
    examName: String,
    destination: Destination,
    mimeType: String
) {
    val exportFile = File(filePath)
    if (!exportFile.exists()) {
        Toast.makeText(context, "Export file not found", Toast.LENGTH_SHORT).show()
        return
    }

    val fileUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        exportFile
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, fileUri)
        putExtra(Intent.EXTRA_SUBJECT, "Exam Results - $examName")
        putExtra(Intent.EXTRA_TEXT, "Exported results for $examName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    if (destination == Destination.DRIVE) {
        intent.setPackage("com.google.android.apps.docs")
    }

    val chooserTitle = when (destination) {
        Destination.DEVICE -> "Save export to device"
        Destination.EMAIL -> "Send export via email"
        Destination.DRIVE -> "Upload export to Drive"
        Destination.SHARE -> "Share exported file"
    }

    runCatching {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }.onFailure {
        if (destination == Destination.DRIVE) {
            val fallbackIntent = intent.apply { `package` = null }
            runCatching {
                context.startActivity(Intent.createChooser(fallbackIntent, "Share exported file"))
            }.onFailure {
                Toast.makeText(context, "No app available to share file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No app available to share file", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun hasAtLeastOneExportOption(options: ExportOptionState): Boolean {
    return options.includeIdentity || options.includeScores || options.includeAnswers || options.includeTimestamps
}

private fun buildExportConfig(
    options: ExportOptionState,
    sortBy: ScanResultRepository.ExportSortBy,
    fileName: String
): ScanResultRepository.ExportCsvConfig {
    return ScanResultRepository.ExportCsvConfig(
        includeIdentity = options.includeIdentity,
        includeScores = options.includeScores,
        includeAnswers = options.includeAnswers,
        includeTimestamps = options.includeTimestamps,
        includeHeaders = options.includeHeaders,
        sortBy = sortBy,
        fileName = fileName
    )
}

private fun sanitizeForDisplayFileName(raw: String): String {
    val cleaned = raw
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
    return cleaned.ifBlank { "exam_results" }
}

private fun saveFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
    return runCatching {
        if (!sourceFile.exists()) return false
        context.contentResolver.openOutputStream(targetUri)?.use { out ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(out)
            }
        } ?: throw IOException("Unable to open output stream")
        true
    }.getOrElse { false }
}
