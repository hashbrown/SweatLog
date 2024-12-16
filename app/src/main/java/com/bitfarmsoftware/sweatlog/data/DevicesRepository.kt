package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.UserData
import kotlinx.coroutines.flow.Flow

interface DevicesRepository {

    val savedDevices: Flow<List<Device>>

    val availableDevices: Flow<Device>

    suspend fun saveDevice(device: Device)
}