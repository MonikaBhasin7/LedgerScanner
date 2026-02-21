package com.example.ledgerscanner.feature.scanner.results.repo

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.ledgerscanner.base.errors.ErrorMessages
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.getAnswersForQuestionIndex
import com.example.ledgerscanner.database.entity.setStudentDetails
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.QuestionStat
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
import com.example.ledgerscanner.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ScanResultRepository @Inject constructor(
    private val scanResultDao: ScanResultDao,
    private val examDao: ExamDao,
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager
) {
    enum class ExportOutputFormat {
        CSV,
        PDF_COMBINED,
        PDF_INDIVIDUAL
    }

    enum class ExportSortBy {
        SCANNED_AT_DESC,
        SCANNED_AT_ASC,
        SCORE_DESC,
        SCORE_ASC,
        ENROLLMENT_ASC,
        ENROLLMENT_DESC
    }

    data class ExportCsvConfig(
        val includeIdentity: Boolean = true,
        val includeScores: Boolean = true,
        val includeAnswers: Boolean = true,
        val includeTimestamps: Boolean = true,
        val includeHeaders: Boolean = true,
        val sortBy: ExportSortBy = ExportSortBy.SCANNED_AT_DESC,
        val fileName: String? = null
    )

    data class ExportPreviewPayload(
        val previewText: String = "",
        val totalRows: Int,
        val previewFilePath: String? = null,
        val previewMimeType: String? = null
    )

    suspend fun saveSheet(
        details: StudentDetailsForScanResult,
        scanResultEntity: ScanResultEntity,
    ): Long {
        return scanResultDao.insert(scanResultEntity.setStudentDetails(details.barcodeId))
    }

    // ============ Insert ============

    suspend fun insert(scanResult: ScanResultEntity): Long {
        return scanResultDao.insert(scanResult)
    }

    // ============ Update ============

    suspend fun update(scanResult: ScanResultEntity) {
        scanResultDao.update(scanResult)
    }

    // ============ Delete ============

    suspend fun delete(scanResult: ScanResultEntity) {
        scanResultDao.delete(scanResult)
    }

    suspend fun deleteById(id: Int) {
        scanResultDao.deleteById(id)
    }

    suspend fun deleteAllByExamId(examId: Int) {
        scanResultDao.deleteAllByExamId(examId)
    }

    // ============ Get ============

    fun getAllByExamId(examId: Int): Flow<List<ScanResultEntity>> {
        return scanResultDao.getAllByExamId(examId)
    }

    suspend fun getByIdOnce(id: Int): ScanResultEntity? {
        return scanResultDao.getByIdOnce(id)
    }

    // ============ Count ============

    fun getCountByExamId(examId: Int): Flow<Int> {
        return scanResultDao.getCountByExamId(examId)
    }

    suspend fun getCountByExamIdOnce(examId: Int): Int {
        return scanResultDao.getCountByExamIdOnce(examId)
    }

    // ============ Statistics ============

    fun getStatistics(examId: Int): Flow<ExamStatistics> {
        return scanResultDao.getBasicStatisticsByExamId(examId).map { basicStats ->
            val allResults = scanResultDao.getAllByExamIdOnce(examId)
            val exam = examDao.getExamById(examId)

            if (allResults.isEmpty()) {
                return@map ExamStatistics(
                    avgScore = basicStats.avgScore,
                    topScore = basicStats.topScore,
                    lowestScore = basicStats.lowestScore,
                    sheetsCount = basicStats.sheetsCount
                )
            }

            // Calculate totals
            var totalCorrect = 0
            var totalWrong = 0
            var totalUnanswered = 0

            // Question-wise statistics
            val answerKey = exam?.answerKey.orEmpty()
            val totalQuestions = exam?.totalQuestions
                ?: allResults.maxOfOrNull { it.totalQuestions }
                ?: 0

            // Score distribution
            val scoreDistribution = mutableMapOf(
                "0-25" to 0,
                "26-50" to 0,
                "51-75" to 0,
                "76-100" to 0
            )

            allResults.forEach { result ->
                totalCorrect += result.correctCount
                totalWrong += result.wrongCount
                totalUnanswered += result.blankCount

                // Score distribution
                val percent = result.scorePercent
                when {
                    percent <= 25f -> scoreDistribution["0-25"] = scoreDistribution["0-25"]!! + 1
                    percent <= 50f -> scoreDistribution["26-50"] = scoreDistribution["26-50"]!! + 1
                    percent <= 75f -> scoreDistribution["51-75"] = scoreDistribution["51-75"]!! + 1
                    else -> scoreDistribution["76-100"] = scoreDistribution["76-100"]!! + 1
                }

            }

            val questionStats = buildQuestionStats(
                results = allResults,
                totalQuestions = totalQuestions,
                answerKey = answerKey
            )

            // Calculate median and pass rate
            val medianScore = calculateMedian(allResults.map { it.scorePercent })
            val passRate = (allResults.count { it.scorePercent >= 40f }.toFloat() / allResults.size) * 100
            val firstScannedAt = allResults.minOfOrNull { it.scannedAt }
            val lastScannedAt = allResults.maxOfOrNull { it.scannedAt }

            ExamStatistics(
                avgScore = basicStats.avgScore,
                topScore = basicStats.topScore,
                lowestScore = basicStats.lowestScore,
                sheetsCount = basicStats.sheetsCount,
                totalCorrect = totalCorrect,
                totalWrong = totalWrong,
                totalUnanswered = totalUnanswered,
                firstScannedAt = firstScannedAt,
                lastScannedAt = lastScannedAt,
                questionStats = questionStats,
                scoreDistribution = scoreDistribution,
                medianScore = medianScore,
                passRate = passRate
            )
        }
    }

    private fun calculateMedian(scores: List<Float>): Float {
        if (scores.isEmpty()) return 0f
        val sorted = scores.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2
        } else {
            sorted[size / 2]
        }
    }

    private fun buildQuestionStats(
        results: List<ScanResultEntity>,
        totalQuestions: Int,
        answerKey: Map<Int, Int>
    ): Map<Int, QuestionStat> {
        if (totalQuestions <= 0 || results.isEmpty()) return emptyMap()

        val attemptsByQuestion = mutableMapOf<Int, Int>()
        val correctByQuestion = mutableMapOf<Int, Int>()
        val hasAnswerKey = answerKey.isNotEmpty()

        for (questionNum in 1..totalQuestions) {
            attemptsByQuestion[questionNum] = 0
            correctByQuestion[questionNum] = 0
        }

        results.forEach { result ->
            for (questionNum in 1..totalQuestions) {
                val userAnswers = result.studentAnswers[questionNum]
                    ?: result.studentAnswers[questionNum - 1]
                    ?: emptyList()
                if (userAnswers.isEmpty()) continue

                attemptsByQuestion[questionNum] = (attemptsByQuestion[questionNum] ?: 0) + 1

                if (hasAnswerKey) {
                    val correctOption = answerKey[questionNum]
                        ?: answerKey[questionNum - 1]
                        ?: continue
                    val hasMultipleMarks =
                        result.multipleMarksDetected?.contains(questionNum) == true ||
                                result.multipleMarksDetected?.contains(questionNum - 1) == true
                    val isCorrect = !hasMultipleMarks &&
                            userAnswers.size == 1 &&
                            userAnswers.first() == correctOption
                    if (isCorrect) {
                        correctByQuestion[questionNum] = (correctByQuestion[questionNum] ?: 0) + 1
                    }
                } else {
                    // Fallback when answer key is absent: show attempt-rate so UI is not empty.
                    correctByQuestion[questionNum] = (correctByQuestion[questionNum] ?: 0) + 1
                }
            }
        }

        return (1..totalQuestions)
            .mapNotNull { questionNum ->
                val totalAttempts = attemptsByQuestion[questionNum] ?: 0
                if (totalAttempts == 0) return@mapNotNull null

                val correctCount = correctByQuestion[questionNum] ?: 0
                val correctPercentage = (correctCount.toFloat() / totalAttempts) * 100f

                questionNum to QuestionStat(
                    questionNumber = questionNum,
                    correctCount = correctCount,
                    totalAttempts = totalAttempts,
                    correctPercentage = correctPercentage
                )
            }
            .toMap()
    }

    suspend fun deleteSheet(sheetId: Int) {
        withContext(Dispatchers.IO) {
            scanResultDao.deleteById(sheetId)
        }
    }

    suspend fun deleteMultipleSheets(sheetIds: List<Int>) {
        withContext(Dispatchers.IO) {
            scanResultDao.deleteByIds(sheetIds)
        }
    }

    suspend fun exportResultsCsv(
        examEntity: ExamEntity,
        config: ExportCsvConfig = ExportCsvConfig()
    ): File = withContext(Dispatchers.IO) {
        val results = scanResultDao.getAllByExamIdOnce(examEntity.id)
        if (results.isEmpty()) {
            throw IllegalStateException(ErrorMessages.EXPORT_NO_DATA)
        }
        if (!config.includeIdentity && !config.includeScores && !config.includeAnswers && !config.includeTimestamps) {
            throw IllegalArgumentException("Select at least one export option")
        }

        val sortedResults = sortResults(results, config.sortBy)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val baseName = config.fileName?.takeIf { it.isNotBlank() } ?: examEntity.examName
        val fileName = "${sanitizeFileName(baseName)}_${timestamp}.csv"
        val exportFile = File(exportsDir, fileName)

        exportFile.writeText(
            buildCsv(
                examEntity = examEntity,
                results = sortedResults,
                config = config
            )
        )
        exportFile
    }

    suspend fun getExportPreview(
        examEntity: ExamEntity,
        config: ExportCsvConfig,
        format: ExportOutputFormat = ExportOutputFormat.CSV,
        maxDataRows: Int = 8
    ): ExportPreviewPayload = withContext(Dispatchers.IO) {
        val results = scanResultDao.getAllByExamIdOnce(examEntity.id)
        if (results.isEmpty()) {
            throw IllegalStateException(ErrorMessages.EXPORT_NO_DATA)
        }
        val sortedResults = sortResults(results, config.sortBy)
        when (format) {
            ExportOutputFormat.CSV -> {
                val csv = buildCsv(
                    examEntity = examEntity,
                    results = sortedResults.take(maxDataRows),
                    config = config
                )
                ExportPreviewPayload(
                    previewText = csv,
                    totalRows = sortedResults.size
                )
            }

            ExportOutputFormat.PDF_COMBINED -> {
                val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val baseName = config.fileName?.takeIf { it.isNotBlank() } ?: examEntity.examName
                val previewFile = File(
                    exportsDir,
                    "${sanitizeFileName(baseName)}_${timestamp}_preview_combined.pdf"
                )
                createPdf(
                    outputFile = previewFile,
                    examEntity = examEntity,
                    results = sortedResults,
                    config = config,
                    title = "${examEntity.examName} - Combined Report"
                )
                ExportPreviewPayload(
                    totalRows = sortedResults.size,
                    previewFilePath = previewFile.absolutePath,
                    previewMimeType = "application/pdf"
                )
            }

            ExportOutputFormat.PDF_INDIVIDUAL -> {
                val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val baseName = config.fileName?.takeIf { it.isNotBlank() } ?: examEntity.examName
                val previewFile = File(
                    exportsDir,
                    "${sanitizeFileName(baseName)}_${timestamp}_preview_individual.pdf"
                )
                createPdf(
                    outputFile = previewFile,
                    examEntity = examEntity,
                    results = listOf(sortedResults.first()),
                    config = config,
                    title = "${examEntity.examName} - Sheet #${sortedResults.first().id}"
                )
                ExportPreviewPayload(
                    totalRows = sortedResults.size,
                    previewText = "Sample preview for one individual PDF report.",
                    previewFilePath = previewFile.absolutePath,
                    previewMimeType = "application/pdf"
                )
            }
        }
    }

    suspend fun exportResultsPdfCombined(
        examEntity: ExamEntity,
        config: ExportCsvConfig = ExportCsvConfig()
    ): File = withContext(Dispatchers.IO) {
        val results = scanResultDao.getAllByExamIdOnce(examEntity.id)
        if (results.isEmpty()) {
            throw IllegalStateException(ErrorMessages.EXPORT_NO_DATA)
        }
        val sortedResults = sortResults(results, config.sortBy)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val baseName = config.fileName?.takeIf { it.isNotBlank() } ?: examEntity.examName
        val fileName = "${sanitizeFileName(baseName)}_${timestamp}.pdf"
        val exportFile = File(exportsDir, fileName)

        createPdf(
            outputFile = exportFile,
            examEntity = examEntity,
            results = sortedResults,
            config = config,
            title = "${examEntity.examName} - Combined Report"
        )
        exportFile
    }

    suspend fun exportResultsPdfIndividualZip(
        examEntity: ExamEntity,
        config: ExportCsvConfig = ExportCsvConfig()
    ): File = withContext(Dispatchers.IO) {
        val results = scanResultDao.getAllByExamIdOnce(examEntity.id)
        if (results.isEmpty()) {
            throw IllegalStateException(ErrorMessages.EXPORT_NO_DATA)
        }
        val sortedResults = sortResults(results, config.sortBy)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val baseName = config.fileName?.takeIf { it.isNotBlank() } ?: examEntity.examName
        val tempDir = File(exportsDir, "tmp_pdf_$timestamp").apply { mkdirs() }
        val zipFile = File(exportsDir, "${sanitizeFileName(baseName)}_${timestamp}_individual.zip")

        sortedResults.forEach { result ->
            val studentRef = result.enrollmentNumber
                ?.takeIf { it.isNotBlank() }
                ?.let { "_$it" }
                .orEmpty()
            val pdfFile = File(tempDir, "sheet_${result.id}$studentRef.pdf")
            createPdf(
                outputFile = pdfFile,
                examEntity = examEntity,
                results = listOf(result),
                config = config,
                title = "${examEntity.examName} - Sheet #${result.id}"
            )
        }

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            tempDir.listFiles()
                ?.filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
                ?.forEach { file ->
                    FileInputStream(file).use { input ->
                        zipOut.putNextEntry(ZipEntry(file.name))
                        input.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
        }

        tempDir.listFiles()?.forEach { it.delete() }
        tempDir.delete()
        zipFile
    }

    private fun buildCsv(
        examEntity: ExamEntity,
        results: List<ScanResultEntity>,
        config: ExportCsvConfig
    ): String {
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val totalQuestions = examEntity.totalQuestions.coerceAtLeast(0)
        val alwaysHeaders = listOf(
            "sheet_id",
            "exam_id",
            "exam_name"
        )
        val identityHeaders = if (config.includeIdentity) {
            listOf("enrollment_number", "barcode")
        } else {
            emptyList()
        }
        val scoreHeaders = if (config.includeScores) {
            listOf(
                "score",
                "score_percent",
                "correct_count",
                "wrong_count",
                "blank_count",
                "attempted_count",
                "low_confidence_count",
                "multiple_marks_count",
                "avg_confidence",
                "min_confidence"
            )
        } else {
            emptyList()
        }
        val timeHeaders = if (config.includeTimestamps) {
            listOf("scanned_at")
        } else {
            emptyList()
        }
        val questionHeaders = if (config.includeAnswers) {
            (1..totalQuestions).map { "Q$it" }
        } else {
            emptyList()
        }

        return buildString {
            if (config.includeHeaders) {
                appendLine(
                    (alwaysHeaders + identityHeaders + scoreHeaders + timeHeaders + questionHeaders)
                        .joinToString(",")
                )
            }

            results.forEach { result ->
                val attemptedCount = (result.totalQuestions - result.blankCount).coerceAtLeast(0)
                val alwaysColumns = listOf(
                    result.id.toString(),
                    examEntity.id.toString(),
                    examEntity.examName
                ).map(::escapeCsv)

                val identityColumns = if (config.includeIdentity) {
                    listOf(
                        result.enrollmentNumber.orEmpty(),
                        result.barCode.orEmpty()
                    ).map(::escapeCsv)
                } else {
                    emptyList()
                }

                val scoreColumns = if (config.includeScores) {
                    listOf(
                        result.score.toString(),
                        result.scorePercent.toString(),
                        result.correctCount.toString(),
                        result.wrongCount.toString(),
                        result.blankCount.toString(),
                        attemptedCount.toString(),
                        (result.lowConfidenceQuestions?.size ?: 0).toString(),
                        (result.multipleMarksDetected?.size ?: 0).toString(),
                        result.avgConfidence?.toString().orEmpty(),
                        result.minConfidence?.toString().orEmpty()
                    ).map(::escapeCsv)
                } else {
                    emptyList()
                }

                val timeColumns = if (config.includeTimestamps) {
                    listOf(timeFormatter.format(Date(result.scannedAt))).map(::escapeCsv)
                } else {
                    emptyList()
                }

                val answerColumns = if (config.includeAnswers) {
                    (0 until totalQuestions).map { questionIndex ->
                        val answers = result.getAnswersForQuestionIndex(questionIndex)
                        if (answers.isEmpty()) {
                            escapeCsv("")
                        } else {
                            val answerLabel = answers.joinToString("|") { option ->
                                if (option in 0..25) ('A' + option).toString() else option.toString()
                            }
                            escapeCsv(answerLabel)
                        }
                    }
                } else {
                    emptyList()
                }

                appendLine(
                    (
                        alwaysColumns +
                            identityColumns +
                            scoreColumns +
                            timeColumns +
                            answerColumns
                        ).joinToString(",")
                )
            }
        }
    }

    private fun createPdf(
        outputFile: File,
        examEntity: ExamEntity,
        results: List<ScanResultEntity>,
        config: ExportCsvConfig,
        title: String
    ) {
        val document = PdfDocument()
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36
        val lineGap = 18f

        val titlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            textSize = 11f
        }

        var pageIndex = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create())
        var canvas = page.canvas
        var y = margin.toFloat()

        fun ensureSpace(linesRequired: Int = 1) {
            val requiredHeight = linesRequired * lineGap
            if (y + requiredHeight > pageHeight - margin) {
                document.finishPage(page)
                pageIndex += 1
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
                )
                canvas = page.canvas
                y = margin.toFloat()
            }
        }

        fun drawLine(text: String, paint: Paint = bodyPaint) {
            ensureSpace(1)
            canvas.drawText(text, margin.toFloat(), y, paint)
            y += lineGap
        }

        fun drawWrapped(prefix: String, text: String) {
            val maxChars = 85
            if (text.isBlank()) {
                drawLine("$prefix -")
                return
            }
            val chunks = text.chunked(maxChars)
            chunks.forEachIndexed { index, part ->
                if (index == 0) {
                    drawLine("$prefix $part")
                } else {
                    drawLine("  $part")
                }
            }
        }

        drawLine(title, titlePaint)
        drawLine("Generated at: ${timeFormatter.format(Date())}", bodyPaint)
        drawLine("Exam ID: ${examEntity.id}", bodyPaint)
        y += 6f

        results.forEachIndexed { idx, result ->
            val attemptedCount = (result.totalQuestions - result.blankCount).coerceAtLeast(0)
            ensureSpace(8)
            if (idx > 0) {
                drawLine("-------------------------------------------------------------", bodyPaint)
            }
            drawLine("Sheet #${result.id}", headerPaint)
            if (config.includeIdentity) {
                drawLine("Enrollment: ${result.enrollmentNumber.orEmpty()}")
                drawLine("Barcode: ${result.barCode.orEmpty()}")
            }
            if (config.includeScores) {
                drawLine("Score: ${result.score} (${result.scorePercent}%)")
                drawLine(
                    "Counts: Correct ${result.correctCount}, Wrong ${result.wrongCount}, Blank ${result.blankCount}, Attempted $attemptedCount"
                )
                drawLine(
                    "Quality: Low confidence ${result.lowConfidenceQuestions?.size ?: 0}, Multiple marks ${result.multipleMarksDetected?.size ?: 0}"
                )
            }
            if (config.includeTimestamps) {
                drawLine("Scanned at: ${timeFormatter.format(Date(result.scannedAt))}")
            }
            if (config.includeAnswers) {
                val answersText = buildString {
                    for (q in 0 until examEntity.totalQuestions) {
                        val label = q + 1
                        val answers = result.getAnswersForQuestionIndex(q)
                        val value = if (answers.isEmpty()) {
                            "-"
                        } else {
                            answers.joinToString("|") { option ->
                                if (option in 0..25) ('A' + option).toString() else option.toString()
                            }
                        }
                        append("Q$label:$value ")
                    }
                }.trim()
                drawWrapped("Answers:", answersText)
            }
            y += 4f
        }

        document.finishPage(page)
        outputFile.outputStream().use { output ->
            document.writeTo(output)
        }
        document.close()
    }

    private fun sortResults(
        results: List<ScanResultEntity>,
        sortBy: ExportSortBy
    ): List<ScanResultEntity> {
        return when (sortBy) {
            ExportSortBy.SCANNED_AT_DESC -> results.sortedByDescending { it.scannedAt }
            ExportSortBy.SCANNED_AT_ASC -> results.sortedBy { it.scannedAt }
            ExportSortBy.SCORE_DESC -> results.sortedByDescending { it.scorePercent }
            ExportSortBy.SCORE_ASC -> results.sortedBy { it.scorePercent }
            ExportSortBy.ENROLLMENT_ASC -> results.sortedBy { it.enrollmentNumber.orEmpty() }
            ExportSortBy.ENROLLMENT_DESC -> results.sortedByDescending { it.enrollmentNumber.orEmpty() }
        }
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
        return cleaned.ifBlank { "exam_results" }
    }

    private fun escapeCsv(value: String): String {
        val escapedValue = value.replace("\"", "\"\"")
        return "\"$escapedValue\""
    }
}
