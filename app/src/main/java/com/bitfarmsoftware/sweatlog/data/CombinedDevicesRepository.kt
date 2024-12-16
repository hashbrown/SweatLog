package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Collections.list
import javax.inject.Inject

class CombinedDevicesRepository @Inject constructor (
    userDataRepository: UserDataRepository,
    remoteDevicesDataSource: RemoteDevicesDataSource
) : DevicesRepository {

    override val savedDevices: Flow<List<Device>> =
            userDataRepository.userData
                .map { it.savedDevices.toList() }


    override val availableDevices: Flow<Device> =
        remoteDevicesDataSource.scanDevices()


    override suspend fun saveDevice(device: Device) {
        TODO("Not yet implemented")
    }

}