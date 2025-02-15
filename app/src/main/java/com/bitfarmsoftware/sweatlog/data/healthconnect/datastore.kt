package com.bitfarmsoftware.sweatlog.data.healthconnect

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.miles
import androidx.health.connect.client.units.milesPerHour
import androidx.health.connect.client.units.watts
import com.bitfarmsoftware.sweatlog.data.WorkoutSegment
import com.bitfarmsoftware.sweatlog.data.WorkoutSession
import com.bitfarmsoftware.sweatlog.model.display
import com.bitfarmsoftware.sweatlog.model.roundTo
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.reflect.KClass

class HealthConnectDataStore @Inject constructor(private val context: Context){

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(SpeedRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getWritePermission(PowerRecord::class),
        HealthPermission.getReadPermission(PowerRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class),
        HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(LeanBodyMassRecord::class),
        HealthPermission.getReadPermission(BodyFatRecord::class)
    )

    private var availability = mutableStateOf(false)
        private set

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> true
            else -> false
        }.also { Timber.i("checkAvailability=${it}")}
    }


    suspend fun hasAllPermissions(): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun saveWorkout(workoutSession: WorkoutSession): List<String> {
        return insertRecords(workoutSession.records(ZonedDateTime.now().offset, getBMRPerMinute()))
    }

    private suspend fun insertRecords(records: List<Record>): List<String> {
        val response = healthConnectClient.insertRecords(records)
        return response.recordIdsList
    }

    suspend fun <T: Record> getRecords(recordType: KClass<T>, start: Instant, end: Instant): List<T> {
        val request = ReadRecordsRequest(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(start, end),
            ascendingOrder = false
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    private suspend fun getBMRPerMinute(): Double {
        // get most recent lean mass recorded in last 2 weeks or known avg of 51 lbs.
        val end = Instant.now()
        val start = end.minus(14, ChronoUnit.DAYS)
        val weightRecords = getRecords(WeightRecord::class, start, end)
        val weightLbs =
            if (weightRecords.isEmpty()) {
                142.5
            } else {
                weightRecords.first().weight.inPounds
            }
        val bfRecords = getRecords(BodyFatRecord::class, start, end)
        val bodyfatPct =
            if (bfRecords.isEmpty()) {
                18.0
            } else {
                bfRecords.first().percentage.value
            }
        // Katch-McArdle Formula
        val fatMassLbs = (bodyfatPct / 100) * weightLbs
        val leanMassLbs = weightLbs - fatMassLbs
        val bmrPerMinute = ((21.6 * (leanMassLbs/2.205)) + 370)/24/60
        Timber.i("Calculated BMR/min= ${bmrPerMinute.display(1)} :: weight=$weightLbs fat=$bodyfatPct lean=$leanMassLbs")
        return bmrPerMinute.roundTo(2)
    }

}

fun WorkoutSession.records(zoneOffset: ZoneOffset, bmrPerMin: Double): List<Record> {
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
        records.addAll(segment.records(zoneOffset, bmrPerMin))
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


fun WorkoutSegment.records(zoneOffset: ZoneOffset, bmrPerMin: Double): List<Record> {

    val activeCalories = this.data.totalEnergy!! - (this.duration().inWholeMinutes * bmrPerMin)

    val records = mutableListOf<Record>(
        SpeedRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            samples = listOf(
                SpeedRecord.Sample(
                    time = this.startTime.truncatedTo(ChronoUnit.MILLIS),
                    speed = this.data.avgSpeed!!.roundTo(1).milesPerHour
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
                    revolutionsPerMinute = this.data.avgCadence!!.roundTo(1)
                )
            ),
        ),
        DistanceRecord(
            startTime = this.range().start,
            startZoneOffset = zoneOffset,
            endTime = this.range().endInclusive,
            endZoneOffset = zoneOffset,
            distance = this.data.distance!!.roundTo(1).miles,
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
            energy = activeCalories.roundTo(2).kilocalories

        )
    )

    // check to see if we were optionally collecting Heartrate
    if (this.data.avgHeartrate != null) {
        records.add(
            HeartRateRecord(
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
