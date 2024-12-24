package com.bitfarmsoftware.sweatlog.data

import androidx.compose.runtime.collectAsState
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.LogData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject constructor() {

    fun observeLogData(device: BleDeviceResource): Flow<LogData> {
        Timber.i("Request to observe LogData to Device that is connected? ${device.connected}")
        return device.bikeData
            .onEach { Timber.i("bikeData is emitting: ${it}") }
            .filter { it.isNotEmpty() }
            .map { data -> parseBikeData(data) }
    }

    private fun parseBikeData(data: ByteArray): LogData {
        var speed: Double? = null
        var avgSpeed: Double? = null
        var cadence: Double? = null
        var avgCadence: Double? = null
        var distance: UInt? = null
        var resistance: Short? = null
        var power: Int? = null
        var avgPower: Int? = null
        var totalEnergy: UInt? = null
        var energyPerHour: UInt? = null
        var energyPerMinute: UShort? = null
        var heartRate: UShort? = null
        var met: UShort? = null
        var elapsedTime: UInt? = null
        var remainingTime: UInt? = null

        if (data.size < 2) {
            Timber.e("Data too short to parse flags")
            return LogData()
        }

        // Parse the Flags (2 bytes)
        val flags = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)

        var offset = 2 // Start after the flags

        // Instantaneous Speed or "More Data"
        val hasMoreData = flags and 0x01 != 0
        if (!hasMoreData) {
            // Parse Instantaneous Speed (uint16)
            if (data.size >= offset + 2) {
                val speedRaw = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                speed = (speedRaw / 100u).toDouble() // Convert to m/s
                offset += 2
            }
        }

        // Average Speed
        if (flags and 0x02 != 0) { // Bit 1
            if (data.size >= offset + 2) {
                val avgSpeedRaw = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                avgSpeed = (avgSpeedRaw / 100u).toDouble() // Convert to m/s
                offset += 2
            }
        }

        // Instantaneous Cadence
        if (flags and 0x04 != 0) { // Bit 2
            if (data.size >= offset + 2) {
                val cadenceRaw = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                cadence = (cadenceRaw / 2u).toDouble() // Convert to RPM
                offset += 2
            }
        }

        // Average Cadence
        if (flags and 0x08 != 0) { // Bit 3
            if (data.size >= offset + 2) {
                val avgCadenceRaw = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                avgCadence = (avgCadenceRaw / 2u).toDouble() // Convert to RPM
                offset += 2
            }
        }

        // Distance
        if (flags and 0x10 != 0) { // Bit 4
            if (data.size >= offset + 3) {
                distance = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8) or
                        ((data[offset + 2].toInt() and 0xFF) shl 16)).toUInt()
                offset += 3
            }
        }

        // Resistance
        if (flags and 0x20 != 0) { // Bit 5
            if (data.size >= offset + 1) {
                resistance = (data[offset].toInt() and 0xFF).toShort()
                offset += 1
            }
        }

        // Instantaneous Power
        if (flags and 0x40 != 0) { // Bit 6
            if (data.size >= offset + 2) {
                power = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8))
                offset += 2
            }
        }

        // Average Power
        if (flags and 0x80 != 0) { // Bit 7
            if (data.size >= offset + 2) {
                avgPower = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8))
                offset += 2
            }
        }

        // Total Energy
        if (flags and 0x100 != 0) { // Bit 8
            if (data.size >= offset + 2) {
                totalEnergy = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                offset += 2
            }
        }

        // Energy Per Hour
        if (flags and 0x200 != 0) { // Bit 9
            if (data.size >= offset + 2) {
                energyPerHour = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                offset += 2
            }
        }

        // Energy Per Minute
        if (flags and 0x400 != 0) { // Bit 9
            if (data.size >= offset + 1) {
                energyPerMinute = (data[offset].toInt() and 0xFF).toUShort()
                offset += 1
            }
        }

        // Heart Rate
        if (flags and 0x800 != 0) { // Bit 10
            if (data.size >= offset + 1) {
                heartRate = (data[offset].toInt() and 0xFF).toUShort()
                offset += 1
            }
        }

        // Metabolic Equivalent
        if (flags and 0x1000 != 0) { // Bit 10
            if (data.size >= offset + 1) {
                met = (data[offset].toInt() and 0xFF).toUShort()
                offset += 1
            }
        }

        // Elapsed Time
        if (flags and 0x2000 != 0) { // Bit 11
            if (data.size >= offset + 2) {
                elapsedTime = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                offset += 2
            }
        }

        // Remaining Time
        if (flags and 0x4000 != 0) { // Bit 11
            if (data.size >= offset + 2) {
                remainingTime = ((data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
                offset += 2
            }
        }

        return LogData(
            speed,
            avgSpeed,
            cadence,
            avgCadence,
            distance,
            resistance,
            power,
            avgPower,
            totalEnergy,
            energyPerHour,
            energyPerMinute,
            heartRate,
            met,
            elapsedTime,
            remainingTime
        )
    }
}

