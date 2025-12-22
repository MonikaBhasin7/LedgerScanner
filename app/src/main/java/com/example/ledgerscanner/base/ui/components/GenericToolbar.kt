package com.example.ledgerscanner.base.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200

sealed class ToolbarAction {
    // Icon only
    data class Icon(
        val icon: ImageVector,
        val contentDescription: String? = null,
        val onClick: () -> Unit
    ) : ToolbarAction()

    // Text only
    data class Text(
        val text: String,
        val onClick: () -> Unit
    ) : ToolbarAction()

    // Icon + Text
    data class IconText(
        val icon: ImageVector,
        val text: String,
        val onClick: () -> Unit
    ) : ToolbarAction()

    // Custom widget
    data class Custom(
        val content: @Composable () -> Unit
    ) : ToolbarAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericToolbar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: List<ToolbarAction> = emptyList()
) {
    Column {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Grey100)
                .padding(end = 16.dp),
            title = {
                androidx.compose.material3.Text(
                    text = title,
                    style = AppTypography.h3Bold,
                    color = Black
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Grey100,
            ),
            navigationIcon = {
                onBackClick?.let {
                    Box(
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(start = 8.dp, end = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color = Blue100)
                            .padding(8.dp),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Sharp.ArrowBack,
                            contentDescription = "Back",
                            tint = Blue500
                        )
                    }
                }
            },
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { action ->
                        when (action) {
                            is ToolbarAction.Icon -> {
                                ToolbarIconAction(
                                    icon = action.icon,
                                    contentDescription = action.contentDescription,
                                    onClick = action.onClick
                                )
                            }

                            is ToolbarAction.Text -> {
                                ToolbarTextAction(
                                    text = action.text,
                                    onClick = action.onClick
                                )
                            }

                            is ToolbarAction.IconText -> {
                                ToolbarIconTextAction(
                                    icon = action.icon,
                                    text = action.text,
                                    onClick = action.onClick
                                )
                            }

                            is ToolbarAction.Custom -> {
                                action.content()
                            }
                        }
                    }
                }
            }
        )
        HorizontalDivider(
            color = Grey200,
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun ToolbarIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(color = Blue100)
            .padding(8.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Blue500
        )
    }
}

@Composable
private fun ToolbarTextAction(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(color = Blue100)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = AppTypography.label3SemiBold,
            color = Blue500
        )
    }
}

@Composable
private fun ToolbarIconTextAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(color = Blue100)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Blue500,
                modifier = Modifier.size(18.dp)
            )
            androidx.compose.material3.Text(
                text = text,
                style = AppTypography.label3SemiBold,
                color = Blue500
            )
        }
    }
}