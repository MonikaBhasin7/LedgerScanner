package com.example.ledgerscanner.feature.scanner.exam.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.database.entity.ExamEntity

sealed class ExamAction(
    val label: String,
    val icon: ImageVector,
    val isDangerous: Boolean = false
) {
    data object ContinueSetup : ExamAction("Continue Setup", Icons.Default.Edit)
    data object ScanSheets : ExamAction("Scan Sheets", Icons.Default.PhotoCamera)
    data object ViewResults : ExamAction("View Results", Icons.Default.BarChart)

    data object ViewReport : ExamAction("View Report", Icons.Default.Analytics)
    data object MarkCompleted : ExamAction("Mark as Completed", Icons.Default.CheckCircle)
    data object EditExam : ExamAction("Edit Exam", Icons.Default.Edit)
    data object Duplicate : ExamAction("Duplicate", Icons.Default.ContentCopy)
    data object ExportResults : ExamAction("Export Results", Icons.Default.FileDownload)
    data object Archive : ExamAction("Archive", Icons.Default.Archive)
    data object Restore : ExamAction("Restore Exam", Icons.Default.Restore)
    data object Delete : ExamAction("Delete", Icons.Default.Delete, isDangerous = true)
}

sealed class ExamActionDialog(examEntity: ExamEntity) {
    data class Delete(val examEntity: ExamEntity) : ExamActionDialog(examEntity)
    data class Duplicate(val examEntity: ExamEntity) : ExamActionDialog(examEntity)
    data class MarkCompleted(val examEntity: ExamEntity) : ExamActionDialog(examEntity)
    data class Archive(val examEntity: ExamEntity) : ExamActionDialog(examEntity)
    data class Restore(val examEntity: ExamEntity) : ExamActionDialog(examEntity)
}

data class ExamActionPopupConfig(
    val menuItems: List<ExamAction>,
    val quickAction: QuickActionButton? = null
)

data class QuickActionButton(
    val action: ExamAction,
    val style: ButtonType = ButtonType.PRIMARY,
    val secondaryAction: ExamAction? = null // For split buttons
)
