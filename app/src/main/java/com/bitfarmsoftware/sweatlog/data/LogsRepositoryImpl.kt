package com.bitfarmsoftware.sweatlog.data

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.units.miles
import androidx.health.connect.client.units.milesPerHour
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.db.SweatLogDao
import com.bitfarmsoftware.sweatlog.data.db.SweatLog
import com.bitfarmsoftware.sweatlog.data.healthconnect.HealthConnectDataStore
import com.bitfarmsoftware.sweatlog.model.RepoResource
import com.bitfarmsoftware.sweatlog.model.WorkoutData
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
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepositoryImpl @Inject constructor(
    private val healthConnect: HealthConnectDataStore,
    private val sweatlogDao: SweatLogDao,
    private val serviceProvider: BleDeviceServiceProvider
): LogsRepository {

    @Inject lateinit var workoutFactory: Workout.WorkoutFactory

    override val requiredPermissions = healthConnect.permissions

    private var _hasPermissions = MutableStateFlow(false)
    override val hasAllPermissions: StateFlow<Boolean> = _hasPermissions

    @OptIn(ExperimentalCoroutinesApi::class)
    override val sweatLogs: Flow<RepoResource<List<SweatLog>>> get() = _sweatLogs
    private val _sweatLogs: Flow<RepoResource<List<SweatLog>>> = flow {
        emit(RepoResource.Loading)
        sweatlogDao.getAllLogs().collect { logs ->
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

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    override suspend fun checkPermissions() {
        val hasPermissionsResult = healthConnect.hasAllPermissions()
        Timber.i("checkPermissionsResult=${hasPermissionsResult}")
        _hasPermissions.value = hasPermissionsResult
    }


    override fun requestHealthConnectPermissions(): ActivityResultContract<Set<String>, Set<String>> {
        return healthConnect.requestPermissionsActivityContract()
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun startNewWorkout(device: BleDeviceResource): Flow<RepoResource<WorkoutData>> {

        if (activeWorkout == null) {
            val workout = workoutFactory.create(device)
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

    override suspend fun finishWorkout(){
        Timber.i("Finish Workout")
        activeWorkout?.let {
            it.finish()
            saveWorkout(it.session)
        }
        activeWorkout = null
    }

    private suspend fun saveWorkout(workout: WorkoutSession){
        val insertedIds =
                healthConnect.insertRecords(workout.records(ZonedDateTime.now().offset))
        if (insertedIds.isEmpty()) {
            // TODO queue sync of data into healthconnect later
        } else {
            val sessionHCId = insertedIds[0]
            val sweatlog = SweatLog(
                title = "Indoor Cycling",
                connectId = sessionHCId,
                workoutData = workout.asSummary()
            )
            sweatlogDao.insertAll(sweatlog)
        }
    }
}

fun WorkoutSession.records(zoneOffset: ZoneOffset): List<Record> {
    val records = mutableListOf<Record>()
    val segments = mutableListOf<ExerciseSegment>()
    val segmentLevels = mutableListOf<Int>()
    for (segment in this.segments) {
        segments.add(
            ExerciseSegment(startTime = segment.startTime,
                endTime = segment.endTime,
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                repetitions = 1
            )
        )
        segmentLevels.add(segment.data.resistance!!)
        records.addAll(segment.records(zoneOffset))
    }


    val exerciseSession =  ExerciseSessionRecord(startTime = this.sessionSegment.startTime,
            startZoneOffset = zoneOffset,
            endTime = this.sessionSegment.endTime,
            endZoneOffset = zoneOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            title = "Indoor Cycling - Biking Stationary",
            notes = segmentLevels.joinToString(),
            segments = segments
        )

    return listOf(exerciseSession) + records
}


fun WorkoutSegment.records(zoneOffset: ZoneOffset) = mutableListOf<Record>(
        SpeedRecord(
            startTime = this.startTime,
            startZoneOffset = zoneOffset,
            endTime = this.endTime,
            endZoneOffset = zoneOffset,
            samples = listOf(
                SpeedRecord.Sample(time = this.startTime,
                    speed = this.data.avgSpeed!!.milesPerHour)
            ),
        ),
        CyclingPedalingCadenceRecord(
            startTime = this.startTime,
            startZoneOffset = zoneOffset,
            endTime = this.endTime,
            endZoneOffset = zoneOffset,
            samples = listOf(
                CyclingPedalingCadenceRecord.Sample(time = this.startTime,
                    revolutionsPerMinute = this.data.avgCadence!!)
            ),
        ),
        DistanceRecord(startTime = this.startTime,
            startZoneOffset = zoneOffset,
            endTime = this.endTime,
            endZoneOffset = zoneOffset,
            distance = this.data.distance!!.miles,
        ),
        HeartRateRecord(startTime = this.startTime,
            startZoneOffset = zoneOffset,
            endTime = this.endTime,
            endZoneOffset = zoneOffset,
            samples =
                if (this.data.avgHeartrate != null) {
                    listOf(HeartRateRecord.Sample(
                        time = this.startTime,
                        beatsPerMinute = this.data.avgHeartrate!!.toLong()
                    ))
                } else {
                    emptyList()
                }

        )
    )



