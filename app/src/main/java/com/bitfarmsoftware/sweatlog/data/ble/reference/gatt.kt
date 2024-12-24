package com.bitfarmsoftware.sweatlog.data.ble.reference

import java.util.UUID
import kotlin.uuid.Uuid

object GattServices {
    const val FITNESS_MACHINE = "00001826-0000-1000-8000-00805f9b34fb"
}

object GattCharacteristics {
    const val INDOOR_BIKE_DATA = "00002ad2-0000-1000-8000-00805f9b34fb"
}

fun String.uuid(): UUID { return UUID.fromString(this)}
