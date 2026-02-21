package com.example.ledgerscanner.feature.scanner.statistics.viewModel

import com.example.ledgerscanner.base.errors.ErrorMessages
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.repo.ScanResultRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportResultsViewModel @Inject constructor(
    private val scanResultRepository: ScanResultRepository
) : ViewModel() {

    enum class PreviewFormat {
        CSV,
        PDF_COMBINED,
        PDF_INDIVIDUAL
    }

    private val _sheetCount = MutableStateFlow(0)
    val sheetCount: StateFlow<Int> = _sheetCount.asStateFlow()

    private val _exportState = MutableStateFlow<UiState<ExportedFilePayload>>(UiState.Idle())
    val exportState: StateFlow<UiState<ExportedFilePayload>> = _exportState.asStateFlow()
    private val _previewState = MutableStateFlow<UiState<ScanResultRepository.ExportPreviewPayload>>(UiState.Idle())
    val previewState: StateFlow<UiState<ScanResultRepository.ExportPreviewPayload>> = _previewState.asStateFlow()

    fun loadSheetCount(examId: Int) {
        viewModelScope.launch {
            runCatching {
                scanResultRepository.getCountByExamIdOnce(examId)
            }.onSuccess {
                _sheetCount.value = it
            }
        }
    }

    fun exportCsv(examEntity: ExamEntity, config: ScanResultRepository.ExportCsvConfig) {
        viewModelScope.launch {
            _exportState.value = UiState.Loading()
            try {
                val exportedFile = scanResultRepository.exportResultsCsv(examEntity, config)
                _exportState.value = UiState.Success(
                    ExportedFilePayload(
                        path = exportedFile.absolutePath,
                        examName = examEntity.examName,
                        mimeType = "text/csv",
                        suggestedExtension = "csv"
                    )
                )
            } catch (_: CancellationException) {
                // ignore cancellation
            } catch (e: Exception) {
                _exportState.value = UiState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: ErrorMessages.EXPORT_FAILED
                )
            }
        }
    }

    fun exportPdfCombined(examEntity: ExamEntity, config: ScanResultRepository.ExportCsvConfig) {
        viewModelScope.launch {
            _exportState.value = UiState.Loading()
            try {
                val exportedFile = scanResultRepository.exportResultsPdfCombined(examEntity, config)
                _exportState.value = UiState.Success(
                    ExportedFilePayload(
                        path = exportedFile.absolutePath,
                        examName = examEntity.examName,
                        mimeType = "application/pdf",
                        suggestedExtension = "pdf"
                    )
                )
            } catch (_: CancellationException) {
                // ignore cancellation
            } catch (e: Exception) {
                _exportState.value = UiState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: ErrorMessages.EXPORT_FAILED
                )
            }
        }
    }

    fun exportPdfIndividual(examEntity: ExamEntity, config: ScanResultRepository.ExportCsvConfig) {
        viewModelScope.launch {
            _exportState.value = UiState.Loading()
            try {
                val exportedFile = scanResultRepository.exportResultsPdfIndividualZip(examEntity, config)
                _exportState.value = UiState.Success(
                    ExportedFilePayload(
                        path = exportedFile.absolutePath,
                        examName = examEntity.examName,
                        mimeType = "application/zip",
                        suggestedExtension = "zip"
                    )
                )
            } catch (_: CancellationException) {
                // ignore cancellation
            } catch (e: Exception) {
                _exportState.value = UiState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: ErrorMessages.EXPORT_FAILED
                )
            }
        }
    }

    fun loadPreview(
        examEntity: ExamEntity,
        config: ScanResultRepository.ExportCsvConfig,
        format: PreviewFormat
    ) {
        viewModelScope.launch {
            _previewState.value = UiState.Loading()
            try {
                val repoFormat = when (format) {
                    PreviewFormat.CSV -> ScanResultRepository.ExportOutputFormat.CSV
                    PreviewFormat.PDF_COMBINED -> ScanResultRepository.ExportOutputFormat.PDF_COMBINED
                    PreviewFormat.PDF_INDIVIDUAL -> ScanResultRepository.ExportOutputFormat.PDF_INDIVIDUAL
                }
                val preview = scanResultRepository.getExportPreview(examEntity, config, repoFormat)
                _previewState.value = UiState.Success(preview)
            } catch (_: CancellationException) {
                // ignore cancellation
            } catch (e: Exception) {
                _previewState.value = UiState.Error(
                    e.message?.takeIf { it.isNotBlank() } ?: ErrorMessages.EXPORT_FAILED
                )
            }
        }
    }

    fun resetExportState() {
        _exportState.value = UiState.Idle()
    }

    fun resetPreviewState() {
        _previewState.value = UiState.Idle()
    }
}

data class ExportedFilePayload(
    val path: String,
    val examName: String,
    val mimeType: String,
    val suggestedExtension: String
)
