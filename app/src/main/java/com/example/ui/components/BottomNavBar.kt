package com.example.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppTab

@Composable
fun BottomNavBar(currentTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        modifier = Modifier.height(64.dp)
    ) {
        val tabs = listOf(
            Triple(AppTab.HOME, "Home", Icons.Default.Home),
            Triple(AppTab.CHART, "Chart", Icons.Default.ShowChart),
            Triple(AppTab.PRACTICE, "Practice", Icons.Default.School),
            Triple(AppTab.REPLAY, "Replay", Icons.Default.Replay),
            Triple(AppTab.JOURNAL, "Journal", Icons.Default.Book),
            Triple(AppTab.LEARN, "Learn", Icons.Default.MenuBook)
        )

        tabs.forEach { (tab, label, icon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
