package com.example.ledgerscanner.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.auth.AuthViewModel
import com.example.ledgerscanner.auth.LoginUiState
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.White

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

    val phoneCount = data.phoneNumber.length
    val otpCount = data.otp.length
    val isPhoneValid = phoneCount == 10
    val isOtpValid = otpCount == 6

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF6F8FF),
            Color(0xFFEAF0FF),
            Color(0xFFFFFFFF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LedgerScanner",
                style = AppTypography.h2Bold,
                color = Blue500
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Secure login with OTP",
                style = AppTypography.body2Regular,
                color = Grey600
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (data.otpSent) "Verify OTP" else "Login",
                        style = AppTypography.h3Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (data.otpSent) {
                            "Enter the 6-digit OTP sent to your phone"
                        } else {
                            "Enter your 10-digit phone number to receive an OTP"
                        },
                        style = AppTypography.body3Regular,
                        color = Grey600
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GenericTextField(
                        value = data.phoneNumber,
                        onValueChange = viewModel::onPhoneChange,
                        label = "Phone number",
                        placeholder = "9876543210",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = Grey600
                            )
                        },
                        supportingText = {
                            Text(
                                text = "$phoneCount/10 digits",
                                style = AppTypography.body4Medium,
                                color = if (phoneCount == 10) Color(0xFF2E7D32) else Grey600
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    if (data.otpSent) {
                        Spacer(modifier = Modifier.height(16.dp))
                        GenericTextField(
                            value = data.otp,
                            onValueChange = viewModel::onOtpChange,
                            label = "OTP",
                            placeholder = "123456",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Grey600
                                )
                            },
                            supportingText = {
                                Text(
                                    text = "$otpCount/6 digits",
                                    style = AppTypography.body4Medium,
                                    color = if (otpCount == 6) Color(0xFF2E7D32) else Grey600
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFD32F2F))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage,
                                    style = AppTypography.body3Regular,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!data.otpSent) {
                        GenericButton(
                            text = if (isLoading) "Sending OTP..." else "Send OTP",
                            onClick = { viewModel.requestOtp() },
                            modifier = Modifier.fillMaxWidth(),
                            type = ButtonType.PRIMARY,
                            enabled = !isLoading && isPhoneValid
                        )
                    } else {
                        GenericButton(
                            text = if (isLoading) "Verifying..." else "Verify OTP",
                            onClick = { viewModel.verifyOtp() },
                            modifier = Modifier.fillMaxWidth(),
                            type = ButtonType.PRIMARY,
                            enabled = !isLoading && isOtpValid
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GenericButton(
                            text = "Resend OTP",
                            onClick = { viewModel.requestOtp() },
                            modifier = Modifier.fillMaxWidth(),
                            type = ButtonType.SECONDARY,
                            enabled = !isLoading
                        )
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Blue500,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "By continuing, you agree to our Terms & Privacy Policy.",
                style = AppTypography.body4Medium.copy(fontWeight = FontWeight.Normal),
                color = Grey600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

    }
}
