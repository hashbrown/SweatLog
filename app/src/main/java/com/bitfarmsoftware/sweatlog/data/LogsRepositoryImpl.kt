package com.bitfarmsoftware.sweatlog.data

import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
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
import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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

fun WorkoutSession.records(zoneOffset: ZoneOffset): List<Record> {
    val records = mutableListOf<Record>()
    val segments = mutableListOf<ExerciseSegment>()
    val segmentLevels = mutableListOf<Int>()
    for (segment in this.segments) {
        segments.add(
            ExerciseSegment(startTime = segment.range().start,
                endTime = segment.range().endInclusive,
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                repetitions = 1
            )
        )
        segmentLevels.add(segment.data.resistance!!)
        records.addAll(segment.records(zoneOffset))
    }


    val exerciseSession =  ExerciseSessionRecord(startTime = this.sessionSegment.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.sessionSegment.range().endInclusive,
            endZoneOffset = zoneOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY,
            title = "Indoor Cycling",
            notes = segmentLevels.joinToString(),
            segments = segments
        )

    return listOf(exerciseSession) + records
}

fun WorkoutSegment.range(): ClosedRange<Instant> {
    val correctedStart = this.startTime.truncatedTo(ChronoUnit.MILLIS)
    val correctedEnd = if (this.endTime.truncatedTo(ChronoUnit.MILLIS).isAfter(correctedStart)) {
        this.endTime.truncatedTo(ChronoUnit.MILLIS)
    } else {
        this.endTime.plus(1, ChronoUnit.MILLIS)
    }
    return correctedStart..correctedEnd
}


fun WorkoutSegment.records(zoneOffset: ZoneOffset): List<Record> {

    val activeCalories = this.data.totalEnergy!! - (this.duration().inWholeMinutes * .60)

    val records = mutableListOf<Record>(
        SpeedRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            samples = listOf(
                SpeedRecord.Sample(
                    time = this.startTime.truncatedTo(ChronoUnit.MILLIS),
                    speed = this.data.avgSpeed!!.milesPerHour
                )
            ),
        ),
        CyclingPedalingCadenceRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            samples = listOf(
                CyclingPedalingCadenceRecord.Sample(
                    time = this.range().start,
                    revolutionsPerMinute = this.data.avgCadence!!
                )
            ),
        ),
        DistanceRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            distance = this.data.distance!!.miles,
        ),
        PowerRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            samples = listOf(
                PowerRecord.Sample(
                    time = this.range().start,
                    power = this.data.avgPower!!.watts
                )
            )
        ),
        TotalCaloriesBurnedRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            energy = this.data.totalEnergy!!.kilocalories

        ),
        ActiveCaloriesBurnedRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            energy = activeCalories.kilocalories

        )
    )

    // check to see if we were optionally collecting Heartrate
    if (this.data.avgHeartrate != null) {
        records.add(HeartRateRecord(
                startTime = this.range().start,
                startZoneOffset = zoneOffset,
                endTime = this.range().endInclusive,
                endZoneOffset = zoneOffset,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = this.range().start,
                        beatsPerMinute = this.data.avgHeartrate!!.toLong()
                    )
                )
            )
        )
    }


    return records
}



