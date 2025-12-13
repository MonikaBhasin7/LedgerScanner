package com.example.ledgerscanner.feature.scanner.exam.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton

@Composable
fun SaveAndNextBarWidget(
    onNext: () -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        GenericButton(
            text = "Next",
            onClick = onNext,
            type = ButtonType.PRIMARY,
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }
}