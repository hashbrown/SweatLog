package com.bitfarmsoftware.sweatlog.data

import androidx.activity.result.contract.ActivityResultContract
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.db.SweatLog
import com.bitfarmsoftware.sweatlog.data.db.SweatLogDao
import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
import com.bitfarmsoftware.sweatlog.data.healthconnect.HealthConnectDataStore
import com.bitfarmsoftware.sweatlog.model.RepoResource
import com.bitfarmsoftware.sweatlog.model.asSummary
import com.bitfarmsoftware.sweatlog.service.BleDeviceServiceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepositoryImpl @Inject constructor(
    private val healthConnect: HealthConnectDataStore,
    private val sweatlogDao: SweatLogDao,
    private val serviceProvider: BleDeviceServiceProvider
): LogsRepository {

    override val requiredPermissions = healthConnect.permissions

    private var _hasPermissions = MutableStateFlow(false)
    override val hasAllPermissions: StateFlow<Boolean> = _hasPermissions

    @OptIn(ExperimentalCoroutinesApi::class)
    override val sweatLogs: Flow<RepoResource<List<SweatLog>>> get() = _sweatLogs
    private val _sweatLogs: Flow<RepoResource<List<SweatLog>>> = flow {
        emit(RepoResource.Loading)
        sweatlogDao.getAllSweatLogs()
            .catch { cause -> Timber.e(cause, "sweatLogs flow error") }
            .onEach { Timber.i("sweatLogs db flow emitting ${it.size} logs") }
            .collect { logs ->
            emit(RepoResource.Success(logs))
        }
    }.catch { cause ->
        emit(RepoResource.Error(cause))
    }.onStart {
        Timber.i("sweatLogs flow started")
    }.onCompletion {
        Timber.i("sweatLogs flow done")
    }

    private var activeWorkout: Workout? = null

    private val dbScope = CoroutineScope(Job() + Dispatchers.Default)

    override suspend fun checkPermissions() {
        val hasPermissionsResult = healthConnect.hasAllPermissions()
        Timber.i("checkPermissionsResult=${hasPermissionsResult}")
        _hasPermissions.value = hasPermissionsResult
    }


    override fun requestHealthConnectPermissions(): ActivityResultContract<Set<String>, Set<String>> {
        return healthConnect.requestPermissionsActivityContract()
    }

    override fun getSweatLog(id: Int): Flow<RepoResource<SweatLog>> {
        return sweatlogDao.getSweatLog(id).map { RepoResource.Success(it) }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun startNewWorkout(device: BleDeviceResource): Flow<RepoResource<WorkoutData>> {

        if (activeWorkout == null) {
            val workout = Workout(serviceProvider)
            workout.start()
            activeWorkout = workout
        }

        return flowOf(RepoResource.Loading)
            .flatMapLatest {
                activeWorkout!!.workoutData
                    .map({ RepoResource.Success(it) })
                    .onStart { Timber.i("logRepo:: workoutData flow started") }
                    .onCompletion { Timber.i("LogRepo:: workoutData flow completed") }
            }

    }

    override suspend fun finishWorkout() {
        Timber.i("Finish Workout")
        activeWorkout?.let {
            it.finish()
            saveWorkout(it.session)
        }
        activeWorkout = null
    }

    private suspend fun saveWorkout(workout: WorkoutSession) {
        withContext(dbScope.coroutineContext) {
            val insertedIds =
                healthConnect.saveWorkout(workout)
            if (insertedIds.isEmpty()) {
                // TODO queue sync of data into healthconnect later
                Timber.w("No HealthConnect records inserted")
            } else {
                val sessionHCId = insertedIds[0]
                val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy - h:mma")
                val now = Instant.now().truncatedTo(ChronoUnit.MINUTES) // Truncate to the nearest earlier hour
                val zonedNow = ZonedDateTime.ofInstant(now, ZoneId.systemDefault()) // Convert to ZonedDateTime
                val title = "${zonedNow.format(formatter)} - Indoor Cycling"
                val sweatlog = SweatLog(
                    title = title,
                    connectId = sessionHCId,
                    workoutData = workout.asSummary()
                )
                sweatlogDao.insertAll(sweatlog)
            }
        }
    }
}







