package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.OmrTemplateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class CreateTemplateViewModel @Inject constructor(
) : ViewModel() {
    private val _pickedBitmap = MutableStateFlow<Bitmap?>(null)
    val pickedBitmap: StateFlow<Bitmap?> = _pickedBitmap

    private val _templateResult = MutableStateFlow<OmrTemplateResult?>(null)
    val templateResult: StateFlow<OmrTemplateResult?> = _templateResult

    fun setBitmap(b: Bitmap?) {
        _pickedBitmap.value = b
    }


    fun setTemplateResult(r: OmrTemplateResult?) {
        _templateResult.value = r
    }
}