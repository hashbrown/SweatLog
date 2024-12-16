package com.bitfarmsoftware.sweatlog.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.bluetooth.BluetoothLe
import com.bitfarmsoftware.sweatlog.model.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class RemoteDevicesDataSource @Inject constructor(
    context: Context
) {

    private val ble = BluetoothLe(context)

    @SuppressLint("MissingPermission")
    fun scanDevices(): Flow<Device> =
        ble.scan()
//            .onEach { Timber.d("Raw scan result: ${it.device.name}") }
            .filter { it.device.name != null }
            .map { result ->
                Device(
                    name = result.device.name!!,
                    address = result.deviceAddress.address,
                    uuid = result.device.id.toString(),
                    isConnected = result.device.bondState == BluetoothDevice.BOND_BONDED
                )
            }

}