package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.data.ble.hexStringToByteArray
import com.bitfarmsoftware.sweatlog.model.WorkoutData
import com.bitfarmsoftware.sweatlog.model.SavedDevice

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

//    val logsSample = listOf(
//        SweatLog(
//            "111",
//            "12-06-2024",
//            WorkoutData(elapsedTime = 1234U, avgPower = 180, totalEnergy = 234u)
//        ),
//        SweatLog(
//            "2222",
//            "12-12-2024",
//            WorkoutData(elapsedTime = 6345U, avgPower = 138, totalEnergy = 513u)
//        ),
//        SweatLog(
//            "3333",
//            "12-15-2024",
//            WorkoutData(elapsedTime = 345U, avgPower = 210, totalEnergy = 68u)
//        ),
//        SweatLog(
//            "4444",
//            "12-24-2024",
//            WorkoutData(elapsedTime = 2223U, avgPower = 165, totalEnergy = 436u)
//        ),
//    )

    val bikeData = listOf(
        "7409F705520078000005001D0000000000001E00".hexStringToByteArray(),
        "7409A7054E0081000005001B0000000000002000".hexStringToByteArray(),
        "7409F705520089000005001D0000000000002200".hexStringToByteArray(),
        "7409A7054E0091000005001B0000000000002400".hexStringToByteArray(),
        "7409C705500099000005001B0001000000002600".hexStringToByteArray(),
        "7409C7055000A2000005001B0001000000002800".hexStringToByteArray(),
        "7409A7054E00AA000005001B0001000000002A00".hexStringToByteArray(),
        "7409C7055000B2000005001B0001000000002C00".hexStringToByteArray(),
        "7409C7055000BA000005001B0001000000002E00".hexStringToByteArray(),
        "7409C7055000C2000005001B0001000000003000".hexStringToByteArray(),
        "7409C7055000CA000005001B0001000000003200".hexStringToByteArray(),
        "7409F7055200D3000005001D0001000000003400".hexStringToByteArray(),
        "7409F7055200DB000005001D0001000000003600".hexStringToByteArray(),
        "7409F7055200E4000005001D0001000000003800".hexStringToByteArray(),
        "7409F7055200EC000005001D0001000000003A00".hexStringToByteArray(),
        "740908065400F5000005001E0001000000003C00".hexStringToByteArray(),
        "740938065600FD00000500200001000000003E00".hexStringToByteArray(),
        "74090806540006010005001E0001000000004000".hexStringToByteArray(),
        "7409C70550000E010005001B0001000000004200".hexStringToByteArray(),
        "7409C705500017010005001B0001000000004400".hexStringToByteArray(),
        "7409F70552001F010005001D0001000000004600".hexStringToByteArray(),
        "7409C705500027010005001B0001000000004800".hexStringToByteArray(),
        "7409F705520030010005001D0001000000004A00".hexStringToByteArray(),
        "74090806540038010005001E0002000000004C00".hexStringToByteArray(),
        "74090806540041010005001E0002000000004E00".hexStringToByteArray(),
        "7409380656004901000500200002000000005000".hexStringToByteArray(),
        "7409580658005201000500220002000000005200".hexStringToByteArray(),
        "7409580658005B01000500220002000000005400".hexStringToByteArray(),
        "7409380656006401000500200002000000005600".hexStringToByteArray(),
        "7409380656006D01000500200002000000005800".hexStringToByteArray(),
        "7409580658007601000500220002000000005A00".hexStringToByteArray(),
        "7409580658007F01000500220002000000005C00".hexStringToByteArray(),
        "7409380656008801000500200002000000005E00".hexStringToByteArray(),
        "7409580658009101000500220002000000006000".hexStringToByteArray(),
        "7409380656009A01000500200002000000006200".hexStringToByteArray(),
        "740958065800A301000500220002000000006400".hexStringToByteArray(),
        "740938065600AC01000500200002000000006600".hexStringToByteArray(),
        "740908065400B5010005001E0002000000006800".hexStringToByteArray(),
        "740958065800BD01000500220002000000006A00".hexStringToByteArray(),
        "740958065800C601000500220003000000006C00".hexStringToByteArray(),
        "740958065800CF01000500220003000000006E00".hexStringToByteArray(),
        "740958065800D801000500220003000000007000".hexStringToByteArray(),
        "740988065A00E101000500230003000000007200".hexStringToByteArray()
    )
}
