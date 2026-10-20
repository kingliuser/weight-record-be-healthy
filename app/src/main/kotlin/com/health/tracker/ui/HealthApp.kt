package com.health.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.health.tracker.ui.achievement.AchievementScreen
import com.health.tracker.ui.monthly.MonthlyScreen
import com.health.tracker.ui.settings.SettingsScreen
import com.health.tracker.ui.today.TodayScreen
import com.health.tracker.ui.weight.WeightScreen

@Composable
fun HealthApp() {
    val root: RootViewModel = viewModel(factory = containerFactory { RootViewModel(it.profileRepository) })
    val hasProfile by root.hasProfile.collectAsState()

    when (hasProfile) {
        null -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "减重记录",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        false -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { HealthTopBar(pageTitle = "建档", onOpenSettings = null) },
        ) { insets ->
            SettingsScreen(modifier = Modifier.fillMaxSize().padding(insets))
        }

        true -> TabbedApp()
    }
}

@Composable
private fun TabbedApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val activeRoute = backStackEntry?.destination?.route ?: BottomTab.TODAY.route
    val onSettings = activeRoute == ROUTE_SETTINGS
    val pageTitle = BottomTab.entries.firstOrNull { it.route == activeRoute }?.label ?: "设置"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HealthTopBar(
                pageTitle = pageTitle,
                showBack = onSettings,
                onBack = { navController.popBackStack() },
                onOpenSettings = if (onSettings) {
                    null
                } else {
                    { navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true } }
                },
            )
        },
        bottomBar = {
            if (!onSettings) {
                HealthBottomBar(activeRoute) { tab ->
                    if (tab.route != activeRoute) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        },
    ) { insets ->
        NavHost(
            navController = navController,
            startDestination = BottomTab.TODAY.route,
            modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            composable(BottomTab.TODAY.route) { TodayScreen() }
            composable(BottomTab.WEIGHT.route) { WeightScreen() }
            composable(BottomTab.MONTHLY.route) { MonthlyScreen() }
            composable(BottomTab.ACHIEVEMENT.route) { AchievementScreen() }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(firstRun = false, onDone = { navController.popBackStack() })
            }
        }
    }
}

private const val ROUTE_SETTINGS = "settings"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthTopBar(
    pageTitle: String,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = {},
) {
    LargeTopAppBar(
        title = {
            Column {
                Text(
                    text = "减重记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = pageTitle, style = MaterialTheme.typography.headlineMedium)
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
        actions = {
            onOpenSettings?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun HealthBottomBar(activeRoute: String, onSelect: (BottomTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == activeRoute,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, textAlign = TextAlign.Center) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "今日", Icons.Filled.Home),
    WEIGHT("weight", "体重", Icons.Filled.Person),
    MONTHLY("monthly", "月度", Icons.Filled.DateRange),
    ACHIEVEMENT("achievement", "成就", Icons.Filled.Star),
}
