package com.bitfarmsoftware.sweatlog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.bitfarmsoftware.sweatlog.navigation.SLNavHost
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import timber.log.Timber
import kotlin.reflect.KClass


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SLApp(
    appState: SLAppState,
    modifier: Modifier = Modifier,
) {

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
    )

    when {
        permissionsState.allPermissionsGranted -> {
            SLAppView(appState, modifier)
        }

        permissionsState.shouldShowRationale -> {
            Text("Bluetooth permissions are required to find devices.")
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Request Permissions")
            }
        }

        else -> {
            LaunchedEffect(permissionsState.allPermissionsGranted) {
                permissionsState.launchMultiplePermissionRequest() }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SLAppView(
    appState: SLAppState,
    modifier: Modifier = Modifier,
) {
    val currentDestination = appState.currentDestination
    val snackbarHostState = remember { SnackbarHostState() }


    NavigationSuiteScaffold(
        navigationSuiteItems = {
            appState.topLevelDestinations.forEach { destination ->
                val selected = currentDestination
                    .isRouteInHierarchy(destination.baseRoute)
                item(
                    selected = selected,
                    onClick = { appState.navigateToTopLevelDestination(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(destination.iconTextId)) },
                    modifier = Modifier
                )
            }
        },

        ) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Simple Scaffold Screen") },
                    navigationIcon = {
                        IconButton(onClick = { /* "Open nav drawer" */ }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Localized description")
                        }
                    }
                )
            },
//            floatingActionButtonPosition = FabPosition. End,
//            floatingActionButton = {
//                ExtendedFloatingActionButton(onClick = { /* fab click handler */ })
//                { Text("Inc") }
//            },
        ) { padding ->
            Column(
                Modifier
//                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
//                // Show the top app bar on top level destinations.
//                val destination = appState.currentTopLevelDestination
//                var shouldShowTopAppBar = false
//
//                if (destination != null) {
//                    shouldShowTopAppBar = true
//                    NiaTopAppBar(
//                        titleRes = destination.titleTextId,
//                        navigationIcon = NiaIcons.Search,
//                        navigationIconContentDescription = stringResource(
//                            id = settingsR.string.feature_settings_top_app_bar_navigation_icon_description,
//                        ),
//                        actionIcon = NiaIcons.Settings,
//                        actionIconContentDescription = stringResource(
//                            id = settingsR.string.feature_settings_top_app_bar_action_icon_description,
//                        ),
//                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
//                            containerColor = Color.Transparent,
//                        ),
//                        onActionClick = { onTopAppBarActionClick() },
//                        onNavigationClick = { appState.navigateToSearch() },
//                    )
//                }

                Box(
                ) {
                    SLNavHost(
                        appState = appState,
                        onShowSnackbar = { message, action ->
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = action,
                                duration = Short,
                            ) == ActionPerformed
                        },
                        modifier = modifier
                    )
                }

                // TODO: We may want to add padding or spacer when the snackbar is shown so that
                //  content doesn't display behind it.
            }
        }
    }

}

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } ?: false


