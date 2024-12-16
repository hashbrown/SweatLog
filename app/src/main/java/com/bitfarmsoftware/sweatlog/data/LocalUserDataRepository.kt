package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.SampleData.devicesSample
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class LocalUserDataRepository @Inject constructor (datastore: SLDataStore): UserDataRepository {


    override val userData: Flow<UserData> =

        flow{
            val savedDevices = mutableSetOf<Device>()
            devicesSample.map { device ->
                savedDevices.add(device)
            }
            Timber.i("Emitting UserData with ${savedDevices.size} devices")
            emit(UserData(savedDevices = savedDevices))
        }

    override suspend fun saveDevice(device: Device) {
        TODO("Not yet implemented")
    }
}