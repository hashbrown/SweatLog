package com.bitfarmsoftware.sweatlog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitfarmsoftware.sweatlog.model.Device

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDeviceDialog(
    onDismiss: () -> Unit = {},
    viewModel: DevicesViewModel = hiltViewModel(),) {

    val configuration = LocalConfiguration.current
    viewModel.scanForDevices()

    BasicAlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = configuration.screenWidthDp.dp - 80.dp),
        onDismissRequest = { onDismiss() }) {

        // Apply Material Design-specified shape, background tone, padding, and spacing.
        Surface(
//            modifier = Modifier.wrapContentWidth().wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge, // dialog default shape
            tonalElevation = AlertDialogDefaults.TonalElevation // dialog default tonal elevation
        ) {
            FindDevicesScreen(viewModel)
        }
    }

}


@Composable
fun FindDevicesScreen(
    viewModel: DevicesViewModel
) {

    val availableDevicesUiState by viewModel.availableDevicesState.collectAsStateWithLifecycle()

    FindDevicesScreen(
        viewModel::connectDevice,
        availableDevicesUiState
    )
}

@Composable
internal fun FindDevicesScreen(
    onConnectDeviceClick: (Device) -> Unit,
    availableDevicesState: DevicesUiState
) {
    LazyColumn {

        // Items for Available Devices List
        when (availableDevicesState) {
            is DevicesUiState.Loading -> {
                item {
                    Text(
                        text = "Searching for devices...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is DevicesUiState.Success -> {
                items((availableDevicesState).devices) { device ->
                    Text(
                        text = device.name,
                        modifier = Modifier
                            .clickable(
                                onClick = {onConnectDeviceClick(device)}
                            )
                            .padding(8.dp)
                    )
                    DeviceCard(device = device, onConnectDeviceClick)
                }
            }
        }
    }
}

