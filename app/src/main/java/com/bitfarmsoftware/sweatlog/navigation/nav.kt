package com.bitfarmsoftware.sweatlog.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bitfarmsoftware.sweatlog.R
import com.bitfarmsoftware.sweatlog.ui.DevicesScreen
import com.bitfarmsoftware.sweatlog.ui.LogsScreen
import com.bitfarmsoftware.sweatlog.ui.SLAppState
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable


@Serializable
data object SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions) = navigate(route = SettingsRoute, navOptions)

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
    val route: KClass<*>,
    val baseRoute: KClass<*> = route,
) {
    LOGS(
        selectedIcon = Icons.Rounded.AvTimer,
        unselectedIcon = Icons.Outlined.AvTimer,
        iconTextId = R.string.feature_logs_title,
        titleTextId = R.string.app_name,
        route = LogsRoute::class,
        baseRoute = LogsBaseRoute::class,
    ),
    SETTINGS(
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        iconTextId = R.string.feature_settings_title,
        titleTextId = R.string.feature_settings_title,
        route = SettingsRoute::class,
    ),
}

/**
 * The navigation graph defined in this file defines the different top level routes. Navigation
 * within each route is handled using state and Back Handlers.
 */
@Composable
fun SLNavHost(
    appState: SLAppState,
    onHandleBackPress: () -> Unit,
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val navController = appState.navController
    NavHost(
        navController = navController,
        startDestination = LogsBaseRoute,
        modifier = modifier,
    ) {
        logsSection(
            navController,
            onHandleBackPress,
            modifier
        )
        composable<SettingsRoute> {
            DevicesScreen(
                onShowSnackbar = onShowSnackbar,
//                onLogClick = TODO(),
                modifier = modifier
            )
        }
    }
}
