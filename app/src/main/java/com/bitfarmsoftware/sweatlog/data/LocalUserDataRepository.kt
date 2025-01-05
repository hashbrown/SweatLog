package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.SampleData.devicesSample
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalUserDataRepository @Inject constructor (private val datastore: SLPreferencesDataSource): UserDataRepository {

    init {
        Timber.i("Initializing LocalUserDataRepo")
    }

    private val savedDevices = mutableSetOf<SavedDevice>()

    override val userData: Flow<UserData> =
        datastore.userData

//        flow{
//            devicesSample.take(1)
//                .map { device ->
//                savedDevices.add(device)
//            }
//            Timber.i("Emitting UserData with ${savedDevices.size} devices")
//            delay(2000)
//            emit(UserData(savedDevices = savedDevices))
//        }

    override suspend fun saveDevice(device: Device) {
        val saved = SavedDevice(device.name, device.address, device.uuid)
        savedDevices.add(saved)
        datastore.saveUserData(UserData(savedDevices = savedDevices))
    }
}