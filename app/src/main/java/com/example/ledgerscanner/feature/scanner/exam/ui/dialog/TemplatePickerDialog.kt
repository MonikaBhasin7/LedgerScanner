package com.example.ledgerscanner.feature.scanner.exam.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.utils.AssetUtils

@Composable
fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        items = AssetUtils.listJsonAssets(context)
        if (selected == null && items.isNotEmpty()) selected = items.first()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose OMR Template") },
        text = {
            LazyColumn {
                items(items) { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = name },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == name),
                            onClick = { selected = name }
                        )
                        Text(
                            name.removeSuffix(".json")
                                .replace(
                                    "_",
                                    " "
                                )
                                .replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let(onSelect) }
            ) { Text("Use") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}