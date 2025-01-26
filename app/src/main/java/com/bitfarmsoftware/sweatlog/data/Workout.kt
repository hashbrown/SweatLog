package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.healthconnect.HealthConnectDataStore
import com.bitfarmsoftware.sweatlog.model.IndoorBikeData
import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
import com.bitfarmsoftware.sweatlog.model.asWorkoutData
import com.bitfarmsoftware.sweatlog.service.BleDeviceServiceProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import timber.log.Timber
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration


class Workout@AssistedInject constructor(
    private val healthConnect: HealthConnectDataStore,
    private val serviceProvider: BleDeviceServiceProvider,
    @Assisted
    private val device: BleDeviceResource

) {

    @AssistedFactory
    interface WorkoutFactory {
        fun create(device: BleDeviceResource): Workout
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val workoutData: Flow<WorkoutData> get() = _workoutData.flatMapLatest { it }
    private val _workoutData = MutableStateFlow<Flow<WorkoutData>>(emptyFlow())

    lateinit var session: WorkoutSession

    suspend fun start() {
        session = WorkoutSession()
        _workoutData.value =
            accumulatedFlow(serviceProvider.getService().startTrainingSession())
    }

    suspend fun finish() {
        _workoutData.value = emptyFlow()
        serviceProvider.getService().stopTrainingSession()
    }

    /*
    Scan takes each reading from the bike data flow, turns it into a WorkoutData object,
     and accumulates it into the WorkoutSession.
    Then the map function returns the accumulated result from the WorkoutSession
     */
    private fun accumulatedFlow(bikeDataFlow: Flow<IndoorBikeData>): Flow<WorkoutData>{
        return bikeDataFlow.scan(session){ acc, value ->
            acc.accumulate(value.asWorkoutData())
        }.map {it.sessionWorkoutData}
            .onStart { Timber.i("Workout:: workoutData flow started") }
            .onCompletion { Timber.i("Workout:: workoutData flow completed") }
    }

}


class WorkoutSession() {
    companion object {
        val MAX_SEGMENT_TIME: Duration = 15.minutes
    }

    var sessionSegment: WorkoutSegment = WorkoutSegment()
    val sessionWorkoutData: WorkoutData get() = sessionSegment.data

    private lateinit var currentSegment: WorkoutSegment

    /**
     * WorkoutSession creates a new segment when resistance changes,
     * or when the current segment has reached a max segment time.
     */
    val segments = mutableListOf<WorkoutSegment>()

    fun accumulate(workoutData: WorkoutData): WorkoutSession{
        if (startNewSegment(workoutData)){
            currentSegment = WorkoutSegment(sessionWorkoutData)
            segments.add(currentSegment)
        }
        sessionSegment.accumulate(workoutData)
        currentSegment.accumulate(workoutData)
        sessionSegment.endTime = currentSegment.endTime

        return this
    }

    /*
    Start new WorkoutSegment when
    * Not initialized - it's the first segment
    * The resitance changes - this is the main case except:
    ** to protect against quickly changing resistance values (machine sensitivity issue)
    ** wait at least 5 seconds before starting a new segment
    * After 15 minutes a new segment is always created, to report metrics in 15 min increments.
    //TODO allow user to configure the max segment time.
     */
    private fun startNewSegment(workoutData: WorkoutData): Boolean{
        return !this::currentSegment.isInitialized
                ||(currentSegment.data.resistance != workoutData.resistance
                && currentSegment.duration().inWholeSeconds > 5)
                || Instant.now().isAfter(currentSegment.startTime.plus(MAX_SEGMENT_TIME.toJavaDuration()))
    }

}


class WorkoutSegment(initialState: WorkoutData = WorkoutData()){

    val startTime: Instant = Instant.now()
    var endTime: Instant = Instant.now()

    private val accumulator: WorkoutDataAccumulator = WorkoutDataAccumulator(initialState)
    val data get() = accumulator.value

    fun accumulate(workoutData: WorkoutData){

        accumulator.accumulate(workoutData)
        endTime = Instant.now()
    }

    fun duration(): Duration {
        return java.time.Duration.between(startTime, endTime)
            .toKotlinDuration()

    }
}


class WorkoutDataAccumulator(private val initialState: WorkoutData) {
    var value: WorkoutData = WorkoutData()

    private val speedAccum = NumberAccumulator<Double>()
    private val cadenceAccum = NumberAccumulator<Double>()
    private val powerAccum = NumberAccumulator<Int>()
    private val heartrateAccum = NumberAccumulator<Int>()
    private val initialDistance = initialState.distance ?: 0.0
    private val initialEnergy = initialState.totalEnergy ?: 0
    private val initialTime = initialState.elapsedTime ?: 0


    fun accumulate(workoutData: WorkoutData): WorkoutDataAccumulator{
        if (workoutData.speed != null) speedAccum.accumulate(workoutData.speed)
        if (workoutData.cadence != null) cadenceAccum.accumulate(workoutData.cadence)
        if (workoutData.power != null) powerAccum.accumulate(workoutData.power)
        if (workoutData.heartrate != null) heartrateAccum.accumulate(workoutData.heartrate)

        // These values are accumulated already in the IndoorBikeData provided by the device.
        // Only report the delta of the value that was observed during this segment.
        val distanceDelta = workoutData.distance?.let { it - initialDistance }?: 0.0
        val energyDelta = workoutData.totalEnergy?.let { it - initialEnergy }?: 0
        val timeDelta = workoutData.elapsedTime?.let { it - initialTime }?: 0

        value = workoutData.copy(
            minSpeed = speedAccum.min,
            maxSpeed = speedAccum.max,
            avgSpeed = speedAccum.avg,
            minCadence = cadenceAccum.min,
            maxCadence = cadenceAccum.max,
            avgCadence = cadenceAccum.avg,
            minPower = powerAccum.min,
            maxPower = powerAccum.max,
            avgPower = powerAccum.avg,
            minHeartrate = heartrateAccum.min,
            maxHeartrate = heartrateAccum.max,
            avgHeartrate = heartrateAccum.avg,
            distance = distanceDelta,
            totalEnergy = energyDelta,
            elapsedTime = timeDelta
        )
        return this
    }

}

open class NumberAccumulator<T : Number> {
    private var count: Int = 0
    private var sum: Double = 0.0 // Use Double for precise calculations

    var min: T? = null
    var max: T? = null

    var avg: T? = null

    fun accumulate(value: T) {
        val adjValue = adjustedValue(value)
        sum += adjValue.toDouble()
        count++
        if (count == 1) { avg = adjValue}
        if (count % 5 == 0) avg = coerceAvgToType(sum / count)

        if (min == null || adjValue.toDouble() < min!!.toDouble()) {
            min = adjValue
        }
        if (max == null || adjValue.toDouble() > max!!.toDouble()) {
            max = adjValue
        }
    }

    open fun adjustedValue(value: T): T {
        return value
    }

    @Suppress("UNCHECKED_CAST")
    private fun coerceAvgToType(value: Double): T? {
        return when (min) { // Use `min` or any T value to infer the type of T
            is Int -> value.roundToInt() as T // Rounds the Double before converting to Int
            is Long -> value.roundToLong() as T
            is Float -> value.toFloat() as T
            is Double -> value as T
            is Short -> value.roundToInt().toShort() as T
            else -> throw IllegalArgumentException("Unsupported type: ${min?.javaClass?.simpleName}")
        }
    }
}

@Suppress("UNCHECKED_CAST")
class DeltaNumberAccumulator<T : Number> (private val baseValue: T): NumberAccumulator<T>() {
    override fun adjustedValue(value: T): T {
        return when (value) {
            is Int -> (value - baseValue.toInt()) as T
            is Long -> (value - baseValue.toLong()) as T
            is Float -> (value - baseValue.toFloat()) as T
            is Double -> (value - baseValue.toDouble()) as T
            is Short -> (value - baseValue.toShort()).toShort() as T
            is Byte -> (value - baseValue.toByte()).toByte() as T
            else -> throw IllegalArgumentException("Unsupported type: ${value::class.simpleName}")
        }
    }
}
