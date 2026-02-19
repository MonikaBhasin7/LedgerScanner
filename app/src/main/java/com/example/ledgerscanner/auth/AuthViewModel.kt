package com.example.ledgerscanner.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.network.OperationResult
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.feature.scanner.exam.data.repository.TemplateCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val templateCatalogRepository: TemplateCatalogRepository
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
        val digitsOnly = value.filter { it.isDigit() }.take(10)
        val updated = currentData().copy(phoneNumber = digitsOnly, errorMessage = null)
        _uiState.value = UiState.Success(updated)
    }

    fun onOtpChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(6)
        val updated = currentData().copy(otp = digitsOnly, errorMessage = null)
        _uiState.value = UiState.Success(updated)
    }

    fun requestOtp() {
        val data = currentData()
        val phone = data.phoneNumber.trim()
        if (phone.isBlank()) {
            _uiState.value = UiState.Error(message = "Please enter your phone number", data = data)
            return
        }
        if (phone.length != 10) {
            _uiState.value = UiState.Error(message = "Phone number must be 10 digits", data = data)
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading(message = "Requesting OTP", data = data)
            when (val result = authRepository.requestOtp(phone)) {
                is OperationResult.Success -> {
                    _uiState.value = UiState.Success(data.copy(otpSent = true, errorMessage = null))
                }
                is OperationResult.Error -> {
                    val message = result.message.ifBlank { "Failed to send OTP. Try again." }
                    _uiState.value = UiState.Error(message = message, data = data)
                }
            }
        }
    }

    fun verifyOtp() {
        val data = currentData()
        val phone = data.phoneNumber.trim()
        val otp = data.otp.trim()
        if (phone.isBlank()) {
            _uiState.value = UiState.Error(message = "Please enter your phone number", data = data)
            return
        }
        if (phone.length != 10) {
            _uiState.value = UiState.Error(message = "Phone number must be 10 digits", data = data)
            return
        }
        if (otp.length != 6) {
            _uiState.value = UiState.Error(message = "OTP must be 6 digits", data = data)
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading(message = "Verifying OTP", data = data)
            when (val result = authRepository.verifyOtp(phone, otp)) {
                is OperationResult.Success -> {
                    _uiState.value = UiState.Success(data.copy(errorMessage = null))
                    _loginSuccess.value = true
                    viewModelScope.launch {
                        templateCatalogRepository.syncTemplatesFromServer()
                    }
                }
                is OperationResult.Error -> {
                    val message = result.message.ifBlank { "Invalid OTP. Please try again." }
                    _uiState.value = UiState.Error(message = message, data = data)
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
