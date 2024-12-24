package com.bitfarmsoftware.sweatlog.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitfarmsoftware.sweatlog.R
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.ui.theme.SweatLogTheme


@Composable
fun DevicesScreen(
    onShowSnackbar: suspend (String, String?) -> Boolean,
    modifier: Modifier = Modifier,
    viewModel: DevicesViewModel = hiltViewModel(),
) {

    val myDevicesUiState by viewModel.myDevicesState.collectAsStateWithLifecycle()
    val availableDevicesUiState by viewModel.availableDevicesState.collectAsStateWithLifecycle()

    DevicesScreen(
        viewModel::connectDevice,
        myDevicesUiState,
        availableDevicesUiState
    )
}

@Composable
internal fun DevicesScreen(
    onConnectDeviceClick: (Device) -> Unit,
    savedDevicesState: DevicesUiState,
    availableDevicesState: DevicesUiState
) {
    LazyColumn {
        // Header for Saved Devices
        item {
            Text(
                text = "SAVED DEVICES",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // Items for Saved Devices List
        when (savedDevicesState) {
            is DevicesUiState.Loading -> {
                item {
                    Text(
                        text = "Loading saved devices...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is DevicesUiState.Success -> {
                items((savedDevicesState as DevicesUiState.Success).devices) { device ->
                    DeviceCard(device = device, onConnectDeviceClick)
                }
            }
        }

        // Spacer between lists
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Header for Available Devices
        item {
            Text(
                text = "FOUND DEVICES",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

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
                items((availableDevicesState as DevicesUiState.Success).devices) { device ->
                    DeviceCard(device = device, onConnectDeviceClick)
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onConnectDeviceClick: (Device) -> Unit,
) {
    Row(modifier = Modifier
        .padding(all = 8.dp)
        .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround) {
        Image(
            painter = painterResource(R.drawable.bike_on_car),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                .align(Alignment.CenterVertically)
                // Set image size to 40 dp
                .size(48.dp)
                // Clip image to be shaped as a circle
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the Device is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            label = "AnimationState",
        )

        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = device.name,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,

            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = 1.dp,
                        // surfaceColor color will be changing gradually from primary to surface
                        color = surfaceColor,
                        // animateContentSize will change the Surface size gradually
                        modifier = Modifier
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "Address(${isExpanded}): ${device.address}\n" +
                                    "UUID: ${device.uuid}",
                            modifier = Modifier.padding(all = 4.dp),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        var isConnected by remember { mutableStateOf(false) }

        if (device is BleDeviceResource) {
            val connectionState by device.connected.collectAsState()
            isConnected = connectionState
        }

        val onConnectClick: (Device) -> Unit = when(device) {
            is BleDeviceResource -> {{onConnectDeviceClick(device)}}
            else -> {{}}
        }

        TextButton(
            onClick = {onConnectClick(device)},
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text(
                text = if (isConnected) "CONNECTED" else "CONNECT"
            )
        }
    }
}


@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Preview(
    name = "Light Mode",
    showBackground = true
)
@Composable
fun PreviewDeviceCard() {
    SweatLogTheme {
        Surface {
            DeviceCard(
                SavedDevice(
                    name = "Cycle 1260Sc",
                    address = "123-123",
                    uuid = "123-345-ghjt"
                ),
                {device -> Unit }
            )
        }
    }
}

//@Preview(
//    name = "Light Mode",
//    showBackground = true
//)
//@Composable
//fun PreviewDevicesScreen() {
//    SweatLogTheme {
//        DevicesScreen(SampleData.conversationSample)
//    }
//}
