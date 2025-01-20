package com.bitfarmsoftware.sweatlog.model

import timber.log.Timber
import java.time.Instant

private const val METERS_PER_MILE = 1609.344

fun ByteArray.toTrainingData(): IndoorBikeData {
    var speed: Double? = null
    var avgSpeed: Double? = null
    var cadence: Double? = null
    var avgCadence: Double? = null
    var distance: Double? = null
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

    if (this.size < 2) {
        Timber.e("Data too short to parse flags")
        return IndoorBikeData()
    }

    val flagsStr =  StringBuilder()

    // Parse the Flags (2 bytes)
    val flags = (this[0].toInt() and 0xFF) or ((this[1].toInt() and 0xFF) shl 8)

    var offset = 2 // Start after the flags

    // Instantaneous Speed or "More Data"
    flagsStr.append("Speed/MoreData(${flags and 0x01})[${offset}]")
    val moreDataIndicator = flags and 0x01 != 0
    if (!moreDataIndicator) {
        if (this.size >= offset + 2) {
            val speedRaw = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            speed = (speedRaw / 100u).toDouble()/1.60934 // Convert km/h -> miles/h
            offset += 2
        }
    }

    // Average Speed
    flagsStr.append(", AvgSpeed(${flags and 0x02})[${offset}]")
    if (flags and 0x02 != 0) {
        if (this.size >= offset + 2) {
            val avgSpeedRaw = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            avgSpeed = (avgSpeedRaw / 100u).toDouble()/1.60934 // Convert km/h -> miles/h
            offset += 2
        }
    }

    // Instantaneous Cadence
    flagsStr.append(", Cadence(${flags and 0x04})[${offset}]")
    if (flags and 0x04 != 0) {
        if (this.size >= offset + 2) {
            val cadenceRaw = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            cadence = (cadenceRaw / 2u).toDouble() // Convert to RPM
            offset += 2
        }
    }

    // Average Cadence
    flagsStr.append(", AvgCadence(${flags and 0x08})[${offset}]")
    if (flags and 0x08 != 0) {
        if (this.size >= offset + 2) {
            val avgCadenceRaw = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            avgCadence = (avgCadenceRaw / 2u).toDouble() // Convert to RPM
            offset += 2
        }
    }

    // Distance
    flagsStr.append(", Distance(${flags and 0x10})[${offset}]")
    if (flags and 0x10 != 0) {
        if (this.size >= offset + 3) {
            val distanceRawMeters = ((this[offset].toUInt() and 0xFFu) or
                    ((this[offset + 1].toUInt() and 0xFFu) shl 8) or
                    ((this[offset + 2].toUInt() and 0xFFu) shl 16))
            distance=(distanceRawMeters.toDouble()/ METERS_PER_MILE)
            offset += 3
        }
    }

    // Resistance
    flagsStr.append(", Resistance(${flags and 0x20})[${offset}]")
    if (flags and 0x20 != 0) {
        if (this.size >= offset + 1) {
            resistance = (this[offset].toInt() and 0xFF).toShort()
            offset += 2
        }
    }

    // Instantaneous Power
    flagsStr.append(", Power(${flags and 0x40})[${offset}]")
    if (flags and 0x40 != 0) {
        if (this.size >= offset + 2) {
            power = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8))
            offset += 2
        }
    }

    // Average Power
    flagsStr.append(", AvgPower(${flags and 0x80})[${offset}]")
    if (flags and 0x80 != 0) {
        if (this.size >= offset + 2) {
            avgPower = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8))
            offset += 2
        }
    }

    // Energy Expenditure is all under the Bit 8 flag
    flagsStr.append(", EnergyExpenditure(${flags and 0x100})[${offset}]")
    if (flags and 0x100 != 0) {
        // Total Energy
        if (this.size >= offset + 2) {
            totalEnergy = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            offset += 2
        }

        // Energy Per Hour
        if (this.size >= offset + 2) {
            energyPerHour = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            offset += 2
        }


        // Energy Per Minute
        if (this.size >= offset + 1) {
            energyPerMinute = (this[offset].toInt() and 0xFF).toUShort()
            offset += 1
        }
    }

    // Heart Rate
    flagsStr.append(", HeartRate(${flags and 0x200})[${offset}]")
    if (flags and 0x200 != 0) {
        if (this.size >= offset + 1) {
            heartRate = (this[offset].toInt() and 0xFF).toUShort()
            offset += 1
        }
    }

    // Metabolic Equivalent
    flagsStr.append(", MET(${flags and 0x400})[${offset}]")
    if (flags and 0x400 != 0) {
        if (this.size >= offset + 1) {
            met = (this[offset].toInt() and 0xFF).toUShort()
            offset += 1
        }
    }

    // Elapsed Time
    flagsStr.append(", ElapsedTime(${flags and 0x800})[${offset}]")
    if (flags and 0x800 != 0) {
        if (this.size >= offset + 2) {
            elapsedTime = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            offset += 2
        }
    }

    // Remaining Time
    flagsStr.append(", RemainingTime(${flags and 0x1000})[${offset}]")
    if (flags and 0x1000 != 0) {
        if (this.size >= offset + 2) {
            remainingTime = ((this[offset].toInt() and 0xFF) or
                    ((this[offset + 1].toInt() and 0xFF) shl 8)).toUInt()
            offset += 2
        }
    }

//    Timber.i("RECORD: Bytes(${this.size}) Flags{${flagsStr}}")

    return IndoorBikeData(
        speed = speed,
        avgSpeed = avgSpeed,
        cadence = cadence,
        avgCadence =  avgCadence,
        distance =distance,
        resistance = resistance,
        power = power,
        avgPower = avgPower,
        totalEnergy = totalEnergy,
        energyPerHour = energyPerHour,
        energyPerMinute = energyPerMinute,
        heartrate = heartRate,
        metabolicEquivalent = met,
        elapsedTime = elapsedTime,
        remainingTime = remainingTime
    )
}

fun Double.display(precision: Int): String {
    return String.format("%.${precision}f", this)
}