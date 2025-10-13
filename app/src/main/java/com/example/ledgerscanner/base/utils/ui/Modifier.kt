package com.example.ledgerscanner.base.utils.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.genericClick(
    showRipple: Boolean = false,
    onClick: () -> Unit
): Modifier {
    return if (showRipple) {
        this.clickable(onClick = onClick)
    } else {
        this.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    }
}