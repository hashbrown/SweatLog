package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.ble.RemoteDevicesDataSource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
class CombinedDevicesRepository @Inject constructor (
    private val userDataRepository: UserDataRepository,
    private val remoteDevicesDataSource: RemoteDevicesDataSource
) : DevicesRepository {

    override val savedDevices: Flow<List<SavedDevice>> =
            userDataRepository.userData
                .map { it.savedDevices.toList() }

    override val availableDevices: Flow<BleDeviceResource> =
        remoteDevicesDataSource.availableDevices

    override val connectedDevice: Flow<BleDeviceResource> =
        remoteDevicesDataSource.connectedDevices

    override suspend fun scanForDevices() {
        remoteDevicesDataSource.scanDevices()
    }

    override suspend fun saveDevice(device: Device) {
        userDataRepository.saveDevice(device)
    }

    override suspend fun connectDevice(device: BleDeviceResource) {
        remoteDevicesDataSource.connectDevice(device)
    }


}

