package com.bitfarmsoftware.sweatlog.data

import androidx.lifecycle.viewModelScope
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.TrainingData
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.service.BleDeviceService
import com.bitfarmsoftware.sweatlog.service.BleDeviceServiceProvider
import com.bitfarmsoftware.sweatlog.ui.LogUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedDevicesRepository @Inject constructor (
    private val userDataRepository: UserDataRepository,
    private val serviceProvider: BleDeviceServiceProvider
) : DevicesRepository {

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    init {
        Timber.i("Init CombinedDevicesRepo")
        scope.async {
            val service = serviceProvider.getService()
            Timber.i("Updating services flows")
            _availableDevices.value = service.availableDevices
            _connectedDevice.value = service.connectedDevice
            service
        }
    }

//    private val deferred = scope.async {
//        val service = serviceProvider.getService()
//        Timber.i("Updating services flows")
//        _availableDevices.value = service.availableDevices
//        _connectedDevice.value = service.connectedDevice
//    }

    override val savedDevices: Flow<List<SavedDevice>> =
        userDataRepository.userData
            .map { it.savedDevices.toList() }

    // Setup MutableStateFlows so that we can update once the service is available from the provider
    @OptIn(ExperimentalCoroutinesApi::class)
    override val availableDevices: Flow<BleDeviceResource> get() = _availableDevices.flatMapLatest { it }
    private val _availableDevices = MutableStateFlow<Flow<BleDeviceResource>>(emptyFlow())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val connectedDevice: Flow<BleDeviceResource> get() = _connectedDevice.flatMapLatest { it }
    private val _connectedDevice = MutableStateFlow<Flow<BleDeviceResource>>(emptyFlow())



    override suspend fun scanForDevices() {
        serviceProvider.getService().scanDevices()
    }

    override suspend fun stopDeviceScan() {
        serviceProvider.getService().stopScan()
    }

    override suspend fun connectDevice(device: BleDeviceResource) {
        serviceProvider.getService().connectDevice(device)
    }

    override suspend fun startTraining(device: BleDeviceResource):Flow<TrainingData> {
        return serviceProvider.getService().startTrainingSession(device)
    }

    override suspend fun saveDevice(device: Device) {
        userDataRepository.saveDevice(device)
    }

}

