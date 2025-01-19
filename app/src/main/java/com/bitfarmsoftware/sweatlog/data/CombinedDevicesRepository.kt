package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.WorkoutData
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.service.BleDeviceService
import com.bitfarmsoftware.sweatlog.service.BleDeviceServiceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedDevicesRepository @Inject constructor (
    private val userDataRepository: UserDataRepository,
    private val serviceProvider: BleDeviceServiceProvider
) : DevicesRepository {

    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private lateinit var serviceResult: Deferred<BleDeviceService>

    init {
        Timber.i("Init CombinedDevicesRepo")
        serviceResult = scope.async {
            val service = serviceProvider.getService()
            Timber.i("Updating services flows")
            _availableDevices.value = service.availableDevices
            _connectedDevice.value = service.connectedDevice
            service
        }
    }

    override val savedDevices: Flow<List<SavedDevice>> =
        userDataRepository.userData
            .map { it.savedDevices.toList() }

    // Setup MutableStateFlows so that we can update once the service is available from the provider
    @OptIn(ExperimentalCoroutinesApi::class)
    override val availableDevices: Flow<BleDeviceResource>  get() = _availableDevices.flatMapLatest { it }
    private val _availableDevices = MutableStateFlow<Flow<BleDeviceResource>>(emptyFlow())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val connectedDevice: Flow<BleDeviceResource> get() = _connectedDevice.flatMapLatest { it }
    private val _connectedDevice = MutableStateFlow<Flow<BleDeviceResource>>(emptyFlow())



    override suspend fun scanForDevices() {
        serviceResult.await().scanDevices()
    }

    override suspend fun stopDeviceScan() {
        serviceResult.await().stopScan()
    }

    override suspend fun connectDevice(device: BleDeviceResource) {
        serviceResult.await().connectDevice(device)
    }

//    override suspend fun startWorkout(device: BleDeviceResource):Flow<WorkoutData> {
//        return serviceProvider.getService().startTrainingSession(device)
//    }

    override suspend fun saveDevice(device: Device) {
        userDataRepository.saveDevice(device)
    }

}

