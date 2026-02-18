package com.example.ledgerscanner.feature.scanner.statistics.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.ToolbarAction
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.QuestionStat
import com.example.ledgerscanner.feature.scanner.statistics.viewModel.ExamStatisticsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 08/01/26
// ===========================================================================

@Composable
fun ExamStatisticsScreen(
    navController: NavHostController,
    examStatisticsViewModel: ExamStatisticsViewModel = hiltViewModel(),
    examEntity: ExamEntity,
) {
    val statistics by examStatisticsViewModel.statistics.collectAsStateWithLifecycle()
    val isLoading by examStatisticsViewModel.isLoading.collectAsStateWithLifecycle()
    val handleBack = rememberBackHandler(navController)

    LaunchedEffect(examEntity) {
        examStatisticsViewModel.loadStatistics(examEntity.id)
    }

    Scaffold(
        containerColor = Color(0xFFF4F7FB),
        topBar = {
            GenericToolbar(
                title = "Exam Statistics",
                onBackClick = handleBack,
                actions = listOf(
                    ToolbarAction.Icon(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        onClick = { /* Share */ }
                    ),
                    ToolbarAction.Icon(
                        icon = Icons.Default.Download,
                        contentDescription = "Download",
                        onClick = { /* Download */ }
                    )
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exam Header
                item {
                    ExamHeader(
                        title = examEntity.examName,
                        totalQuestions = examEntity.totalQuestions,
                        optionsPerQuestion = examEntity.template.options_per_question,
                        sheetsCount = statistics.sheetsCount,
                        firstScannedAt = statistics.firstScannedAt,
                        lastScannedAt = statistics.lastScannedAt
                    )
                }
                // Stats Cards Row 1
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            title = "Average Score",
                            value = statistics.getValidAvgScore(),
                            subtitle = "${statistics.sheetsCount} sheets",
                            subtitleColor = Green500,
                            icon = Icons.Default.TrendingUp,
                            backgroundColor = Blue100.copy(alpha = 0.55f)
                        )
                        StatCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            title = "Highest Score",
                            value = statistics.getValidTopScore(),
                            subtitle = "Top performer",
                            subtitleColor = Green500,
                            icon = Icons.Default.EmojiEvents,
                            backgroundColor = Green500.copy(alpha = 0.14f)
                        )
                    }
                }

                // Stats Cards Row 2
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            title = "Lowest Score",
                            value = statistics.getValidLowestScore(),
                            subtitle = "Needs support",
                            subtitleColor = Orange600,
                            icon = Icons.Default.TrendingDown,
                            backgroundColor = Orange600.copy(alpha = 0.14f)
                        )
                        StatCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            title = "Pass Rate (≥40%)",
                            value = statistics.getValidPassRate(),
                            subtitle = "Overall outcome",
                            subtitleColor = Blue500,
                            icon = Icons.Default.CheckCircle,
                            backgroundColor = Blue100.copy(alpha = 0.55f)
                        )
                    }
                }

                // Score Distribution
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        ScoreDistributionCard(statistics.scoreDistribution)
                    }
                }

                // Detailed Breakdown
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        DetailedBreakdownCard(
                            totalCorrect = statistics.totalCorrect,
                            totalWrong = statistics.totalWrong,
                            totalUnanswered = statistics.totalUnanswered
                        )
                    }
                }

                // Question Analysis
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        QuestionAnalysisCard(statistics.questionStats)
                    }
                }

                // Question Performance Heatmap
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        QuestionHeatmapCard(statistics.questionStats, examEntity.totalQuestions)
                    }
                }

                // Insights
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        InsightsCard(statistics)
                    }
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Export report */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export Report", style = AppTypography.text14SemiBold)
                        }
                        Button(
                            onClick = { /* Share insights */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Insights", style = AppTypography.text14SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamHeader(
    title: String,
    totalQuestions: Int,
    optionsPerQuestion: Int,
    sheetsCount: Int,
    firstScannedAt: Long?,
    lastScannedAt: Long?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE5EEF8),
                            Color(0xFFDCEAF8),
                            Color(0xFFEFF5FC)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier
                        .size(24.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = AppTypography.text18SemiBold,
                    color = Color(0xFF0F1B2D)
                )
            }

            Text(
                text = "$totalQuestions Questions • $optionsPerQuestion Options • OMR Analysis",
                style = AppTypography.text14Regular,
                color = Color(0xFF7D8FA6)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Scanned: ${formatScannedDateRange(firstScannedAt, lastScannedAt)}",
                    style = AppTypography.text14Regular,
                    color = Color(0xFF7D8FA6)
                )

                Surface(
                    color = Color(0xFF2F80ED),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "$sheetsCount Sheets Analyzed",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        style = AppTypography.text14SemiBold,
                        color = White
                    )
                }
            }
        }
    }
}

