package com.bitfarmsoftware.sweatlog.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bitfarmsoftware.sweatlog.data.DevicesRepository
import com.bitfarmsoftware.sweatlog.data.LogsRepository
import com.bitfarmsoftware.sweatlog.data.SampleData
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.db.SweatLog
import com.bitfarmsoftware.sweatlog.model.RepoResource
//import com.bitfarmsoftware.sweatlog.model.SweatLog
import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
import com.bitfarmsoftware.sweatlog.data.db.WorkoutSegmentSummary
import com.bitfarmsoftware.sweatlog.model.asWorkoutData
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    logsRepository: LogsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // The UI collects from this StateFlow to get its state updates
    val uiState: StateFlow<LogsUiState> =
        logsRepository.sweatLogs.map { repoResource ->
            when (repoResource) {
                is RepoResource.Loading -> LogsUiState.Loading
                is RepoResource.None -> LogsUiState.Success(emptyList())
                is RepoResource.Error -> LogsUiState.Success(emptyList())
                is RepoResource.Success ->
                    LogsUiState.Success(
                        repoResource.data.map {
                            it.display()
                        }.toList()
                    )
            }
        }
        .stateIn(
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
class LogSummaryViewModel @Inject constructor(
    logsRepository: LogsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val logId = savedStateHandle.toRoute<LogRoute>().id

    val uiState: StateFlow<LogSummaryUiState> =
        logsRepository.getSweatLog(logId.toInt()).map{ repoResource ->
            when (repoResource) {
                is RepoResource.Loading -> LogSummaryUiState.Loading
                is RepoResource.None -> LogSummaryUiState.Error
                is RepoResource.Error -> LogSummaryUiState.Error
                is RepoResource.Success ->
                    LogSummaryUiState.Success(
                        repoResource.data.display()
                    )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LogSummaryUiState.Loading
        )


    override fun onCleared() {
        super.onCleared()
        Timber.i("LogSummaryViewModel Cleared")
    }

}

sealed interface LogSummaryUiState {
    data object Loading : LogSummaryUiState
    data object Error : LogSummaryUiState
    data class Success(
        /**
         * The list of news resources contained in this feed.
         */
        val sweatLog: SweatLogView,
    ) : LogSummaryUiState
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logsRepository: LogsRepository,
    devicesRepository: DevicesRepository,
) : ViewModel() {

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
    val uiState: StateFlow<WorkoutUiState> = logsRepository.hasAllPermissions
        .flatMapLatest { hasPerm ->
            if (!hasPerm) {
                flowOf(WorkoutUiState.NeedsPermissions)
            } else {
               deviceState.flatMapLatest { state ->
                   when (state) {
                       is DeviceState.WaitingToConnect ->
                           flowOf(WorkoutUiState.Loading)
                       is DeviceState.Connected ->
                           logsRepository.startNewWorkout(state.device)
                               .map { repoResource ->
                                   when (repoResource) {
                                       is RepoResource.Loading -> WorkoutUiState.Loading
                                       is RepoResource.None -> WorkoutUiState.Loading
                                       is RepoResource.Error -> WorkoutUiState.Loading
                                       is RepoResource.Success ->
                                           WorkoutUiState.Success(repoResource.data.display())

                                   }
                               }
                   }
                }

            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutUiState.Loading,
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

    fun finishWorkout() {
        viewModelScope.launch {
            logsRepository.finishWorkout()
        }
    }
}

private sealed interface DeviceState {
    data object WaitingToConnect: DeviceState
    data class Connected(
        val device: BleDeviceResource
    ): DeviceState
}


sealed interface WorkoutUiState {
    data object NeedsPermissions : WorkoutUiState
    data object Loading : WorkoutUiState
    data class Success(
        val logData: LogDataView,
    ) : WorkoutUiState
}

class FakeWorkoutViewModel @Inject constructor(
    ) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WorkoutUiState> = SampleData.bikeData.asFlow()
        .map { it.toTrainingData()}
        .map { bikeData ->
            WorkoutUiState.Success(bikeData.asWorkoutData().display())
        }
        .onEach { delay(1000) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutUiState.Loading,
        )
}

data class SweatLogView(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val summary: LogDataView,
    val segments: List<WorkoutSegmentView>
)

data class WorkoutSegmentView(
    val startTime: String,
    val endTime: String,
    val data: LogDataView,
)

data class LogDataView(
    val speed: String = "",
    val avgSpeed: String = "",
    val minSpeed: String = "",
    val maxSpeed: String = "",
    val cadence: String = "",
    val avgCadence: String = "",
    val minCadence: String = "",
    val maxCadence: String = "",
    val distance: String = "",
    val resistance:String = "",
    val power: String = "",
    val avgPower: String = "",
    val minPower: String = "",
    val maxPower: String = "",
    val totalEnergy: String = "",
    val heartrate: String = "",
    val avgHeartrate: String = "",
    val minHeartrate: String = "",
    val maxHeartrate: String = "",
    val elapsedTime: String = "",
)

fun SweatLog.display(): SweatLogView {
    return SweatLogView (
        id = this.id.toString(),
        title = this.title,
        startTime = this.workoutData.startTime.display(),
        endTime = this.workoutData.endTime.display(),
        summary = this.workoutData.summary.display(),
        segments = this.workoutData.segments.map {
            it.display()
        }
    )
}

fun WorkoutSegmentSummary.display(): WorkoutSegmentView {
    return WorkoutSegmentView(
        startTime = this.startTime.display(),
        endTime = this.endTime.display(),
        data = this.data.display(),
    )
}

fun WorkoutData.display(): LogDataView {

    return LogDataView (
        speed = this.speed?.display(1)?:0.toString(),
        avgSpeed = this.avgSpeed?.display(1)?:0.toString(),
        minSpeed = this.minSpeed?.display(1)?:0.toString(),
        maxSpeed = this.maxSpeed?.display(1)?:0.toString(),
        cadence = this.cadence?.display(1)?:0.toString(),
        avgCadence = this.avgCadence?.display(1)?:0.toString(),
        minCadence = this.minCadence?.display(1)?:0.toString(),
        maxCadence = this.maxCadence?.display(1)?:0.toString(),
        distance = this.distance?.display(1)?:0.toString(),
        resistance = this.resistance.toString(),
        power = this.power.toString(),
        avgPower = this.avgPower.toString(),
        minPower = this.minPower.toString(),
        maxPower = this.maxPower.toString(),
        totalEnergy = this.totalEnergy.toString(),
        heartrate = this.heartrate.toString(),
        avgHeartrate = this.avgHeartrate.toString(),
        minHeartrate = this.minHeartrate.toString(),
        maxHeartrate = this.maxHeartrate.toString(),
        elapsedTime = formatDuration(this.elapsedTime?:0),
    )
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return "%d:%02d:%02d".format(hours, minutes, secs)
}

fun Instant.display(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mma")
    val zonedNow = ZonedDateTime.ofInstant(this, ZoneId.systemDefault()) // Convert to ZonedDateTime
    return zonedNow.format(formatter)
}