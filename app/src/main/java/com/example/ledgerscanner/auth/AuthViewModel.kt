package com.example.ledgerscanner.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<LoginUiState>>(UiState.Success(LoginUiState()))
    val uiState: StateFlow<UiState<LoginUiState>> = _uiState

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private fun currentData(): LoginUiState {
        return when (val state = _uiState.value) {
            is UiState.Success -> state.data ?: LoginUiState()
            is UiState.Loading -> state.data ?: LoginUiState()
            is UiState.Error -> state.data ?: LoginUiState(errorMessage = state.message)
            is UiState.Idle -> LoginUiState()
        }
    }

    fun onPhoneChange(value: String) {
        val updated = currentData().copy(phoneNumber = value, errorMessage = null)
        _uiState.value = UiState.Success(updated)
    }

    fun onOtpChange(value: String) {
        val updated = currentData().copy(otp = value, errorMessage = null)
        _uiState.value = UiState.Success(updated)
    }

    fun requestOtp() {
        val data = currentData()
        val phone = data.phoneNumber.trim()
        if (phone.isBlank()) {
            _uiState.value = UiState.Error(message = "Phone number is required", data = data)
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading(message = "Requesting OTP", data = data)
            when (val result = authRepository.requestOtp(phone)) {
                is OperationResult.Success -> {
                    _uiState.value = UiState.Success(data.copy(otpSent = true, errorMessage = null))
                }
                is OperationResult.Error -> {
                    _uiState.value = UiState.Error(message = result.message, data = data)
                }
            }
        }
    }

    fun verifyOtp() {
        val data = currentData()
        val phone = data.phoneNumber.trim()
        val otp = data.otp.trim()
        if (phone.isBlank() || otp.isBlank()) {
            _uiState.value = UiState.Error(message = "Phone and OTP are required", data = data)
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading(message = "Verifying OTP", data = data)
            when (val result = authRepository.verifyOtp(phone, otp)) {
                is OperationResult.Success -> {
                    _uiState.value = UiState.Success(data.copy(errorMessage = null))
                    _loginSuccess.value = true
                }
                is OperationResult.Error -> {
                    _uiState.value = UiState.Error(message = result.message, data = data)
                }
            }
        }
    }
}

data class LoginUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val errorMessage: String? = null
)
