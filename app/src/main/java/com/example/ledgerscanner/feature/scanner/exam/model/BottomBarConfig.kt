package com.example.ledgerscanner.feature.scanner.exam.model

data class BottomBarConfig(
    val enabled: Boolean = false,
    val onNext: () -> Unit = {},
    val buttonText: String = "Next"
)