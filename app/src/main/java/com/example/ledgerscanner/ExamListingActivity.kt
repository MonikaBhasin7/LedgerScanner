package com.example.ledgerscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.GenericToolbar
import com.example.ledgerscanner.ui.theme.Blue100
import com.example.ledgerscanner.ui.theme.Blue500
import com.example.ledgerscanner.ui.theme.Grey200
import com.example.ledgerscanner.ui.theme.Grey50
import com.example.ledgerscanner.ui.theme.Grey500
import com.example.ledgerscanner.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.ui.theme.White

class ExamListingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    topBar = {
                        GenericToolbar(title = "Exams")
                    },
                    content = { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            SearchBar()
                            FilterChips()
                        }
                    }

                )
            }
        }
    }

    @Composable
    private fun FilterChips() {
        val filters = listOf("All", "Processing", "Draft", "Completed")
        var selectedIndex by remember { mutableIntStateOf(0) }
        return Row(
            modifier = Modifier
                .padding(start = 12.dp, top = 12.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            for ((i, filter) in filters.withIndex()) {
                FilterChip(
                    selected = (selectedIndex == i),
                    onClick = {
                        selectedIndex = i
                    },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Blue100,
                        selectedContainerColor = Blue500,
                        labelColor = Blue500,
                        selectedLabelColor = White,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = null,
                    modifier = Modifier.padding(end = 8.dp)
                )

            }
        }
    }


    @Composable
    private fun SearchBar() {
        var text by remember { mutableStateOf(TextFieldValue("")) }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Search by exam name") },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = "Search Icon")
            },
            trailingIcon = {
                if (text.text.isNotEmpty())
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                    )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Grey50,
                focusedContainerColor = White,
                focusedLeadingIconColor = Grey500,
                unfocusedLeadingIconColor = Grey500,
                focusedIndicatorColor = Grey200,
                unfocusedIndicatorColor = Grey200,
                focusedTrailingIconColor = Grey500,
                unfocusedTrailingIconColor = Grey500,
            )
        )
    }
}
