package com.bitfarmsoftware.sweatlog.model

import kotlinx.coroutines.flow.Flow

data class SweatLog(
    val id: String,
    val title: String
)

data class LogData(
    val speed: Double? = null,
    val avgSpeed: Double? = null,
    val cadence: Double? = null,
    val avgCadence: Double? = null,
    val distance: UInt? = null,
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