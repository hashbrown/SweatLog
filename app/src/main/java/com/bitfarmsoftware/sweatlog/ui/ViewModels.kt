package com.bitfarmsoftware.sweatlog.ui

import androidx.compose.ui.tooling.preview.Devices
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bitfarmsoftware.sweatlog.data.DevicesRepository
import com.bitfarmsoftware.sweatlog.data.SampleData.devicesSample
import com.bitfarmsoftware.sweatlog.data.SampleData.logsSample
import com.bitfarmsoftware.sweatlog.data.UserDataRepository
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SweatLog
import com.bitfarmsoftware.sweatlog.navigation.LogsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
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
class DevicesViewModel @Inject constructor(
    devicesRepository: DevicesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {


    val myDevicesState: StateFlow<DevicesUiState> =
        devicesRepository.savedDevices
            .map(DevicesUiState::Success)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DevicesUiState.Loading,
            )

    val availableDevicesState: StateFlow<DevicesUiState> =
        devicesRepository.availableDevices
//            .onEach { Timber.d("Device Available: ${it.name}") }
            .scan(emptySet<Device>()) { currentSet, newDevice ->
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

}


sealed interface DevicesUiState {
    data object Loading : DevicesUiState

    data class Success(
        val devices: List<Device>,
    ) : DevicesUiState
}



