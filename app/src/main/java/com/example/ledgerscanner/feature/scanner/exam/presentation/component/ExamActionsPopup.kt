package com.example.ledgerscanner.feature.scanner.exam.presentation.component

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
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.presentation.viewmodel.ExamListViewModel

@Composable
fun ExamActionsPopup(
    expanded: Boolean,
    examEntity: ExamEntity,
    viewModel: ExamListViewModel,
    actions: ExamActionPopupConfig,
    onActionClick: (ExamAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val menuWidth = 196.dp

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 0.dp, y = 4.dp),
        modifier = Modifier
            .width(menuWidth)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Black.copy(alpha = 0.12f)
            )
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Grey100, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp)
    ) {
        actions.menuItems.forEachIndexed { index, action ->
            // Divider before dangerous actions
            if (action.isDangerous && index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                    thickness = 1.dp,
                    color = Grey200
                )
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = action.label,
                        style = AppTypography.text13Medium,
                        color = if (action.isDangerous) Red500 else Black
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                            modifier = Modifier.size(17.dp)
                        )
                    }
                },
                onClick = {
                    onActionClick(action)
                    onDismiss()
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
            )
        }
    }
}
