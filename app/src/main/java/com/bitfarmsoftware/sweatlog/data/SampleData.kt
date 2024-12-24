package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.SweatLog

/**
 * com.bitfarmsoftware.sweatlog.data.SampleData for Jetpack Compose Tutorial
 */
object SampleData {
    // Sample conversation data
    val devicesSample = listOf(
        SavedDevice(
            "Device 1",
            "1111-1211 \n more lines \n more lines",
            uuid = "uuid-1111",
            autoConnect = true
        ),
        SavedDevice(
            "Device 2",
            "2222-2222",
            uuid = "uuid-2222",
        ),
        SavedDevice(
            "Device 3",
            "3333-3333",
            uuid = "uuid-3333",
        ),
        SavedDevice(
            "Device 4",
            "444-444",
            uuid = "uuid-4444",
        ),
        SavedDevice(
            "Device 5",
            "5555-555",
            uuid = "uuid-5555",
        ),
        SavedDevice(
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
