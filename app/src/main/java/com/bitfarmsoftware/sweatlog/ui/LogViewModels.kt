package com.bitfarmsoftware.sweatlog.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bitfarmsoftware.sweatlog.data.DevicesRepository
import com.bitfarmsoftware.sweatlog.data.LogsRepository
import com.bitfarmsoftware.sweatlog.data.SampleData
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.SweatLog
import com.bitfarmsoftware.sweatlog.model.TrainingData
import com.bitfarmsoftware.sweatlog.model.display
import com.bitfarmsoftware.sweatlog.model.toTrainingData
import com.bitfarmsoftware.sweatlog.navigation.LogRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    logsRepository: LogsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {


    // Backing property to avoid state updates from other classes
//    private val _uiState = MutableStateFlow(LogsUiState.Success(logsSample))
    // The UI collects from this StateFlow to get its state updates
    val uiState: StateFlow<LogsUiState> = flowOf(
        LogsUiState.Success(SampleData.logsSample.map { it.display() }) // Transform all elements at once
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LogsUiState.Loading
    )

    override fun onCleared() {
        super.onCleared()
        Timber.i("LogsViewModel Cleared")
    }

}

sealed interface LogsUiState {
    data object Loading : LogsUiState

    data class Success(
        /**
         * The list of news resources contained in this feed.
         */
        val logs: List<SweatLogView>,
    ) : LogsUiState
}

@HiltViewModel
class LogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logsRepository: LogsRepository,
    devicesRepository: DevicesRepository,
) : ViewModel() {


    val logId = savedStateHandle.toRoute<LogRoute>().id


    // wrap the connectedDevice flow to emit a WaitingToConnect state when no device is yet connected.
    private val deviceState : StateFlow<DeviceState> = devicesRepository.connectedDevice
        .onEach { device -> Timber.i("ViewModel flow connected device ${device.connected}") }
        .map {
            DeviceState.Connected(it)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DeviceState.WaitingToConnect,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LogUiState> = devicesRepository.connectedDevice
                .onEach { device -> Timber.i("ViewModel flow connected device ${device.connected}") }
                .flatMapLatest { device ->
                    devicesRepository.startTraining(device)
                        .map { logData ->
                            Timber.i("ViewModel recvd LogData: $logData")
                            LogUiState.Success(logData.display())
                        }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = LogUiState.Loading,
                )

    /**
     * This should result in 3 states for the UI:
     * 1) NeedsPermissions - regardless of connected device state,
     * we need to give HealthConnect permissions setup.
     *
     * 2) Loading - we have permissions but there is no connected device
     *
     * 3) Success - we have permissions and a connected device,
     * return the latest state of the devices logData stream
     *
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState2: StateFlow<LogUiState> = logsRepository.hasAllPermissions
        .flatMapLatest { hasPerm ->
            if (!hasPerm) {
                flowOf(LogUiState.NeedsPermissions)
            } else {
               deviceState.flatMapLatest { state ->
                   when (state) {
                       is DeviceState.WaitingToConnect ->
                           flowOf(LogUiState.Loading)

                       is DeviceState.Connected ->
                           devicesRepository.startTraining(state.device)
                           .map { logData ->
                               Timber.i("ViewModel recvd LogData: $logData")
                               LogUiState.Success(logData.display())
                           }
                   }
                }

            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogUiState.Loading,
        )

    val requiredPermissions = logsRepository.requiredPermissions
    val permissionLauncher = logsRepository.requestHealthConnectPermissions()


    override fun onCleared() {
        super.onCleared()
        Timber.i("LogViewModel Cleared")
    }

    fun checkPermissions() {
        viewModelScope.launch {
            Timber.i("LogViewModel checking permissions")
            logsRepository.checkPermissions()
        }
    }
}

private sealed interface DeviceState {
    data object WaitingToConnect: DeviceState
    data class Connected(
        val device: BleDeviceResource
    ): DeviceState
}


sealed interface LogUiState {
    data object NeedsPermissions : LogUiState
    data object Loading : LogUiState
    data class Success(
        val logData: LogDataView,
    ) : LogUiState
}

class FakeLogViewModel @Inject constructor(
    ) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LogUiState> = SampleData.bikeData.asFlow()
        .map { it.toTrainingData()}
        .map { logData ->
            Timber.i("ViewModel recvd LogData: $logData")
            LogUiState.Success(logData.display())
        }
        .onEach { delay(1000) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogUiState.Loading,
        )
}

data class SweatLogView(
    val id: String,
    val title: String,
    val logData: LogDataView
)

data class LogDataView(
    val speed: String = "",
    val avgSpeed: String = "",
    val cadence: String = "",
    val avgCadence: String = "",
    val distance: String = "",
    val resistance:String = "",
    val power: String = "",
    val avgPower: String = "",
    val totalEnergy: String = "",
    val energyPerHour: String = "",
    val energyPerMinute: String = "",
    val heartrate: String = "",
    val metabolicEquivalent: String = "",
    val elapsedTime: String = "",
    val remainingTime: String = ""
)

fun SweatLog.display(): SweatLogView {
    return SweatLogView (
        id = this.id,
        title = this.title,
        logData = this.data.display()
    )
}

fun TrainingData.display(): LogDataView {

    return LogDataView (
        speed = this.speed?.display(1)?:0.toString(),
        cadence = this.cadence?.display(1)?:0.toString(),
        distance = this.distance?.display(2)?:0.toString(),
        resistance = this.resistance.toString(),
        power = this.power.toString(),
        avgPower = this.avgPower.toString(),
        totalEnergy = this.totalEnergy.toString(),
        heartrate = this.heartrate.toString(),
        elapsedTime = formatDuration(this.elapsedTime?.toInt()?:0),
    )
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%d:%02d:%02d".format(hours, minutes, secs)
}