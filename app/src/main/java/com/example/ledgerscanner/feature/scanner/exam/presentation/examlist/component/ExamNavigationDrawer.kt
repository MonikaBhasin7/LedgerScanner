package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.auth.MemberRole
import com.example.ledgerscanner.auth.TokenStore
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.White

enum class DrawerDestination {
    Exams,
    CreateExam,
    CreateTemplate,
    Logout
}

@Composable
fun ExamNavigationDrawerContent(
    selectedDestination: DrawerDestination,
    activeExamCount: Int,
    completedExamCount: Int,
    onDestinationSelected: (DrawerDestination) -> Unit
) {
    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context.applicationContext) }
    val memberName = remember(tokenStore) { tokenStore.getMemberName() }
    val memberPhone = remember { tokenStore.getMemberPhone() }
    val memberRole = remember { tokenStore.getMemberRole() }
    val sectionGap = 12.dp
    val itemGap = 4.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Blue500),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (memberName ?: "L").take(1).uppercase(),
                            style = AppTypography.text16Bold,
                            color = White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = memberName ?: "LedgerScanner User",
                            style = AppTypography.text15SemiBold,
                            color = White
                        )
                        Text(
                            text = memberPhone ?: memberRole?.name ?: "Exam workspace",
                            style = AppTypography.text12Regular,
                            color = White.copy(alpha = 0.92f)
                        )
                    }
                }
                Text(
                    text = "Exam management and scanning workspace",
                    style = AppTypography.text12Regular,
                    color = White.copy(alpha = 0.95f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        DrawerSectionTitle("Browse")
        Spacer(modifier = Modifier.height(itemGap))

        DrawerNavigationEntry(
            title = "Exams",
            subtitle = "Active: $activeExamCount  •  Completed: $completedExamCount",
            icon = Icons.Outlined.ListAlt,
            selected = selectedDestination == DrawerDestination.Exams,
            onClick = { onDestinationSelected(DrawerDestination.Exams) }
        )

        Spacer(modifier = Modifier.height(sectionGap))
        HorizontalDivider(color = Grey200)
        Spacer(modifier = Modifier.height(sectionGap))

        DrawerSectionTitle("Create")
        Spacer(modifier = Modifier.height(itemGap))

        DrawerNavigationEntry(
            title = "Create Exam",
            subtitle = "Set basic info, answer key, and marking",
            icon = Icons.Outlined.PostAdd,
            selected = selectedDestination == DrawerDestination.CreateExam,
            onClick = { onDestinationSelected(DrawerDestination.CreateExam) }
        )
        if (memberRole?.isAdmin() == true)
            DrawerNavigationEntry(
                title = "Create Template",
                subtitle = "Build new OMR layout templates",
                icon = Icons.Outlined.Description,
                selected = selectedDestination == DrawerDestination.CreateTemplate,
                onClick = { onDestinationSelected(DrawerDestination.CreateTemplate) }
            )

        Spacer(modifier = Modifier.height(sectionGap))
        HorizontalDivider(color = Grey200)
        Spacer(modifier = Modifier.height(sectionGap))
        DrawerSectionTitle("Account")
        Spacer(modifier = Modifier.height(itemGap))
        DrawerNavigationEntry(
            title = "Logout",
            subtitle = "Sign out from current device",
            icon = Icons.Outlined.Logout,
            selected = selectedDestination == DrawerDestination.Logout,
            onClick = { onDestinationSelected(DrawerDestination.Logout) }
        )
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        style = AppTypography.text12SemiBold,
        color = Grey600,
        modifier = Modifier.padding(start = 12.dp)
    )
}

@Composable
private fun DrawerNavigationEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        modifier = Modifier.padding(vertical = 2.dp),
        label = {
            Column {
                Text(text = title, style = AppTypography.text14SemiBold)
                Text(
                    text = subtitle,
                    style = AppTypography.text11Regular,
                    color = if (selected) Blue500 else Grey600
                )
            }
        },
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Blue100,
            selectedIconColor = Blue500,
            selectedTextColor = Blue500,
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = Grey800,
            unselectedTextColor = Grey900
        )
    )
}
