package com.example.ledgerscanner.feature.scanner.exam.model

import androidx.compose.ui.graphics.vector.ImageVector

data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
