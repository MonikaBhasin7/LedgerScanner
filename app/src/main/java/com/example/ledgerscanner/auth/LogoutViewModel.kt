package com.example.ledgerscanner.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle())
    val logoutState: StateFlow<UiState<Unit>> = _logoutState

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = UiState.Loading(message = "Logging out")
            when (val result = authRepository.logout()) {
                is OperationResult.Success -> _logoutState.value = UiState.Success(Unit)
                is OperationResult.Error -> _logoutState.value = UiState.Error(result.message)
            }
        }
    }

    fun reset() {
        _logoutState.value = UiState.Idle()
    }
}
