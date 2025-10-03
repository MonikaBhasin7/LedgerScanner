package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import androidx.lifecycle.ViewModel
import com.example.ledgerscanner.feature.scanner.exam.repo.ExamRepository
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OmrScannerViewModel @Inject constructor() : ViewModel() {
    private val _omrImageProcessResult = MutableStateFlow<OmrImageProcessResult?>(null)
    val omrImageProcessResult = _omrImageProcessResult.asStateFlow()

    fun setOmrImageProcessResult(result: OmrImageProcessResult) {
        _omrImageProcessResult.value = result
    }
}