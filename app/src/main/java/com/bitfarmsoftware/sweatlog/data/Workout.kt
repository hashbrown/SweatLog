package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.healthconnect.HealthConnectDataStore
import com.bitfarmsoftware.sweatlog.model.IndoorBikeData
import com.bitfarmsoftware.sweatlog.model.WorkoutData
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

    val session: WorkoutSession = WorkoutSession()

    suspend fun start() {
        _workoutData.value =
            accumulatedFlow(serviceProvider.getService().startTrainingSession())
    }

    suspend fun finish() {
        _workoutData.value = emptyFlow()
        serviceProvider.getService().stopTrainingSession()
    }

    private fun accumulatedFlow(bikeDataFlow: Flow<IndoorBikeData>): Flow<WorkoutData>{
        return bikeDataFlow.scan(session){ acc, value ->
            acc.accumulate(value.asWorkoutData())
        }.map {it.workoutData}
            .onStart { Timber.i("Workout:: workoutData flow started") }
            .onCompletion { Timber.i("Workout:: workoutData flow completed") }
    }

}


class WorkoutSession() {
    companion object {
        val MAX_SEGMENT_TIME: Duration = 15.minutes
    }

    var sessionSegment: WorkoutSegment = WorkoutSegment()
    val workoutData: WorkoutData get() = sessionSegment.data

    private lateinit var currentSegment: WorkoutSegment

    /**
     * WorkoutSession creates a new segment when resistance changes,
     * or when the current segment has reached a max segment time.
     */
    val segments = mutableListOf<WorkoutSegment>()

    fun accumulate(workoutData: WorkoutData): WorkoutSession{
        sessionSegment.accumulate(workoutData)
        if (startNewSegment(workoutData)){
            // handle initial state where we don't yet have a currentSegment holding the last received workoutData.
            val lastWorkoutData = if (::currentSegment.isInitialized) {
                currentSegment.data
            } else {
                WorkoutData()
            }
            currentSegment = WorkoutSegment(lastWorkoutData)
            segments.add(currentSegment)
        }
        currentSegment.accumulate(workoutData)
        sessionSegment.endTime = currentSegment.endTime

        return this
    }

    private fun startNewSegment(workoutData: WorkoutData): Boolean{
        return !this::currentSegment.isInitialized
                ||(currentSegment.data.resistance != workoutData.resistance)
                || Instant.now().isAfter(currentSegment.startTime.plus(MAX_SEGMENT_TIME.toJavaDuration()))
    }

}


class WorkoutSegment(private val initialState: WorkoutData = WorkoutData()){

    lateinit var startTime: Instant
    lateinit var endTime: Instant

    private val accumulator: WorkoutDataAccumulator = WorkoutDataAccumulator(initialState)
    val data get() = accumulator.value

    fun accumulate(workoutData: WorkoutData){
        if (!this::startTime.isInitialized) startTime = Instant.now()

        accumulator.accumulate(workoutData)
        endTime = Instant.now()
    }
}


class WorkoutDataAccumulator(private val initialState: WorkoutData) {
    var value: WorkoutData = WorkoutData()

    private val speedAccum = NumberAccumulator<Double>()
    private val cadenceAccum = NumberAccumulator<Double>()
    private val powerAccum = NumberAccumulator<Int>()
    private val heartrateAccum = NumberAccumulator<Int>()


    fun accumulate(workoutData: WorkoutData): WorkoutDataAccumulator{
        if (workoutData.speed != null) speedAccum.accumulate(workoutData.speed)
        if (workoutData.cadence != null) cadenceAccum.accumulate(workoutData.cadence)
        if (workoutData.power != null) powerAccum.accumulate(workoutData.power)
        if (workoutData.heartrate != null) heartrateAccum.accumulate(workoutData.heartrate)

        // These values are accumulated already in the IndoorBikeData provided by the device.
        // Only report the delta of the value that was observed during this segment.
        val distanceDelta = workoutData.distance?.let { it - (initialState.distance ?: 0.0) }?: 0.0
        val energyDelta = workoutData.totalEnergy?.let { it - (initialState.totalEnergy ?: 0) }?: 0
        val timeDelta = workoutData.elapsedTime?.let { it - (initialState.elapsedTime ?: 0) }?: 0

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
