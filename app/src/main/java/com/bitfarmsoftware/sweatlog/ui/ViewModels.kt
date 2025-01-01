package com.bitfarmsoftware.sweatlog.ui

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.bitfarmsoftware.sweatlog.data.DevicesRepository
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val devicesRepository: DevicesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {



    init {
        Timber.i("Loading DevicesViewModel")
        scanForDevices()
    }

    val myDevicesState: StateFlow<DevicesUiState> =
        devicesRepository.savedDevices
            .map(DevicesUiState::Success)
            .take(2)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DevicesUiState.Loading,
            )

    val availableDevicesState: StateFlow<DevicesUiState> =
        devicesRepository.availableDevices
//            .onEach { Timber.d("Device Available: ${it.name}") }
            .scan(emptySet<BleDeviceResource>()) { currentSet, newDevice ->
                currentSet + newDevice
            }
            .map { devices ->
                DevicesUiState.Success(devices.toList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DevicesUiState.Loading,
            )

    fun scanForDevices() {
        viewModelScope.launch {
            devicesRepository.scanForDevices()
        }
    }

    fun connectDevice(device: Device) {
        when (device) {
            is BleDeviceResource ->
                // Use lifecycleScope for the Application instead of viewModelScope
                viewModelScope.launch {
                    devicesRepository.connectDevice(device)
                }
            is SavedDevice -> Timber.i("Ignoring request to connect to invalid device: ${device.name}")
        }

    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("DevicesViewModel Cleared")
    }

}


sealed interface DevicesUiState {
    data object Loading : DevicesUiState

    data class Success(
        val devices: List<Device>,
    ) : DevicesUiState
}



