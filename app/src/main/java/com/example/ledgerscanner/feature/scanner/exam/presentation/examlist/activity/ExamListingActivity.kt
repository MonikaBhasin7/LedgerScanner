package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
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
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.auth.AuthState
import com.example.ledgerscanner.auth.LogoutViewModel
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.auth.ui.LoginActivity
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
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
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.DrawerItem
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamActionDialog
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component.ExamActionConfirmationDialog
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component.ExamList
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component.FilterChips
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component.SearchBar
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.dialog.TemplatePickerDialog
import com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.viewmodel.ExamListViewModel
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.activity.CreateExamActivity
import com.example.ledgerscanner.feature.scanner.results.ui.activity.ScanResultActivity
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.CreateTemplateActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity
import com.example.ledgerscanner.feature.scanner.statistics.activity.ExamStatisticsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        val updateExamStatusState by viewModel.updateExamStatusState.collectAsState()
        val examStatistics by scanResultViewModel.examStatsCache.collectAsState()
        var pendingCompletedExamId by remember { mutableStateOf<Int?>(null) }
        var celebratingExamId by remember { mutableStateOf<Int?>(null) }
        var waitForCompletedCard by remember { mutableStateOf(false) }
        var walkthroughDismissed by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        var pendingCommitAction by remember { mutableStateOf<(() -> Unit)?>(null) }
        var pendingUndoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
        var undoSnackbarJob by remember { mutableStateOf<Job?>(null) }
        var hiddenExamIds by remember { mutableStateOf(emptySet<Int>()) }
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

        // Handle update exam status state
        LaunchedEffect(updateExamStatusState) {
            when (val state = updateExamStatusState) {
                is UiState.Success -> {
                    Toast.makeText(context, "Exam status updated", Toast.LENGTH_SHORT).show()
                    if (pendingCompletedExamId != null) {
                        waitForCompletedCard = true
                    }
                    viewModel.resetUpdateExamStatusState()
                }

                is UiState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    pendingCompletedExamId = null
                    waitForCompletedCard = false
                    viewModel.resetUpdateExamStatusState()
                }

                else -> {}
            }
        }

        LaunchedEffect(examListResponse, waitForCompletedCard, pendingCompletedExamId) {
            val targetExamId = pendingCompletedExamId ?: return@LaunchedEffect
            if (!waitForCompletedCard) return@LaunchedEffect
            val items = (examListResponse as? UiState.Success)?.data.orEmpty()
            val completedCardVisible =
                items.any { it.id == targetExamId && it.status == ExamStatus.COMPLETED }
            if (completedCardVisible) {
                celebratingExamId = targetExamId
                waitForCompletedCard = false
                pendingCompletedExamId = null
            }
        }

        LaunchedEffect(celebratingExamId) {
            if (celebratingExamId != null) {
                delay(1500)
                celebratingExamId = null
            }
        }

        val visibleExamItems = (examListResponse as? UiState.Success)?.data.orEmpty()
            .filterNot { hiddenExamIds.contains(it.id) }
        val showListControls = examListResponse !is UiState.Success ||
            visibleExamItems.isNotEmpty() ||
            examFilter != null ||
            searchQuery.isNotBlank()
        val firstNoScanExamId = visibleExamItems
            .firstOrNull { exam ->
                exam.status == ExamStatus.ACTIVE && (examStatistics[exam.id]?.sheetsCount ?: 0) == 0
            }?.id
        val showNoScanWalkthrough = firstNoScanExamId != null && !walkthroughDismissed

        fun enqueueUndoAction(
            message: String,
            undoAction: () -> Unit,
            commitAction: () -> Unit
        ) {
            pendingCommitAction?.invoke()
            pendingCommitAction = null
            pendingUndoAction = null
            undoSnackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()

            pendingUndoAction = undoAction
            pendingCommitAction = commitAction
            undoSnackbarJob = drawerScope.launch {
                val dismissJob = launch {
                    delay(6500)
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    withDismissAction = false,
                    duration = SnackbarDuration.Indefinite
                )
                dismissJob.cancel()

                if (result == SnackbarResult.ActionPerformed) {
                    pendingUndoAction?.invoke()
                } else {
                    pendingCommitAction?.invoke()
                }
                pendingUndoAction = null
                pendingCommitAction = null
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
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { data ->
                        Snackbar(
                            containerColor = White,
                            contentColor = Black,
                            shape = RoundedCornerShape(14.dp),
                            action = {
                                TextButton(
                                    onClick = { data.performAction() },
                                    contentPadding = PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 0.dp
                                    )
                                ) {
                                    Text(
                                        text = data.visuals.actionLabel.orEmpty(),
                                        color = Blue500,
                                        style = AppTypography.text13SemiBold
                                    )
                                }
                            },
                            dismissAction = null
                        ) {
                            Text(
                                text = data.visuals.message,
                                color = Grey800,
                                style = AppTypography.text13Medium
                            )
                        }
                    }
                },
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
                    if (showListControls) {
                        SearchBar(searchQuery, onSearchQueryChange = {
                            searchQuery = it
                        })

                        val isLoading = examListResponse is UiState.Loading
                        FilterChips(
                            disableClicking = isLoading,
                            selectedFilter = examFilter,
                            onSelect = { selectedFilter ->
                                examFilter = selectedFilter
                            }
                        )
                    }

                    ExamList(
                        examListResponse = examListResponse,
                        examStatistics = examStatistics,
                        celebratingExamId = celebratingExamId,
                        hiddenExamIds = hiddenExamIds,
                        walkthroughExamId = firstNoScanExamId,
                        showNoScanWalkthrough = showNoScanWalkthrough,
                        selectedFilter = examFilter,
                        searchQuery = searchQuery,
                        onDismissNoScanWalkthrough = {
                            walkthroughDismissed = true
                        },
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
                                },
                                showMarkCompletedDialog = {
                                    showDeleteAndDuplicateDialog =
                                        ExamActionDialog.MarkCompleted(it)
                                },
                                showArchiveDialog = {
                                    showDeleteAndDuplicateDialog = ExamActionDialog.Archive(it)
                                },
                                showRestoreDialog = {
                                    showDeleteAndDuplicateDialog = ExamActionDialog.Restore(it)
                                }
                            )
                        },
                        onLoadStats = {
                            scanResultViewModel.loadStatsForExam(it)
                        },
                        actionsProvider = { status, hasScannedSheets ->
                            examListViewModel.getExamActionForStatus(
                                status = status,
                                hasScannedSheets = hasScannedSheets
                            )
                        },
                        onCreateExamClick = {
                            startActivity(Intent(context, CreateExamActivity::class.java))
                        },
                        onClearFilters = {
                            examFilter = null
                            searchQuery = ""
                            examListViewModel.getExamList(null)
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
                                },
                                showMarkCompletedDialog = {
                                    showDeleteAndDuplicateDialog =
                                        ExamActionDialog.MarkCompleted(it)
                                },
                                showArchiveDialog = {
                                    showDeleteAndDuplicateDialog = ExamActionDialog.Archive(it)
                                },
                                showRestoreDialog = {
                                    showDeleteAndDuplicateDialog = ExamActionDialog.Restore(it)
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
                            hiddenExamIds = hiddenExamIds + examEntity.id
                            enqueueUndoAction(
                                message = "Exam deleted",
                                undoAction = {
                                    hiddenExamIds = hiddenExamIds - examEntity.id
                                },
                                commitAction = {
                                    hiddenExamIds = hiddenExamIds - examEntity.id
                                    examListViewModel.deleteExam(examEntity.id)
                                }
                            )
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

                is ExamActionDialog.MarkCompleted -> {
                    val examEntity = (it as ExamActionDialog.MarkCompleted).examEntity
                    ExamActionConfirmationDialog(
                        title = "Mark Completed",
                        message = "Mark \"${examEntity.examName}\" as completed?",
                        confirmText = "Mark Completed",
                        onConfirm = {
                            val previousStatus = examEntity.status
                            pendingCompletedExamId = examEntity.id
                            waitForCompletedCard = true
                            examListViewModel.updateExamStatus(examEntity.id, ExamStatus.COMPLETED)
                            enqueueUndoAction(
                                message = "Marked as completed",
                                undoAction = {
                                    pendingCompletedExamId = null
                                    waitForCompletedCard = false
                                    celebratingExamId = null
                                    examListViewModel.updateExamStatus(
                                        examEntity.id,
                                        previousStatus
                                    )
                                },
                                commitAction = {}
                            )
                            showDeleteAndDuplicateDialog = null
                        },
                        onDismiss = {
                            showDeleteAndDuplicateDialog = null
                        }
                    )
                }

                is ExamActionDialog.Archive -> {
                    val examEntity = (it as ExamActionDialog.Archive).examEntity
                    ExamActionConfirmationDialog(
                        title = "Archive Exam",
                        message = "Archive \"${examEntity.examName}\"? You can still view results later.",
                        confirmText = "Archive",
                        onConfirm = {
                            val previousStatus = examEntity.status
                            examListViewModel.updateExamStatus(examEntity.id, ExamStatus.ARCHIVED)
                            enqueueUndoAction(
                                message = "Exam archived",
                                undoAction = {
                                    examListViewModel.updateExamStatus(
                                        examEntity.id,
                                        previousStatus
                                    )
                                },
                                commitAction = {}
                            )
                            showDeleteAndDuplicateDialog = null
                        },
                        onDismiss = {
                            showDeleteAndDuplicateDialog = null
                        }
                    )
                }

                is ExamActionDialog.Restore -> {
                    val examEntity = (it as ExamActionDialog.Restore).examEntity
                    ExamActionConfirmationDialog(
                        title = "Restore Exam",
                        message = "Restore \"${examEntity.examName}\" to active state?",
                        confirmText = "Restore",
                        onConfirm = {
                            examListViewModel.updateExamStatus(examEntity.id, ExamStatus.ACTIVE)
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
        showDuplicateDialog: (ExamEntity) -> Unit,
        showMarkCompletedDialog: (ExamEntity) -> Unit,
        showArchiveDialog: (ExamEntity) -> Unit,
        showRestoreDialog: (ExamEntity) -> Unit
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
                showMarkCompletedDialog(examEntity)
            }

            ExamAction.Duplicate -> {
                showDuplicateDialog(examEntity)
            }

            ExamAction.ExportResults -> {
                // TODO: Export results
                Toast.makeText(context, "Export Results - Coming soon", Toast.LENGTH_SHORT).show()
            }

            ExamAction.Archive -> {
                showArchiveDialog(examEntity)
            }

            ExamAction.Restore -> {
                showRestoreDialog(examEntity)
            }

            ExamAction.Delete -> {
                showDeleteDialog(examEntity)
            }
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
