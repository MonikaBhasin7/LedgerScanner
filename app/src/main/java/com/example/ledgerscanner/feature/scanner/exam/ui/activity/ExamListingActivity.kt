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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.auth.LogoutViewModel
import com.example.ledgerscanner.auth.AuthState
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.auth.ui.LoginActivity
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericEmptyState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.ButtonSize
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
import com.example.ledgerscanner.feature.scanner.exam.model.DrawerItem
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.model.QuickActionButton
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.ExamActionConfirmationDialog
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.ExamActionsPopup
import com.example.ledgerscanner.feature.scanner.exam.ui.dialog.TemplatePickerDialog
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.ExamListViewModel
import com.example.ledgerscanner.feature.scanner.results.ui.activity.ScanResultActivity
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.CreateTemplateActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity
import com.example.ledgerscanner.feature.scanner.statistics.activity.ExamStatisticsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ExamListingActivity : ComponentActivity() {

    private val examListViewModel: ExamListViewModel by viewModels()
    private val scanResultViewModel: ScanResultViewModel by viewModels()
    private val logoutViewModel: LogoutViewModel by viewModels()

    @Inject
    lateinit var tokenStore: TokenStore

    companion object {
        private const val EXTRA_TEMPLATE = "template"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                val globalLoggedOut by AuthState.loggedOut.collectAsState()
                val logoutState by logoutViewModel.logoutState.collectAsState()

                LaunchedEffect(globalLoggedOut) {
                    if (globalLoggedOut) {
                        AuthState.reset()
                        startActivity(Intent(this@ExamListingActivity, LoginActivity::class.java))
                        finish()
                    }
                }

                LaunchedEffect(logoutState) {
                    when (logoutState) {
                        is UiState.Success -> {
                            startActivity(
                                Intent(
                                    this@ExamListingActivity,
                                    LoginActivity::class.java
                                )
                            )
                            finish()
                            logoutViewModel.reset()
                        }

                        is UiState.Error -> {
                            val message = (logoutState as UiState.Error).message
                            Toast.makeText(this@ExamListingActivity, message, Toast.LENGTH_SHORT)
                                .show()
                            logoutViewModel.reset()
                        }

                        else -> {}
                    }
                }

                ExamListingScreen(
                    viewModel = examListViewModel,
                    onLogout = { logoutViewModel.logout() },
                    memberName = tokenStore.getMemberName(),
                    memberPhone = tokenStore.getMemberPhone()
                )
            }
        }
    }

    @Composable
    private fun ExamListingScreen(
        viewModel: ExamListViewModel,
        onLogout: () -> Unit,
        memberName: String?,
        memberPhone: String?
    ) {
        val context = LocalContext.current
        var showTemplatePicker by remember { mutableStateOf(false) }
        var examFilter by remember { mutableStateOf<ExamStatus?>(null) }
        val examListResponse by viewModel.examList.collectAsState()
        var searchQuery by remember { mutableStateOf("") }

        var showDeleteAndDuplicateDialog by remember { mutableStateOf<ExamActionDialog?>(null) }
        val deleteState by viewModel.deleteExamState.collectAsState()
        val duplicateState by viewModel.duplicateExamState.collectAsState()
        val examStatistics by scanResultViewModel.examStatsCache.collectAsState()
        var showLogoutDialog by remember { mutableStateOf(false) }
        val drawerState = androidx.compose.material3.rememberDrawerState(
            initialValue = androidx.compose.material3.DrawerValue.Closed
        )
        val drawerScope = rememberCoroutineScope()


        // Handle filter changes - starts collecting from DB
        LaunchedEffect(examFilter) {
            if (searchQuery.isBlank()) {
                examListViewModel.getExamList(examFilter)
            } else {
                examListViewModel.searchExam(searchQuery)
            }
        }

        // Handle search query changes
        LaunchedEffect(searchQuery) {
            if (searchQuery.isBlank()) {
                examListViewModel.getExamList(examFilter)
            } else {
                examListViewModel.searchExam(searchQuery)
            }
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

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Blue500)
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (memberName ?: "U").take(1).uppercase(),
                                        style = AppTypography.h4Bold,
                                        color = Blue500
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = memberName ?: "User",
                                        style = AppTypography.h4Bold,
                                        color = White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = memberPhone ?: "",
                                        style = AppTypography.body4Medium,
                                        color = White
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Quick Actions",
                                style = AppTypography.label2SemiBold,
                                color = Grey600
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val items = listOf(
                                DrawerItem("Exams", Icons.Outlined.ListAlt) {
                                    drawerScope.launch { drawerState.close() }
                                },
                                DrawerItem("Create Exam", Icons.Outlined.PostAdd) {
                                    drawerScope.launch { drawerState.close() }
                                    context.startActivity(
                                        Intent(
                                            context,
                                            CreateExamActivity::class.java
                                        )
                                    )
                                },
                                DrawerItem("Create Template", Icons.Outlined.Description) {
                                    drawerScope.launch { drawerState.close() }
                                    context.startActivity(
                                        Intent(
                                            context,
                                            CreateTemplateActivity::class.java
                                        )
                                    )
                                },
                            )

                            items.forEach { item ->
                                NavigationDrawerItem(
                                    label = { Text(item.label, style = AppTypography.body2Medium) },
                                    selected = false,
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = Blue500
                                        )
                                    },
                                    onClick = item.onClick,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = Blue100,
                                        unselectedTextColor = Grey900,
                                        selectedTextColor = Blue500
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Account",
                                style = AppTypography.label2SemiBold,
                                color = Grey600
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            NavigationDrawerItem(
                                label = { Text("Logout", style = AppTypography.body2Medium) },
                                selected = false,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Logout,
                                        contentDescription = null,
                                        tint = Blue500
                                    )
                                },
                                onClick = {
                                    drawerScope.launch { drawerState.close() }
                                    showLogoutDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Blue100
                                )
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = Grey100,
                topBar = {
                    GenericToolbar(
                        title = "Exams",
                        navigationIcon = Icons.Default.Menu,
                        onNavigationClick = { drawerScope.launch { drawerState.open() } },
                        actions = listOf(
//                            ToolbarAction.IconText(
//                                icon = Icons.Default.Addchart,
//                                text = "Create Exam",
//                                onClick = {
//                                    startActivity(Intent(context, CreateExamActivity::class.java))
//                                }
//                            ),
//                            ToolbarAction.IconText(
//                                icon = Icons.Default.Addchart,
//                                text = "Create Template",
//                                onClick = {
//                                    startActivity(
//                                        Intent(
//                                            context,
//                                            CreateTemplateActivity::class.java
//                                        )
//                                    )
//                                }
//                            )
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

            if (showLogoutDialog) {
                ExamActionConfirmationDialog(
                    title = "Logout",
                    message = "Are you sure you want to logout?",
                    confirmText = "Logout",
                    onConfirm = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    onDismiss = { showLogoutDialog = false }
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
                ScanResultActivity.launchScannedSheetsScreen(context, examEntity)
            }

            ExamAction.ViewReport -> {
                ExamStatisticsActivity.launchExamStatisticsScreen(context, examEntity).apply {
                    startActivity(this)
                }
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
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 12.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
        val sheetCount = examStatistics?.sheetsCount ?: 0
        val actions = examListViewModel.getExamActionForStatus(
            status = item.status,
            hasScannedSheets = sheetCount > 0
        )
        var showMenu by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Grey200, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .genericClick { actions.quickAction?.action?.let { onClick(it) } },
//                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExamIcon(status = item.status)
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusBadge(status = item.status)
                        Text(
                            text = item.examName,
                            color = Black,
                            style = AppTypography.text16Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = Grey500
                        )
                    }
                }

                ExamActionsPopup(
                    expanded = showMenu,
                    examEntity = item,
                    viewModel = examListViewModel,
                    actions = actions,
                    onActionClick = { action ->
                        showMenu = false
                        onActionClick(action)
                    },
                    onDismiss = { showMenu = false },
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    ExamMetadata(
                        totalQuestions = item.totalQuestions,
                        createdAt = item.createdAt,
                        sheetsCount = sheetCount,
                        status = item.status
                    )
                    StatusDetailLine(status = item.status, sheetsCount = sheetCount)
                }

                if (examStatistics != null && examStatistics.hasStats() && sheetCount > 0) {
                    Box(modifier = Modifier.padding(bottom = 10.dp)) {
                        ExamStats(
                            avgScore = examStatistics.avgScore?.toInt(),
                            topScore = examStatistics.topScore?.toInt(),
                            lowestScore = examStatistics.lowestScore?.toInt()
                        )
                    }
                }

                val shouldNudgeScan = sheetCount == 0 && actions.quickAction?.action is ExamAction.ScanSheets

                if (shouldNudgeScan) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Blue500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tap to scan your first sheet",
                            color = Grey500,
                            style = AppTypography.text11Regular,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                ExamQuickActionButton(
                    config = actions.quickAction,
                    onClick = onActionClick,
//                    modifier = Modifier.padding(top = 2.dp),
                    primaryLabelOverride = if (shouldNudgeScan) "Start Scanning" else null,
                    showPulse = shouldNudgeScan
                )
            }
        }
    }

    @Composable
    fun ExamQuickActionButton(
        config: QuickActionButton?,
        onClick: (ExamAction) -> Unit,
        modifier: Modifier = Modifier,
        primaryLabelOverride: String? = null,
        showPulse: Boolean = false
    ) {
        if (config == null) return

        // Completed exam matches the reference: main outlined button + compact icon action
        if (config.style == ButtonType.SECONDARY && config.secondaryAction != null) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenericButton(
                    text = primaryLabelOverride ?: config.action.label,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.SMALL,
                    icon = config.action.icon,
                    modifier = Modifier.weight(1f),
                    onClick = { onClick(config.action) }
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Grey200, RoundedCornerShape(12.dp))
                        .background(White)
                        .genericClick { onClick(config.secondaryAction) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.secondaryAction.icon,
                        contentDescription = config.secondaryAction.label,
                        tint = Grey600,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            return
        }

        if (config.secondaryAction != null) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenericButton(
                    text = primaryLabelOverride ?: config.action.label,
                    type = config.style,
                    size = ButtonSize.SMALL,
                    icon = config.action.icon,
                    modifier = Modifier.weight(1f),
                    onClick = { onClick(config.action) }
                )
                GenericButton(
                    text = config.secondaryAction.label,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.SMALL,
                    icon = config.secondaryAction.icon,
                    modifier = Modifier.weight(1f),
                    onClick = { onClick(config.secondaryAction) }
                )
            }
            return
        }

        if (showPulse) {
            val transition = rememberInfiniteTransition(label = "scan-ripple")
            val rippleOneScale = transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleOneScale"
            )
            val rippleOneAlpha = transition.animateFloat(
                initialValue = 0.22f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleOneAlpha"
            )
            val rippleTwoScale = transition.animateFloat(
                initialValue = 1f,
                targetValue = 1.26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, delayMillis = 420),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleTwoScale"
            )
            val rippleTwoAlpha = transition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, delayMillis = 420),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleTwoAlpha"
            )

            Box(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = rippleOneScale.value
                            scaleY = rippleOneScale.value
                            alpha = rippleOneAlpha.value
                        }
                        .clip(RoundedCornerShape(50.dp))
                        .background(Blue500)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = rippleTwoScale.value
                            scaleY = rippleTwoScale.value
                            alpha = rippleTwoAlpha.value
                        }
                        .clip(RoundedCornerShape(50.dp))
                        .background(Blue500)
                )
                GenericButton(
                    text = primaryLabelOverride ?: config.action.label,
                    type = config.style,
                    size = ButtonSize.SMALL,
                    icon = config.action.icon,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onClick(config.action) }
                )
            }
        } else {
            GenericButton(
                text = primaryLabelOverride ?: config.action.label,
                type = config.style,
                size = ButtonSize.SMALL,
                icon = config.action.icon,
                modifier = modifier.fillMaxWidth(),
                onClick = { onClick(config.action) }
            )
        }
    }

    @Composable
    private fun ExamIcon(status: ExamStatus) {
        val bg = when (status) {
            ExamStatus.DRAFT -> Color(0xFFFFF2D6)
            ExamStatus.ACTIVE -> Color(0xFFE3F0FF)
            ExamStatus.COMPLETED -> Color(0xFFECEBFF)
            ExamStatus.ARCHIVED -> Grey200
        }
        val tint = when (status) {
            ExamStatus.DRAFT -> Color(0xFFFFB300)
            ExamStatus.ACTIVE -> Blue500
            ExamStatus.COMPLETED -> Color(0xFF6C63FF)
            ExamStatus.ARCHIVED -> Grey500
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = "Exam Icon",
                tint = tint,
                modifier = Modifier.size(17.dp)
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

        val sheetsLine = when {
            status == ExamStatus.DRAFT -> null
            sheetsCount > 0 -> "Sheets: $sheetsCount scanned"
            else -> "No sheets scanned yet"
        }

        Text(
            text = "$totalQuestions questions • Created $formattedDate",
            color = Grey500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.text11Medium
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                value = "$validAvg%",
                label = "AVG",
                valueColor = Blue500,
                backgroundColor = Color(0xFFE7F0FA),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "$validTop%",
                label = "TOP",
                valueColor = Green600,
                backgroundColor = Color(0xFFEAF7EE),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = "$validLow%",
                label = "LOW",
                valueColor = Orange600,
                backgroundColor = Color(0xFFFFF2E6),
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun StatTile(
        value: String,
        label: String,
        valueColor: Color,
        backgroundColor: Color,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = valueColor,
                style = AppTypography.text13Bold
            )
            Text(
                text = label,
                color = Grey500,
                style = AppTypography.text10Regular
            )
        }
    }

    @Composable
    private fun StatusDetailLine(status: ExamStatus, sheetsCount: Int) {
        val (icon, text, tint) = when (status) {
            ExamStatus.DRAFT -> Triple(
                Icons.Outlined.WarningAmber,
                "Incomplete setup",
                Grey600
            )

            ExamStatus.ACTIVE -> if (sheetsCount > 0) {
                Triple(Icons.Outlined.BarChart, "$sheetsCount sheets scanned", Grey600)
            } else {
                Triple(Icons.Outlined.Description, "No sheets scanned yet", Grey600)
            }

            ExamStatus.COMPLETED -> Triple(
                Icons.Filled.CheckCircle,
                "$sheetsCount sheets scanned",
                Grey600
            )

            ExamStatus.ARCHIVED -> Triple(
                Icons.Outlined.Description,
                "Archived • $sheetsCount sheets",
                Grey600
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                color = Black,
                style = AppTypography.text12Medium
            )
        }
    }

    @Composable
    private fun StatusBadge(status: ExamStatus) {
        val (bg, textColor) = when (status) {
            ExamStatus.DRAFT -> Color(0xFFFFF4D8) to Color(0xFF8A6A00)
            ExamStatus.ACTIVE -> Color(0xFFE5F6EA) to Color(0xFF2F8A45)
            ExamStatus.COMPLETED -> Color(0xFFE9EDFF) to Color(0xFF4F67D8)
            ExamStatus.ARCHIVED -> Grey200 to Grey600
        }
        Box(
            modifier = Modifier
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status.name,
                color = textColor,
                style = AppTypography.label5Medium
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
    private fun SearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {

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
