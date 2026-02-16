package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericErrorState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel.TemplateSelectionViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@Composable
fun SelectTemplateScreen(
    onSelect: (Template) -> Unit,
) {
    val viewModel: TemplateSelectionViewModel = hiltViewModel()
    val templateData by viewModel.templateData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Template",
                    style = AppTypography.title2Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider()

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()

        ) {
            when (val state = templateData) {
                is UiState.Loading -> {
                    GenericLoader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                is UiState.Error -> {
                    GenericErrorState(
                        message = "Failed to load templates",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                is UiState.Success -> {
                    TemplateGrid(
                        templates = state.data ?: listOf(),
                        onSelect = { template ->
                            onSelect(template)
                        }
                    )
                }

                is UiState.Idle<*> -> {}
            }
        }
    }
}

@Composable
fun TemplateGrid(
    templates: List<Template>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    onSelect: (Template) -> Unit
) {
    if (templates.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GenericErrorState(message = "No templates found")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(
            items = templates,
            key = { index, template ->
                template.name ?: "${template.name}::$index"
            }
        ) { _, template ->
            TemplateCard(
                template = template,
                onClick = { onSelect(template) }
            )
        }
    }
}

@Composable
private fun TemplateCard(template: Template, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f) // Make cards more square-ish
            .genericClick(showRipple = true) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Grey100
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = template.name ?: "",
                style = AppTypography.h4Medium,
                color = Grey500,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}