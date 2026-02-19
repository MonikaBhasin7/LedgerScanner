package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.feature.scanner.exam.data.repository.TemplateSelectionRepository
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private var observeJob: Job? = null
    private var syncJob: Job? = null

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        if (observeJob == null) {
            observeJob = viewModelScope.launch {
                repository.observeTemplates().collect { templates ->
                    if (templates.isEmpty()) {
                        if (_templateData.value !is UiState.Error) {
                            _templateData.value = UiState.Loading(message = "Loading templates")
                        }
                    } else {
                        _templateData.value = UiState.Success(templates)
                    }
                }
            }
        }

        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            when (val result = repository.refreshTemplates()) {
                is OperationResult.Success -> {
                    val current = (_templateData.value as? UiState.Success)?.data.orEmpty()
                    if (current.isEmpty()) {
                        _templateData.value = UiState.Error("No templates found")
                    }
                }

                is OperationResult.Error -> {
                    val current = (_templateData.value as? UiState.Success)?.data.orEmpty()
                    if (current.isEmpty()) {
                        _templateData.value =
                            UiState.Error(result.message.ifBlank { "Failed to load templates" })
                    }
                }
            }
        }
    }
}
