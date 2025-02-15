package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
import com.bitfarmsoftware.sweatlog.model.IndoorBikeData
import com.bitfarmsoftware.sweatlog.model.asWorkoutData
import com.bitfarmsoftware.sweatlog.service.BleDeviceServiceProvider
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
import java.time.Clock
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration


class Workout(private val serviceProvider: BleDeviceServiceProvider) {


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


class WorkoutSession (private val clock: Clock = Clock.systemDefaultZone()) {
    companion object {
        val MAX_SEGMENT_TIME: Duration = 15.minutes
    }

    var sessionSegment: WorkoutSegment = WorkoutSegment(clock=clock)
    val sessionWorkoutData: WorkoutData get() = sessionSegment.data

    private lateinit var currentSegment: WorkoutSegment

    /**
     * WorkoutSession creates a new segment when resistance changes,
     * or when the current segment has reached a max segment time.
     */
    val segments = mutableListOf<WorkoutSegment>()

    fun accumulate(workoutData: WorkoutData): WorkoutSession{
        if (startNewSegment(workoutData)){
            currentSegment = WorkoutSegment(sessionWorkoutData, clock)
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
    * The resistance changes - this is the main case except:
    ** to protect against quickly changing resistance values (machine sensitivity issue)
    ** wait at least 5 seconds before starting a new segment
    * After 15 minutes a new segment is always created, to report metrics in 15 min increments.
    //TODO allow user to configure the max segment time.
     */
    private fun startNewSegment(workoutData: WorkoutData): Boolean{
        return !this::currentSegment.isInitialized
                ||(currentSegment.data.resistance != workoutData.resistance
                && currentSegment.stabilized)
                || Instant.now(clock).isAfter(currentSegment.startTime.plus(MAX_SEGMENT_TIME.toJavaDuration()))
    }

}


class WorkoutSegment(initialState: WorkoutData = WorkoutData(),
                     private val clock: Clock = Clock.systemDefaultZone()){

    val startTime: Instant = Instant.now(clock)
    var endTime: Instant = Instant.now(clock)

    private val resistance = StableValue(clock)
    var stabilized = resistance.stable

    private val accumulator: WorkoutDataAccumulator = WorkoutDataAccumulator(initialState)
    val data get() = accumulator.value

    fun accumulate(workoutData: WorkoutData){
        // set resistance for Segment on first value
        stabilized = resistance.update(workoutData.resistance!!)
        accumulator.accumulate(workoutData)
        endTime = Instant.now(clock)
    }

    fun duration(now: Instant = endTime): Duration {
        return java.time.Duration.between(startTime, now)
            .toKotlinDuration()

    }
}


class WorkoutDataAccumulator(initialState: WorkoutData) {
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
            elapsedTime = timeDelta,
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
        sum += value.toDouble()
        count++
        if (count == 1) { avg = value}
        if (count % 5 == 0) avg = coerceAvgToType(sum / count)

        if (min == null || value.toDouble() < min!!.toDouble()) {
            min = value
        }
        if (max == null || value.toDouble() > max!!.toDouble()) {
            max = value
        }
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

class StableValue(private val clock: Clock, private val threshold: Duration = 10.seconds){
    private var lastValue: Int = 0
    private lateinit var lastTime: Instant

    var stable = false

    fun update(value: Int): Boolean {
        if (!this::lastTime.isInitialized){
            lastTime = Instant.now(clock)
            lastValue = value
        } else {
            if (value == lastValue) {
                stable = java.time.Duration.between(lastTime, Instant.now(clock)).toKotlinDuration() > threshold
            } else {
                lastTime = Instant.now(clock)
                lastValue = value
                stable = false
            }
        }

        return stable
    }

}

