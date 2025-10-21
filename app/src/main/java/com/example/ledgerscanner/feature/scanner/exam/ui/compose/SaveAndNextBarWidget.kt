package com.example.ledgerscanner.feature.scanner.exam.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton

@Composable
fun SaveAndNextBarWidget(
    onSaveDraft: () -> Unit,
    onNext: () -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        GenericButton(
            text = "Save Draft",
            onClick = onSaveDraft,
            type = ButtonType.SECONDARY,
            modifier = Modifier.weight(1f),
            enabled = enabled
        )

        GenericButton(
            text = "Next",
            onClick = onNext,
            type = ButtonType.PRIMARY,
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }
}