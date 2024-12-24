package com.bitfarmsoftware.sweatlog.data.ble

import android.annotation.SuppressLint
import android.content.Context
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.GattClientScope
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class RemoteDevicesDataSource @Inject constructor(
    context: Context
) {

    private val ble = BluetoothLe(context)

    // Backing property to avoid state updates from other classes
    private val _connectedDevices = MutableSharedFlow<BleDeviceResource>(replay = 1)
    // The UI collects from this StateFlow to get its state updates
    val connectedDevices: SharedFlow<BleDeviceResource> = _connectedDevices

    // Backing property to avoid state updates from other classes
    private val _availableDevices = MutableSharedFlow<BleDeviceResource>(replay = 1)
    // The UI collects from this StateFlow to get its state updates
    val availableDevices: SharedFlow<BleDeviceResource> = _availableDevices

    private var scanJob: Job? = null

    @SuppressLint("MissingPermission")
    suspend fun scanDevices() {
        val coroutineScope = CoroutineScope(coroutineContext)
        scanJob = coroutineScope.launch {
            ble.scan()
//                .onEach { Timber.d("Raw scan result: ${it.device.name}") }
                .filter { it.device.name != null }
                .map { result ->
                    BleDeviceResource(
                        bleDevice = result.device,
                        name = result.device.name!!,
                        address = result.deviceAddress.address,
                        uuid = result.device.id.toString(),
                    )
                }
                .collect { device ->
                    _availableDevices.emit(device) // Emit scan result
                }
        }

        coroutineScope.launch {
            delay(10_000)
            Timber.i("Cancelling Scan Job")
            scanJob?.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectDevice(device: BleDeviceResource) {
        val coroutineScope = CoroutineScope(coroutineContext)
        ble.connectGatt(device.bleDevice) {
            device.connected(this, coroutineScope)
            _connectedDevices.emit(device)

            awaitCancellation()
        }
    }


}