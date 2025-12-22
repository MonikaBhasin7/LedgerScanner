package com.example.ledgerscanner.feature.scanner.exam.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.ExamListViewModel

@Composable
fun ExamActionsPopup(
    expanded: Boolean,
    examEntity: ExamEntity,
    viewModel: ExamListViewModel,
    actions: ExamActionPopupConfig,
    onActionClick: (ExamAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val menuWidth = 220.dp
    val cardPadding = 16.dp

    val offsetX = screenWidth - menuWidth - cardPadding - 50.dp - 24.dp

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = offsetX, y = 8.dp),
        modifier = Modifier
            .width(menuWidth)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Black.copy(alpha = 0.15f)
            )
            .background(White, RoundedCornerShape(16.dp))
            .border(1.dp, Grey100, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        // Top padding
        Spacer(modifier = Modifier.height(2.dp))

        actions.menuItems.forEachIndexed { index, action ->
            // Divider before dangerous actions
            if (action.isDangerous && index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    thickness = 1.dp,
                    color = Grey200
                )
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = action.label,
                        style = AppTypography.body2Medium,
                        color = if (action.isDangerous) Red500 else Black
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (action.isDangerous)
                                    Red500.copy(alpha = 0.1f)
                                else
                                    Blue100,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = if (action.isDangerous) Red500 else Blue500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = {
                    onActionClick(action)
                    onDismiss()
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Bottom padding
        Spacer(modifier = Modifier.height(2.dp))
    }
}