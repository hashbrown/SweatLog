package com.bitfarmsoftware.sweatlog.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    startBLEService: () -> Unit,
    exitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val permissions = listOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT

    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions + android.Manifest.permission.POST_NOTIFICATIONS
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    when {
        permissionsState.allPermissionsGranted -> {
            // Start BLEDeviceService when permissions are granted
            LaunchedEffect(Unit) {
                startBLEService()
            }
            SLAppView(appState, exitApp, modifier)
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
    exitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDestination = appState.currentDestination
    val snackbarHostState = remember { SnackbarHostState() }

    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val savedDevices = appState.userData.savedDevices
    showSettingsDialog = (savedDevices.isEmpty())

    var showConfirmExitDialog by remember { mutableStateOf(false) }

    if (showConfirmExitDialog) {
        ConfirmDialog(
            onConfirm = { exitApp() },
            onDismiss = { showConfirmExitDialog = false }
        )
    }

    Timber.i("Composing SLAppView, savedDevices=${savedDevices.isNotEmpty()}, showSettingDialog=${showSettingsDialog}")


    if (showSettingsDialog) {
        FindDeviceDialog(
            onDismiss = { showSettingsDialog = false },
        )
    }


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
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
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
                    title = { Text("Sweat Logs") }
                )
            },

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

                Box(
                ) {
                    SLNavHost(
                        appState = appState,
                        onHandleBackPress = { showConfirmExitDialog = true },
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


