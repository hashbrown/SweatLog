package com.bitfarmsoftware.sweatlog.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import com.bitfarmsoftware.sweatlog.ui.LogScreen
import com.bitfarmsoftware.sweatlog.ui.LogsScreen
import kotlinx.serialization.Serializable

@Serializable
data object LogsBaseRoute

@Serializable
data object LogsRoute

@Serializable
data class LogRoute(val id: String)


fun NavController.navigateToLogs(navOptions: NavOptions) = navigate(route = LogsRoute, navOptions)
fun NavController.navigateToLog(logId: String, navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = LogRoute(logId)) {
        navOptions()
    }
}


fun NavGraphBuilder.logsSection(
    navController: NavController,
    modifier: Modifier
) {
    navigation<LogsBaseRoute>(startDestination = LogsRoute) {
        composable<LogsRoute> {
            LogsScreen(
                onLogClick = navController::navigateToLog,
                modifier = modifier
            )
        }
        composable<LogRoute> {
            LogScreen(
                modifier = modifier
            )
        }
    }
}