package com.example.ledgerscanner.feature.scanner.exam.presentation.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.feature.scanner.exam.data.repository.TemplateSelectionRepository
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateSelectionViewModel @Inject constructor(
    private val repository: TemplateSelectionRepository
) : ViewModel() {

    private val _templateData = MutableStateFlow<UiState<List<Template>>>(UiState.Loading())
    val templateData: StateFlow<UiState<List<Template>>> = _templateData

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        if (_templateData.value is UiState.Success) return

        _templateData.value = UiState.Loading()
        viewModelScope.launch {
            val result = repository.loadTemplatesFromAssets()
            _templateData.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Failed to load templates") }
            )
        }
    }
}
