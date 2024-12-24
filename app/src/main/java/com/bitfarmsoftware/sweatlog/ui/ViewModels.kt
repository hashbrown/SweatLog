package com.bitfarmsoftware.sweatlog.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bitfarmsoftware.sweatlog.data.DevicesRepository
import com.bitfarmsoftware.sweatlog.data.LogsRepository
import com.bitfarmsoftware.sweatlog.data.SampleData.logsSample
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.LogData
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.SweatLog
import com.bitfarmsoftware.sweatlog.navigation.LogRoute
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Backing property to avoid state updates from other classes
    private val _uiState = MutableStateFlow(LogsUiState.Success(logsSample))
    // The UI collects from this StateFlow to get its state updates
    val uiState : StateFlow<LogsUiState> = _uiState

}

sealed interface LogsUiState {
    data object Loading : LogsUiState

    data class Success(
        /**
         * The list of news resources contained in this feed.
         */
        val logs: List<SweatLog>,
    ) : LogsUiState
}

@HiltViewModel
class LogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    logsRepository: LogsRepository,
    devicesRepository: DevicesRepository,
) : ViewModel() {

    val logId = savedStateHandle.toRoute<LogRoute>().id

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LogUiState> = devicesRepository.connectedDevice
        .onEach { device -> Timber.i("ViewModel flow connected device ${device.connected}") }
        .flatMapLatest { device ->
            logsRepository.observeLogData(device)
                .map { logData ->
                    Timber.i("ViewModel recvd LogData: $logData")
                    LogUiState.Success(logData)
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogUiState.Loading,
        )

    override fun onCleared() {
        super.onCleared()
        Timber.i("LogViewModel Cleared")
    }
}


sealed interface LogUiState {
    data object Loading : LogUiState

    data class Success(
        val logData: LogData,
    ) : LogUiState
}



@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val devicesRepository: DevicesRepository,
    savedStateHandle: SavedStateHandle,
    private val application: Application
) : ViewModel() {

    init {
        scanForDevices()
    }

    private val appLifeCycleOwner = ProcessLifecycleOwner.get()

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
                appLifeCycleOwner.lifecycleScope.launch {
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



