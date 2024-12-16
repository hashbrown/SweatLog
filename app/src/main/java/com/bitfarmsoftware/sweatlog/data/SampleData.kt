package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SweatLog

/**
 * com.bitfarmsoftware.sweatlog.data.SampleData for Jetpack Compose Tutorial
 */
object SampleData {
    // Sample conversation data
    val devicesSample = listOf(
        Device(
            "Device 1",
            "1111-1211 \n more lines \n more lines",
            uuid = "uuid-1111",
            autoConnect = true
        ),
        Device(
            "Device 2",
            "2222-2222",
            uuid = "uuid-2222",
        ),
        Device(
            "Device 3",
            "3333-3333",
            uuid = "uuid-3333",
        ),
        Device(
            "Device 4",
            "444-444",
            uuid = "uuid-4444",
        ),
        Device(
            "Device 5",
            "5555-555",
            uuid = "uuid-5555",
        ),
        Device(
            "Device 6",
            "6666-66666",
            uuid = "uuid-6666",
        ),
    )

    val logsSample = listOf(
        SweatLog(
            "111",
            "First Workout"
        ),
        SweatLog(
            "222",
            "Second Workout"
        ),
        SweatLog(
            "333",
            "Third Workout"
        ),
        SweatLog(
            "444",
            "Fourth Workout"
        ),
    )
}