private fun formatScannedDateRange(firstScannedAt: Long?, lastScannedAt: Long?): String {
    if (firstScannedAt == null || lastScannedAt == null) return "--"

    val start = Date(firstScannedAt)
    val end = Date(lastScannedAt)
    val startMonthDay = SimpleDateFormat("MMM d", Locale.getDefault()).format(start)
    val endMonthDayYear = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(end)
    val sameDay = firstScannedAt == lastScannedAt

    return if (sameDay) endMonthDayYear else "$startMonthDay - $endMonthDayYear"
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    icon: ImageVector,
    backgroundColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = AppTypography.text12Medium,
                    color = Grey600
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Grey700
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = AppTypography.text24Bold,
                color = Black
            )
            Text(
                text = subtitle,
                style = AppTypography.text11Medium,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun ScoreDistributionCard(distribution: Map<String, Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Grey200, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Score Distribution",
                style = AppTypography.text16SemiBold,
                color = Black
            )
            Text(
                text = "Number of students per score range",
                style = AppTypography.text12Regular,
                color = Grey600
            )
            Spacer(Modifier.height(24.dp))

            val totalSheets = distribution.values.sum()
            if (totalSheets == 0) {
                Text(
                    text = "No score distribution data yet.",
                    style = AppTypography.text13Regular,
                    color = Grey600
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val ranges = listOf("0-25", "26-50", "51-75", "76-100")
                    val colors = listOf(
                        Color(0xFFF44336),
                        Color(0xFFFF9800),
                        Color(0xFFFFEB3B),
                        Color(0xFF4CAF50)
                    )

                    ranges.forEachIndexed { index, range ->
                        val count = distribution[range] ?: 0
                        val maxCount = distribution.values.maxOrNull() ?: 1
                        val heightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = count.toString(),
                                style = AppTypography.text16SemiBold,
                                color = Black
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height((120 * heightFraction).dp.coerceAtLeast(20.dp))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(colors[index])
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("0-25", "26-50", "51-75", "76-100").forEach { label ->
                        Text(
                            text = label,
                            style = AppTypography.text11Medium,
                            color = Grey600,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedBreakdownCard(
    totalCorrect: Int,
    totalWrong: Int,
    totalUnanswered: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Grey200, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFDFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detailed Breakdown",
                style = AppTypography.text16SemiBold,
                color = Black
            )
            Spacer(Modifier.height(16.dp))

            BreakdownRow(
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF4CAF50),
                label = "Total Correct",
                value = totalCorrect.toString()
            )
            Spacer(Modifier.height(12.dp))
            BreakdownRow(
                icon = Icons.Default.Cancel,
                iconColor = Color(0xFFF44336),
                label = "Total Wrong",
                value = totalWrong.toString()
            )
            Spacer(Modifier.height(12.dp))
            BreakdownRow(
                icon = Icons.Default.Remove,
                iconColor = Color(0xFF9E9E9E),
                label = "Unanswered",
                value = totalUnanswered.toString()
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = AppTypography.text14Medium,
                color = Grey700
            )
        }
        Text(
            text = value,
            style = AppTypography.text16SemiBold,
            color = Black
        )
    }
}

@Composable
private fun QuestionAnalysisCard(questionStats: Map<Int, QuestionStat>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Grey200, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question Analysis",
                style = AppTypography.text16SemiBold,
                color = Black
            )
            Text(
                text = "Questions ranked by difficulty",
                style = AppTypography.text12Regular,
                color = Grey600
            )
            Spacer(Modifier.height(16.dp))
            if (questionStats.isEmpty()) {
                Text(
                    text = "No question-wise data available yet.",
                    style = AppTypography.text13Regular,
                    color = Grey600
                )
                return@Column
            }

            // Hardest Questions
            Surface(
                color = Color(0xFFFFF2F4),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Hardest Questions",
                            style = AppTypography.text14SemiBold,
                            color = Color(0xFFC62828),
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    val hardest = questionStats.values
                        .sortedBy { it.correctPercentage }
                        .take(3)

                    hardest.forEach { stat ->
                        QuestionStatRow(
                            questionNumber = stat.questionNumber,
                            percentage = stat.correctPercentage.toInt(),
                            correctCount = stat.correctCount,
                            totalCount = stat.totalAttempts,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Easiest Questions
            Surface(
                color = Color(0xFFF1FAF2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Easiest Questions",
                            style = AppTypography.text14SemiBold,
                            color = Color(0xFF2E7D32),
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    val easiest = questionStats.values
                        .sortedByDescending { it.correctPercentage }
                        .take(3)

                    easiest.forEach { stat ->
                        QuestionStatRow(
                            questionNumber = stat.questionNumber,
                            percentage = stat.correctPercentage.toInt(),
                            correctCount = stat.correctCount,
                            totalCount = stat.totalAttempts,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionStatRow(
    questionNumber: Int,
    percentage: Int,
    correctCount: Int,
    totalCount: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Q$questionNumber",
            style = AppTypography.text14Medium,
            color = Black
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .width(80.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$percentage% correct ($correctCount/$totalCount)",
                style = AppTypography.text12Medium,
                color = color
            )
        }
    }
}

@Composable
private fun QuestionHeatmapCard(questionStats: Map<Int, QuestionStat>, totalQuestions: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Grey200, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question Performance Heatmap",
                style = AppTypography.text16SemiBold,
                color = Black
            )
            Spacer(Modifier.height(16.dp))

            // Create grid of questions (10 columns)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val rows = (totalQuestions + 9) / 10
                repeat(rows) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(10) { col ->
                            val questionNum = row * 10 + col + 1
                            if (questionNum <= totalQuestions) {
                                val stat = questionStats[questionNum]
                                val percentage = stat?.correctPercentage?.toInt()
                                val color = when {
                                    percentage == null -> Grey200
                                    percentage < 40 -> Color(0xFFF44336)
                                    percentage < 60 -> Color(0xFFFF9800)
                                    percentage < 80 -> Color(0xFFFFEB3B)
                                    else -> Color(0xFF4CAF50)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = questionNum.toString(),
                                        style = AppTypography.text11SemiBold,
                                        color = if (percentage == null) Grey700 else Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("●", Grey200, "No attempts")
                LegendItem("●", Color(0xFFF44336), "<40%")
                LegendItem("●", Color(0xFFFF9800), "40-60%")
                LegendItem("●", Color(0xFFFFEB3B), "60-80%")
                LegendItem("●", Color(0xFF4CAF50), ">80%")
            }
        }
    }
}

@Composable
private fun LegendItem(bullet: String, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = bullet, color = color, style = AppTypography.text16Bold)
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = AppTypography.text12Regular,
            color = Grey700
        )
    }
}

@Composable
private fun InsightsCard(statistics: ExamStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFC9E4FF), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Insights",
                    style = AppTypography.text16SemiBold,
                    color = Black
                )
            }
            Spacer(Modifier.height(12.dp))

            buildInsights(statistics).forEach { insight ->
                InsightItem(
                    icon = insight.icon,
                    text = insight.text,
                    color = insight.color
                )
            }
        }
    }
}

