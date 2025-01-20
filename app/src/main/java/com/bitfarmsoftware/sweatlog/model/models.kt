package com.bitfarmsoftware.sweatlog.model

import com.bitfarmsoftware.sweatlog.data.WorkoutSegment
import com.bitfarmsoftware.sweatlog.data.WorkoutSession
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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
    val startTime: @Serializable(with = InstantSerializer::class)Instant,
    val endTime: @Serializable(with = InstantSerializer::class)Instant,
    val data: WorkoutData
)

@Serializable
data class WorkoutSummary(
    val startTime: @Serializable(with = InstantSerializer::class)Instant,
    val endTime: @Serializable(with = InstantSerializer::class)Instant,
    val summary: WorkoutData,
    val segments: List<WorkoutSegmentSummary>
)

fun WorkoutSession.asSummary(): WorkoutSummary {
    return WorkoutSummary(
        startTime = this.sessionSegment.startTime,
        endTime = this.sessionSegment.endTime,
        summary = this.sessionSegment.data,
        segments = this.segments.map { it.asSummary() }
    )
}

fun WorkoutSegment.asSummary(): WorkoutSegmentSummary {
    return WorkoutSegmentSummary(
        startTime = this.startTime,
        endTime = this.endTime,
        data = this.data
    )
}

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

data class IndoorBikeData(
    val speed: Double? = null,
    val avgSpeed: Double? = null,
    val cadence: Double? = null,
    val avgCadence: Double? = null,
    val distance: Double? = null,
    val resistance:Short? = null,
    val power: Int? = null,
    val avgPower: Int? = null,
    val totalEnergy: UInt? = null,
    val energyPerHour: UInt? = null,
    val energyPerMinute: UShort? = null,
    val heartrate: UShort? = null,
    val metabolicEquivalent: UShort? = null,
    val elapsedTime: UInt? = null,
    val remainingTime: UInt? = null
)

fun IndoorBikeData.asWorkoutData(): WorkoutData {
    return WorkoutData(
        speed = this.speed,
        avgSpeed = this.avgSpeed,
        cadence = this.cadence,
        avgCadence =  this.avgCadence,
        distance = this.distance,
        resistance = this.resistance?.toInt(),
        power = this.power,
        avgPower = this.avgPower,
        totalEnergy = this.totalEnergy?.toInt(),
        heartrate = this.heartrate?.toInt(),
        elapsedTime = this.elapsedTime?.toInt()
    )
}


interface Device {
    val name: String
    val address: String
    // likely not stable, this can constantly change for a device
    val uuid: String
}

data class SavedDevice(
    override val name: String,
    override val address: String,
    override val uuid: String, // likely not stable, this can constantly change for a device
    val autoConnect: Boolean = false) : Device {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SavedDevice

        if (name != other.name) return false
        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + address.hashCode()
        return result
    }
}

data class UserData(
    val savedDevices: Set<SavedDevice>
)

sealed interface RepoResource<out T> {
    data object None : RepoResource<Nothing>
    data object Loading : RepoResource<Nothing>
    data class Success<out T>(val data: T) : RepoResource<T>
    data class Error<out T>(val exception: Throwable) : RepoResource<T>
}
