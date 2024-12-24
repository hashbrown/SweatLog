package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import kotlinx.coroutines.flow.Flow

interface DevicesRepository {

    val savedDevices: Flow<List<SavedDevice>>

    val availableDevices: Flow<BleDeviceResource>

    val connectedDevice: Flow<BleDeviceResource>

    suspend fun scanForDevices()

    suspend fun saveDevice(device: Device)

    suspend fun connectDevice(device: BleDeviceResource)
}