package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.BuildConfig
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericErrorState
import com.example.ledgerscanner.base.ui.components.GenericLoader
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Red500
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

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        message = state.message.ifBlank { "Failed to load templates" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                is UiState.Success -> {
                    TemplateList(
                        templates = state.data.orEmpty(),
                        onSelect = onSelect
                    )
                }

                is UiState.Idle<*> -> Unit
            }
        }
    }
}

@Composable
fun TemplateList(
    templates: List<Template>,
    modifier: Modifier = Modifier,
    onSelect: (Template) -> Unit
) {
    if (templates.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GenericErrorState(message = "No templates found")
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = templates,
            key = { template ->
                template.name ?: "template"
            }
        ) { template ->
            TemplateCard(template = template, onClick = { onSelect(template) })
        }
    }
}

@Composable
private fun TemplateCard(template: Template, onClick: () -> Unit) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .genericClick(showRipple = true) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Grey100)
                    .border(1.dp, Grey200, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${template.name} preview",
                        modifier = Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                } else {
                    TemplatePreviewPlaceholder()
                }
            }

            Text(
                text = template.name ?: "Untitled Template",
                style = AppTypography.text14SemiBold,
                color = Grey700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TemplateMetricPill(
                    label = "$safeQuestionCount Questions",
                    modifier = Modifier.weight(1f)
                )
                TemplateMetricPill(
                    label = "$safeOptionCount Options",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Version ${template.version ?: "1.0"}",
                    style = AppTypography.text12Regular,
                    color = Grey500
                )
                Text(
                    text = "Select",
                    style = AppTypography.text12SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TemplateMetricPill(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    ) {
        Text(
            text = label,
            style = AppTypography.text11Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TemplatePreviewPlaceholder() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = Blue100,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        val startX = w * 0.26f
        val startY = h * 0.27f
        val colGap = w * 0.15f
        val rowGap = h * 0.12f

        repeat(4) { c ->
            repeat(4) { r ->
                val cx = startX + (c * colGap)
                val cy = startY + (r * rowGap)

                drawCircle(
                    color = Color.White,
                    radius = w * 0.02f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = if ((c + r) % 3 == 0) Blue500 else Grey500,
                    radius = w * 0.02f,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f)
                )
            }
        }

        drawRoundRect(
            color = Blue500.copy(alpha = 0.2f),
            topLeft = Offset(w * 0.2f, h * 0.68f),
            size = Size(w * 0.6f, h * 0.08f),
            cornerRadius = CornerRadius(12f, 12f)
        )
    }
}
