package com.challenger.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.challenger.app.R
import com.challenger.app.ui.editor.EditorScreen
import com.challenger.app.ui.list.ChallengesScreen
import com.challenger.app.ui.settings.SettingsScreen
import com.challenger.app.ui.stats.StatsScreen
import com.challenger.app.ui.today.TodayScreen

object Routes {
    const val TODAY = "today"
    const val CHALLENGES = "challenges"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{id}?preset={preset}"
    const val STATS = "stats/{id}"

    fun editor(id: Long = 0L, preset: String? = null): String =
        "editor/" + id + "?preset=" + (preset ?: "")

    fun stats(id: Long): String = "stats/" + id
}

private data class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, R.string.tab_today, Icons.Filled.Today),
    Tab(Routes.CHALLENGES, R.string.tab_challenges, Icons.AutoMirrored.Outlined.FormatListBulleted),
    Tab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings)
)

@Composable
fun ChallengerNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = stringResource(tab.labelRes))
                            },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    onOpenChallenge = { navController.navigate(Routes.stats(it)) },
                    onEditChallenge = { navController.navigate(Routes.editor(it)) },
                    onAddChallenge = { navController.navigate(Routes.CHALLENGES) }
                )
            }
            composable(Routes.CHALLENGES) {
                ChallengesScreen(
                    onOpenChallenge = { navController.navigate(Routes.stats(it)) },
                    onCreate = { preset -> navController.navigate(Routes.editor(0L, preset)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("preset") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                EditorScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.STATS,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) {
                StatsScreen(
                    onEdit = { navController.navigate(Routes.editor(it)) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
