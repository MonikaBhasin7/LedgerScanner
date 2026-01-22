package com.example.ledgerscanner.feature.scanner.statistics.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.QuestionStat
import com.example.ledgerscanner.feature.scanner.statistics.viewModel.ExamStatisticsViewModel

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 08/01/26
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Exam Statistics") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = { /* Download */ }) {
                        Icon(Icons.Default.Download, "Download")
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                }
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
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exam Header
                item {
                    ExamHeader(
                        examEntity.examName,
                        examEntity.totalQuestions,
                        statistics.sheetsCount
                    )
                }

                // Stats Cards Row 1
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Average Score",
                            value = statistics.getValidAvgScore(),
                            subtitle = "${statistics.sheetsCount} sheets",
                            subtitleColor = Color(0xFF4CAF50),
                            icon = Icons.Default.TrendingUp,
                            backgroundColor = Color(0xFFE3F2FD)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Highest Score",
                            value = statistics.getValidTopScore(),
                            subtitle = "45/50",
                            subtitleColor = Color(0xFF4CAF50),
                            icon = Icons.Default.EmojiEvents,
                            backgroundColor = Color(0xFFE8F5E9)
                        )
                    }
                }

                // Stats Cards Row 2
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Lowest Score",
                            value = statistics.getValidLowestScore(),
                            subtitle = "21/50",
                            subtitleColor = Color(0xFFFF9800),
                            icon = Icons.Default.TrendingDown,
                            backgroundColor = Color(0xFFFFF3E0)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Pass Rate (≥40%)",
                            value = statistics.getValidPassRate(),
                            subtitle = "26/30",
                            subtitleColor = Color(0xFF2196F3),
                            icon = Icons.Default.CheckCircle,
                            backgroundColor = Color(0xFFE3F2FD)
                        )
                    }
                }

                // Score Distribution
                item {
                    ScoreDistributionCard(statistics.scoreDistribution)
                }

                // Detailed Breakdown
                item {
                    DetailedBreakdownCard(
                        totalCorrect = statistics.totalCorrect,
                        totalWrong = statistics.totalWrong,
                        totalUnanswered = statistics.totalUnanswered
                    )
                }

                // Question Analysis
                item {
                    QuestionAnalysisCard(statistics.questionStats)
                }

                // Question Performance Heatmap
                item {
                    QuestionHeatmapCard(statistics.questionStats, examEntity.totalQuestions)
                }

                // Insights
                item {
                    InsightsCard(statistics)
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Export report */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export Report")
                        }
                        Button(
                            onClick = { /* Share insights */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Insights")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamHeader(title: String, totalQuestions: Int, sheetsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📝",
            fontSize = 32.sp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$totalQuestions Questions • 4 Options • 2 Columns",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Scanned: Dec 16-20, 2025",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "$sheetsCount Sheets Analyzed",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
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
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun ScoreDistributionCard(distribution: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Score Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Number of students per score range",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
                listOf("<40%", "40-60%", "60-80%", "80-90%", ">90%").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Detailed Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuestionAnalysisCard(questionStats: Map<Int, QuestionStat>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Questions ranked by difficulty",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Hardest Questions
            Surface(
                color = Color(0xFFFCE4EC),
                shape = RoundedCornerShape(8.dp)
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
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
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
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp)
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
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { percentage / 100f },
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
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun QuestionHeatmapCard(questionStats: Map<Int, QuestionStat>, totalQuestions: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question Performance Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                                val percentage = stat?.correctPercentage?.toInt() ?: 0
                                val color = when {
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
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
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
        Text(text = bullet, color = color, fontSize = 20.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun InsightsCard(statistics: com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💡",
                    fontSize = 24.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))

            InsightItem(
                icon = Icons.Default.Info,
                text = "Q15 and Q23 need review - only 40-45% correct",
                color = Color(0xFF2196F3)
            )
            InsightItem(
                icon = Icons.Default.Warning,
                text = "4 students scored below passing grade - consider remedial",
                color = Color(0xFFFF9800)
            )
            InsightItem(
                icon = Icons.Default.CheckCircle,
                text = "Strong performance in Q1-Q10 (avg 85%+)",
                color = Color(0xFF4CAF50)
            )
        }
    }
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
            style = MaterialTheme.typography.bodyMedium
        )
    }
}