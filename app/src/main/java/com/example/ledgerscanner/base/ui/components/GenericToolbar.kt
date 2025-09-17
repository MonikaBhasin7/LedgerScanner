package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericToolbar(title: String) {
    return Column {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Grey100,
            )
        )
        Divider(
            color = Grey200,  // or any color you want
            thickness = 2.dp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}