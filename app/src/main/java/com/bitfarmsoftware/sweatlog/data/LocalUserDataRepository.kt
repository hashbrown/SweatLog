package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.SampleData.devicesSample
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class LocalUserDataRepository @Inject constructor (datastore: SLDataStore): UserDataRepository {

    private val savedDevices = mutableSetOf<SavedDevice>()

    override val userData: Flow<UserData> =

        flow{
            devicesSample.map { device ->
                savedDevices.add(device)
            }.first()
            Timber.i("Emitting UserData with ${savedDevices.size} devices")
            emit(UserData(savedDevices = savedDevices))
        }

    override suspend fun saveDevice(device: Device) {
        val saved = SavedDevice(device.name, device.address, device.uuid)
        savedDevices.add(saved)
    }
}