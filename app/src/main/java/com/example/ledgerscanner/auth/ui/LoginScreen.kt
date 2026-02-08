package com.example.ledgerscanner.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.auth.AuthViewModel
import com.example.ledgerscanner.auth.LoginUiState
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey600

@Composable
fun LoginScreen(
    viewModel: AuthViewModel
) {
    val state by viewModel.uiState.collectAsState()

    val data: LoginUiState = when (state) {
        is UiState.Success -> (state as UiState.Success<LoginUiState>).data ?: LoginUiState()
        is UiState.Loading -> (state as UiState.Loading<LoginUiState>).data ?: LoginUiState()
        is UiState.Error -> (state as UiState.Error<LoginUiState>).data ?: LoginUiState()
        is UiState.Idle -> LoginUiState()
    }

    val isLoading = state is UiState.Loading
    val errorMessage = when (state) {
        is UiState.Error -> (state as UiState.Error<LoginUiState>).message
        else -> data.errorMessage
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Login",
            style = AppTypography.h1Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your phone number to receive an OTP",
            style = AppTypography.body2Regular,
            color = Grey600
        )

        Spacer(modifier = Modifier.height(24.dp))

        GenericTextField(
            value = data.phoneNumber,
            onValueChange = viewModel::onPhoneChange,
            label = "Phone number",
            placeholder = "+919876543210",
            modifier = Modifier.fillMaxWidth()
        )

        if (data.otpSent) {
            Spacer(modifier = Modifier.height(16.dp))
            GenericTextField(
                value = data.otp,
                onValueChange = viewModel::onOtpChange,
                label = "OTP",
                placeholder = "123456",
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = AppTypography.body3Regular,
                color = androidx.compose.ui.graphics.Color.Red,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            GenericLoader()
        } else {
            if (!data.otpSent) {
                GenericButton(
                    text = "Send OTP",
                    onClick = { viewModel.requestOtp() },
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.PRIMARY
                )
            } else {
                GenericButton(
                    text = "Verify OTP",
                    onClick = { viewModel.verifyOtp() },
                    modifier = Modifier.fillMaxWidth(),
                    type = ButtonType.PRIMARY
                )
            }
        }
    }
}
