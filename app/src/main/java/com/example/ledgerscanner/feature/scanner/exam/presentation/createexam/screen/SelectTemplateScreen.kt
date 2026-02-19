package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.BuildConfig
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericErrorState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel.TemplateSelectionViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@Composable
fun SelectTemplateScreen(
    onSelect: (Template) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TemplateSelectionViewModel = hiltViewModel()
    val templateData by viewModel.templateData.collectAsState()
    var selectedTemplateName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (val state = templateData) {
                is UiState.Loading -> GenericLoader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                is UiState.Error -> GenericErrorState(
                    message = state.message.ifBlank { "Failed to load templates" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                is UiState.Success -> {
                    val templates = state.data.orEmpty()
                    if (templates.isEmpty()) {
                        GenericErrorState(
                            message = "No templates found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        LaunchedEffect(templates) {
                            if (selectedTemplateName.isNullOrBlank()) {
                                selectedTemplateName = templates.first().name
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = templates,
                                key = { template -> template.name ?: "template" }
                            ) { template ->
                                TemplateCard(
                                    template = template,
                                    selected = template.name == selectedTemplateName,
                                    onClick = { selectedTemplateName = template.name }
                                )
                            }
                        }
                    }
                }

                is UiState.Idle<*> -> Unit
            }
        }

        GenericButton(
            text = "Use Template",
            onClick = {
                val templates = (templateData as? UiState.Success<List<Template>>)?.data.orEmpty()
                val selected = templates.firstOrNull { it.name == selectedTemplateName }
                    ?: templates.firstOrNull()
                selected?.let(onSelect)
            },
            enabled = (templateData as? UiState.Success<List<Template>>)?.data.orEmpty().isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val safeQuestionCount = runCatching { template.getTotalQuestions() }.getOrDefault(0)
    val safeOptionCount = runCatching { template.options_per_question }.getOrDefault(0)
    val imageUrl = template.imageUrl?.takeIf { it.isNotBlank() }
    val imageModel = imageUrl?.let {
        when {
            it.startsWith("http://") || it.startsWith("https://") -> it
            it.startsWith("/") -> BuildConfig.BASE_URL.trimEnd('/') + it
            else -> Uri.parse("file:///android_asset/$it")
        }
    }

    val cardBorder = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .genericClick(showRipple = true) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = cardBorder,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Grey100)
                    .border(1.dp, Grey200, RoundedCornerShape(14.dp))
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${template.name} preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            Text(
                text = template.name ?: "Untitled Template",
                style = AppTypography.text15Bold,
                color = Grey700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TemplateMetricPill(label = "$safeQuestionCount Questions")
                TemplateMetricPill(label = "$safeOptionCount Options")

                Spacer(modifier = Modifier.weight(1f))
                if (selected) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(3.dp)
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun TemplateMetricPill(label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Text(
            text = label,
            style = AppTypography.text12Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