private data class InsightEntry(
    val icon: ImageVector,
    val text: String,
    val color: Color
)

private fun buildInsights(statistics: ExamStatistics): List<InsightEntry> {
    if (statistics.sheetsCount == 0) {
        return listOf(
            InsightEntry(
                icon = Icons.Default.Info,
                text = "No scanned sheets yet. Scan sheets to view insights.",
                color = Blue500
            )
        )
    }

    val insights = mutableListOf<InsightEntry>()
    val hardest = statistics.questionStats.values.minByOrNull { it.correctPercentage }
    val easiest = statistics.questionStats.values.maxByOrNull { it.correctPercentage }
    val passRate = statistics.passRate ?: 0f

    if (hardest != null) {
        insights += InsightEntry(
            icon = Icons.Default.Warning,
            text = "Q${hardest.questionNumber} is hardest (${hardest.correctPercentage.toInt()}% correct).",
            color = Color(0xFFFF9800)
        )
    }
    if (easiest != null) {
        insights += InsightEntry(
            icon = Icons.Default.CheckCircle,
            text = "Q${easiest.questionNumber} is strongest (${easiest.correctPercentage.toInt()}% correct).",
            color = Color(0xFF4CAF50)
        )
    }
    insights += if (passRate < 40f) {
        InsightEntry(
            icon = Icons.Default.Error,
            text = "Pass rate is low (${passRate.toInt()}%). Consider remedial support.",
            color = Color(0xFFF44336)
        )
    } else {
        InsightEntry(
            icon = Icons.Default.Info,
            text = "Pass rate is ${passRate.toInt()}% across ${statistics.sheetsCount} sheets.",
            color = Color(0xFF2196F3)
        )
    }

    return insights.take(3)
}

@Composable
private fun InsightItem(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = AppTypography.text14Regular,
            color = Grey800
        )
    }
}
