package com.bitfarmsoftware.sweatlog.data

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.miles
import androidx.health.connect.client.units.milesPerHour
import androidx.health.connect.client.units.watts
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.db.SweatLog
import com.bitfarmsoftware.sweatlog.data.db.SweatLogDao
import com.bitfarmsoftware.sweatlog.data.healthconnect.HealthConnectDataStore
import com.bitfarmsoftware.sweatlog.model.RepoResource
import com.bitfarmsoftware.sweatlog.model.WorkoutData
import com.bitfarmsoftware.sweatlog.model.asSummary
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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepositoryImpl @Inject constructor(
    private val healthConnect: HealthConnectDataStore,
    private val sweatlogDao: SweatLogDao,
): LogsRepository {

    @Inject
    lateinit var workoutFactory: Workout.WorkoutFactory

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

    private val dbScope = CoroutineScope(Job() + Dispatchers.Default)

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
                healthConnect.insertRecords(workout.records(ZonedDateTime.now().offset))
            if (insertedIds.isEmpty()) {
                // TODO queue sync of data into healthconnect later
                Timber.w("No HealthConnect records inserted")
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
}

fun WorkoutSession.records(zoneOffset: ZoneOffset): List<Record> {
    val records = mutableListOf<Record>()
    val segments = mutableListOf<ExerciseSegment>()
    val segmentLevels = mutableListOf<Int>()
    for (segment in this.segments) {
        // make sure endTime is after startTime in millis precision, else HC pukes
        val startMs = segment.startTime.truncatedTo(ChronoUnit.MILLIS)
        val endMs = if (segment.endTime.truncatedTo(ChronoUnit.MILLIS).isAfter(startMs)) {
                segment.endTime.truncatedTo(ChronoUnit.MILLIS)
            } else {
                segment.endTime.plus(1, ChronoUnit.MILLIS)
            }
        segments.add(
            ExerciseSegment(startTime = startMs,
                endTime = endMs,
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                repetitions = 1
            )
        )
        segmentLevels.add(segment.data.resistance!!)
        records.addAll(segment.records(zoneOffset))
    }


    val exerciseSession =  ExerciseSessionRecord(startTime = this.sessionSegment.startTime.truncatedTo(ChronoUnit.MILLIS),
            startZoneOffset = zoneOffset,
            endTime = this.sessionSegment.endTime.truncatedTo(ChronoUnit.MILLIS),
            endZoneOffset = zoneOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            title = "Indoor Cycling",
            notes = segmentLevels.joinToString(),
            segments = segments
        )

    return listOf(exerciseSession) + records
}


fun WorkoutSegment.records(zoneOffset: ZoneOffset): List<Record> {

    val records = mutableListOf<Record>(
        SpeedRecord(
            startTime = this.startTime.truncatedTo(ChronoUnit.MILLIS),
            startZoneOffset = zoneOffset,
            endTime = this.endTime.truncatedTo(ChronoUnit.MILLIS),
            endZoneOffset = zoneOffset,
            samples = listOf(
                SpeedRecord.Sample(
                    time = this.startTime.truncatedTo(ChronoUnit.MILLIS),
                    speed = this.data.avgSpeed!!.milesPerHour
                )
            ),
        ),
        CyclingPedalingCadenceRecord(
            startTime = this.startTime.truncatedTo(ChronoUnit.MILLIS),
            startZoneOffset = zoneOffset,
            endTime = this.endTime.truncatedTo(ChronoUnit.MILLIS),
            endZoneOffset = zoneOffset,
            samples = listOf(
                CyclingPedalingCadenceRecord.Sample(
                    time = this.startTime.truncatedTo(ChronoUnit.MILLIS),
                    revolutionsPerMinute = this.data.avgCadence!!
                )
            ),
        ),
        DistanceRecord(
            startTime = this.startTime.truncatedTo(ChronoUnit.MILLIS),
            startZoneOffset = zoneOffset,
            endTime = this.endTime.truncatedTo(ChronoUnit.MILLIS),
            endZoneOffset = zoneOffset,
            distance = this.data.distance!!.miles,
        ),
        PowerRecord(
            startTime = this.startTime.truncatedTo(ChronoUnit.MILLIS),
            startZoneOffset = zoneOffset,
            endTime = this.endTime.truncatedTo(ChronoUnit.MILLIS),
            endZoneOffset = zoneOffset,
            samples = listOf(
                PowerRecord.Sample(
                    time = this.startTime.truncatedTo(ChronoUnit.MILLIS),
                    power = this.data.avgPower!!.watts
                )
            )
        ),
        TotalCaloriesBurnedRecord(
            startTime = this.startTime.truncatedTo(ChronoUnit.MILLIS),
            startZoneOffset = zoneOffset,
            endTime = this.endTime.truncatedTo(ChronoUnit.MILLIS),
            endZoneOffset = zoneOffset,
            energy = this.data.totalEnergy!!.kilocalories

        )
    )

    // check to see if we were optionally collecting Heartrate
    if (this.data.avgHeartrate != null) {
        records + HeartRateRecord(
            startTime = this.startTime,
            startZoneOffset = zoneOffset,
            endTime = this.endTime,
            endZoneOffset = zoneOffset,
            samples = listOf(
                HeartRateRecord.Sample(
                    time = this.startTime,
                    beatsPerMinute = this.data.avgHeartrate!!.toLong()
                )
            )
        )
    }


    return records
}



