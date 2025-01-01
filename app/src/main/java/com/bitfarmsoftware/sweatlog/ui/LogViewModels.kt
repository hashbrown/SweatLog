package com.bitfarmsoftware.sweatlog.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bitfarmsoftware.sweatlog.data.DevicesRepository
import com.bitfarmsoftware.sweatlog.data.LogsRepository
import com.bitfarmsoftware.sweatlog.data.SampleData
import com.bitfarmsoftware.sweatlog.data.SampleData.logsSample
import com.bitfarmsoftware.sweatlog.model.TrainingData
import com.bitfarmsoftware.sweatlog.model.SweatLog
import com.bitfarmsoftware.sweatlog.model.display
import com.bitfarmsoftware.sweatlog.model.toTrainingData
import com.bitfarmsoftware.sweatlog.navigation.LogRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    logsRepository: LogsRepository,
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

    override fun onCleared() {
        super.onCleared()
        Timber.i("LogViewModel Cleared")
    }
}


sealed interface LogUiState {
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

fun TrainingData.display(): LogDataView {

    return LogDataView (
        speed = this.speed?.display(1)?:0.toString(),
        cadence = this.cadence?.display(1)?:0.toString(),
        distance = this.distance?.display(2)?:0.toString(),
        resistance = this.resistance.toString(),
        power = this.power.toString(),
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