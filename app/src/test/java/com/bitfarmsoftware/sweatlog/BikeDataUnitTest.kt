package com.bitfarmsoftware.sweatlog

import com.bitfarmsoftware.sweatlog.data.ble.hexStringToByteArray
import com.bitfarmsoftware.sweatlog.model.toTrainingData
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BikeDataUnitTest {
    @Test
    fun parses_indoor_bike_data() {
        val hexStr = "740977054C0061000005001A0000000000001A00"
        val bytes = hexStr.hexStringToByteArray()
        val logData = bytes.toTrainingData()
        assertEquals(5.toShort(), logData.resistance)

    }

    @Test
    fun parses_bytes_to_int() {
        val hexStr = "740977054C0061000005001A0000000000001A00"
        val bytes1 = byteArrayOf(0,26)
        val bytes2 = byteArrayOf(26,0)

        val result1 = ((bytes1[0].toInt() and 0xFF) or
                ((bytes1[1].toInt() and 0xFF) shl 8))

        val result2 = ((bytes2[0].toInt() and 0xFF) or
                ((bytes2[1].toInt() and 0xFF) shl 8))

        assertEquals(result1, result2)

    }
}