package com.bitfarmsoftware.sweatlog.data.db

import com.bitfarmsoftware.sweatlog.model.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant



@Serializable
data class WorkoutData(
    val speed: Double? = null,
    val minSpeed: Double? = null,
    val maxSpeed: Double? = null,
    val avgSpeed: Double? = null,
    val cadence: Double? = null,
    val minCadence: Double? = null,
    val maxCadence: Double? = null,
    val avgCadence: Double? = null,
    val distance: Double? = null,
    val resistance:Int? = null,
    val power: Int? = null,
    val minPower: Int? = null,
    val maxPower: Int? = null,
    val avgPower: Int? = null,
    val totalEnergy: Int? = null,
    val heartrate: Int? = null,
    val minHeartrate: Int? = null,
    val maxHeartrate: Int? = null,
    val avgHeartrate: Int? = null,
    val elapsedTime: Int? = null,
)

@Serializable
data class WorkoutSegmentSummary(
    val startTime: @Serializable(with = InstantSerializer::class) Instant,
    val endTime: @Serializable(with = InstantSerializer::class) Instant,
    val data: WorkoutData
)

@Serializable
data class WorkoutSummary(
    val startTime: @Serializable(with = InstantSerializer::class) Instant,
    val endTime: @Serializable(with = InstantSerializer::class) Instant,
    val summary: WorkoutData,
    val segments: List<WorkoutSegmentSummary>
)