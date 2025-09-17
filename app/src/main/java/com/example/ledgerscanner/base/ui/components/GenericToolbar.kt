package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericToolbar(title: String, onBackClick: (() -> Unit)? = null) {
    return Column {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Grey100)
                .padding(end = 16.dp),
            title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = Black) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Grey100,
            ),
            navigationIcon = {
                onBackClick?.let {
                    Box(
                        modifier = Modifier
                            .clickable {
                                onBackClick()
                            }
                            .padding(start = 8.dp, end = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color = Blue100)
                            .padding(12.dp),
                    ) {
                        Icon(
                            Icons.Sharp.ArrowBack,
                            contentDescription = "Search Icon",
                            tint = Blue500
                        )
                    }
                }
            }
        )
        Divider(
            color = Grey200,  // or any color you want
            thickness = 2.dp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}
