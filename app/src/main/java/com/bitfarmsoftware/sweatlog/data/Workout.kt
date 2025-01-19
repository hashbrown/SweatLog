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
import kotlinx.coroutines.flow.scan
import kotlinx.serialization.Serializable
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
            accumulatedFlow(serviceProvider.getService().startTrainingSession(device))
    }

    suspend fun finish() {
//        serviceProvider.getService().stopTrainingSession(device)
        _workoutData.value = emptyFlow()
    }

    fun accumulatedFlow(bikeDataFlow: Flow<IndoorBikeData>): Flow<WorkoutData>{
        return bikeDataFlow.scan(session){ acc, value ->
            acc.accumulate(value.asWorkoutData())
        }.map {it.workoutData}
    }

}


class WorkoutSession() {
    companion object {
        val MAX_SEGMENT_TIME: Duration = 15.minutes
    }

    private var currentSegment: WorkoutSegment = WorkoutSegment()

    var sessionSegment: WorkoutSegment = WorkoutSegment()
    val workoutData: WorkoutData get() = sessionSegment.data

    /**
     * WorkoutSession creates a new segment when resistance changes,
     * or when the current segment has reached a max segment time.
     */
    val segments = mutableListOf<WorkoutSegment>()

    fun accumulate(workoutData: WorkoutData): WorkoutSession{
        sessionSegment.accumulate(workoutData)
        if (startNewSegment(workoutData)){
            // start new segment when resistance changes
            currentSegment = WorkoutSegment()
            segments.add(currentSegment)
        }
        currentSegment.accumulate(workoutData)

        return this
    }

    private fun startNewSegment(workoutData: WorkoutData): Boolean{
        return (currentSegment.data.resistance != workoutData.resistance)
                || Instant.now().isAfter(currentSegment.startTime.plus(MAX_SEGMENT_TIME.toJavaDuration()))
    }

}


class WorkoutSegment(){

    lateinit var startTime: Instant
    lateinit var endTime: Instant

    private val accumulator: WorkoutDataAccumulator = WorkoutDataAccumulator()
    val data get() = accumulator.value

    fun accumulate(workoutData: WorkoutData){
        if (!this::startTime.isInitialized) startTime = Instant.now()
        endTime = Instant.now()
        accumulator.accumulate(workoutData)
    }

}


class WorkoutDataAccumulator {
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
            avgHeartrate = heartrateAccum.avg
        )

        return this
    }

}

class NumberAccumulator<T : Number> {
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
