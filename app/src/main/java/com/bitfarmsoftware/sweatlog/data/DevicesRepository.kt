package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.TrainingData
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

interface DevicesRepository {

    val savedDevices: Flow<List<SavedDevice>>

    val availableDevices: Flow<BleDeviceResource>

    val connectedDevice: Flow<BleDeviceResource>

    suspend fun scanForDevices()
    suspend fun stopDeviceScan()
    suspend fun connectDevice(device: BleDeviceResource)
    suspend fun startTraining(device: BleDeviceResource): Flow<TrainingData>
    suspend fun saveDevice(device: Device)
}