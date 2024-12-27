package com.bitfarmsoftware.sweatlog.data.ble

import android.annotation.SuppressLint
import android.content.Context
import androidx.bluetooth.BluetoothLe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

    private val _scanning = MutableSharedFlow<Boolean>(replay = 1)
    val scanning: SharedFlow<Boolean> = _scanning

    private var scanJob: Job? = null

    @SuppressLint("MissingPermission")
    suspend fun scanDevices() {
        val coroutineScope = CoroutineScope(coroutineContext)
        scanJob = coroutineScope.launch {
            _scanning.emit(true)
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
            delay(30_000)
            Timber.i("Cancelling Scan Job")
            scanJob?.cancel()
            _scanning.emit(false)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connectDevice(device: BleDeviceResource) {
        val coroutineScope = CoroutineScope(coroutineContext)
        ble.connectGatt(device.bleDevice) {
            device.connected(this, coroutineScope)
            _connectedDevices.emit(device)
            try {
                awaitCancellation()
            } finally {
                Timber.i("connectDevice coroutine is cancelled")
                device.disconnected()
            }
        }
    }


}