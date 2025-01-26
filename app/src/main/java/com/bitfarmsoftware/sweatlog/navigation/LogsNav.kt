package com.bitfarmsoftware.sweatlog.navigation

import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.bitfarmsoftware.sweatlog.ui.LogSummaryScreen
import com.bitfarmsoftware.sweatlog.ui.WorkoutScreen
import com.bitfarmsoftware.sweatlog.ui.LogsScreen
import kotlinx.serialization.Serializable

@Serializable
data object LogsBaseRoute

@Serializable
data object LogsRoute

@Serializable
data class LogRoute(val id: String)

@Serializable
data object WorkoutRoute


fun NavController.navigateToLogs(navOptions: NavOptions) = navigate(route = LogsRoute, navOptions)
fun NavController.navigateToLog(logId: String, navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = LogRoute(logId)) {
        navOptions()
    }
}
fun NavController.navigateToWorkout(navOptions: NavOptionsBuilder.() -> Unit = {}){
    navigate(route = WorkoutRoute) {
        navOptions()
    }
}


fun NavGraphBuilder.logsSection(
    navController: NavController,
    onHandleBackPress: () -> Unit,
    modifier: Modifier
) {
    navigation<LogsBaseRoute>(startDestination = LogsRoute) {
        composable<LogsRoute> {
            LogsScreen(
                onLogClick = navController::navigateToLog,
                startWorkout = navController::navigateToWorkout,
                onHandleBackPress = onHandleBackPress,
                modifier = modifier
            )
        }
        composable<LogRoute> {
            LogSummaryScreen(
                modifier = modifier
            )
        }
        composable<WorkoutRoute> {
            WorkoutScreen(
                onFinishWorkout = navController::popBackStack,
                modifier = modifier
            )
        }
    }
}