package com.bitfarmsoftware.sweatlog

import android.bluetooth.BluetoothAdapter
import androidx.bluetooth.BluetoothDevice
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.ui.LogDataView
import com.bitfarmsoftware.sweatlog.ui.LogUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertIs

class FlowTests {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }


    @Test
    fun combineFlows(): TestResult {

        val device = SavedDevice(
            "test device",
            "add-res-s",
            "uuid-1111"
        )
        val deviceFlow = flow {
            emit(DeviceState.WaitingToConnect)
            delay(2000)
            emit(DeviceState.Connected(device))
        }

        val permissionFlow = flow {
            emit(false)
            delay(500)
            emit(true)
        }

        val trainingFlow = flow {
            emit(LogDataView(power="100"))
            emit(LogDataView(power="200"))
            emit(LogDataView(power="300"))
        }


        val flatmapFlow = permissionFlow.flatMapLatest { hasPerm ->

            if (!hasPerm) {
                flowOf(LogUiState.NeedsPermissions)
            } else {
                deviceFlow.flatMapLatest { state ->

                    when (state) {
                        is DeviceState.WaitingToConnect ->
                            flowOf(LogUiState.Loading)

                        is DeviceState.Connected ->

                            trainingFlow.map { logData ->
                                LogUiState.Success(LogDataView(power = logData.power, speed = "15"))
                            }
                    }

                }
            }
        }

        return runTest {
//            val result = combinedFlow.toList()
//            assertEquals(3, result.size)
//            assertEquals("falseWAITING", result.get(0))
//            assertEquals("trueWAITING", result.get(1))
//            assertEquals("true" + device, result.get(2))

            val flatmapResult = flatmapFlow
                .onEach({System.out.println(it)}).toList()
            assertEquals(5, flatmapResult.size)
            assertEquals(LogUiState.NeedsPermissions, flatmapResult.get(0))
            assertEquals(LogUiState.Loading, flatmapResult.get(1))
            val success = flatmapResult.get(2)
            assertIs<LogUiState.Success>(success)
            assertEquals("15", success.logData.speed)
        }
    }
}

private sealed interface DeviceState {
    data object WaitingToConnect: DeviceState
    data class Connected(
        val device: Device
    ): DeviceState
}