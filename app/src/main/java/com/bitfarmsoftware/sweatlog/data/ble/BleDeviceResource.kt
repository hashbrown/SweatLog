package com.bitfarmsoftware.sweatlog.data.ble

import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.GattClientScope
import androidx.bluetooth.GattService
import com.bitfarmsoftware.sweatlog.data.ble.reference.AllGattCharacteristics
import com.bitfarmsoftware.sweatlog.data.ble.reference.AllGattServices
import com.bitfarmsoftware.sweatlog.data.ble.reference.GattCharacteristics
import com.bitfarmsoftware.sweatlog.data.ble.reference.GattServices
import com.bitfarmsoftware.sweatlog.data.ble.reference.uuid
import com.bitfarmsoftware.sweatlog.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class BleDeviceResource(val bleDevice: BluetoothDevice,
                        override val name: String,
                        override val address: String,
                        override val uuid: String
) : Device {

    var connected = MutableStateFlow<Boolean>(false)

    private val _bikeData = MutableStateFlow<Flow<ByteArray>>(emptyFlow())
    @OptIn(ExperimentalCoroutinesApi::class)
    val bikeData: Flow<ByteArray> = _bikeData.flatMapLatest { it } // Automatically switch flows

    private var gattServices = emptyList<GattService>()

    private lateinit var gattClient: GattClientScope

    fun onConnected(gattClientScope: GattClientScope, coroutineScope: CoroutineScope) {
        Timber.i("Device connected: ${this.name}")
        gattClient = gattClientScope
        connected.update {true}

    }

    fun onDisconnected() {
        Timber.i("Device disconnected: ${this.name}")
        connected.update {false}
    }

    suspend fun startSession() {
        Timber.i("Starting Session")
         val fmService = gattClient.getService(GattServices.FITNESS_MACHINE.uuid())
         if (fmService != null){
             val fmcp = fmService.getCharacteristic(GattCharacteristics.FITNESS_MACHINE_CONTROL_POINT.uuid())
             if(fmcp != null) {
//                 Timber.i("Requesting Reset")
//                 val resetResult = gattClient.writeCharacteristic(fmcp, "0x01".hexStringToByteArray())
                 Timber.i("Requesting Start")
                 // send "start" command
                 val startResult = gattClient.writeCharacteristic(fmcp, "0x07".hexStringToByteArray())
                 Timber.i("Session Started")
                 connectBikeData()

             } else {
                 Timber.w("Could not get Fitness Machine Control Point Characteristic")
             }
         } else {
             Timber.w("StartSession: Cannot get Fitness Machine Service")
         }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun connectBikeData() {
        _bikeData.value = gattClient.getService(GattServices.FITNESS_MACHINE.uuid())
                ?.getCharacteristic(GattCharacteristics.INDOOR_BIKE_DATA.uuid())
                ?.let { bikeCharacteristic ->
                    Timber.i("Found Bike Characteristic, subscribing")
                    gattClient.subscribeToCharacteristic(
                        bikeCharacteristic
                    )
                        .onEach { Timber.i("Raw BikeData:: ${it.toHexString()}") }
                } ?: emptyFlow() // Return an empty flow if characteristic is null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun logGattInfo(scope: GattClientScope, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            scope.servicesFlow
                .collect { currentServices ->
                    // Determine added and removed services
                    val addedServices = currentServices - gattServices.toSet()
                    val removedServices = gattServices - currentServices.toSet()

                    // Log added services
                    addedServices.forEach { service ->
                        Timber.i(
                            "Added Service UUID: ${service.uuid} :: NAME: ${
                                AllGattServices.lookup(
                                    service.uuid
                                )
                            }"
                        )
                        service.characteristics.forEach { characteristic ->
                            Timber.i(
                                " UUID: ${characteristic.uuid} :: NAME: ${
                                    AllGattCharacteristics.lookup(
                                        characteristic.uuid
                                    )
                                }"
                            )

                            val result = gattClient.readCharacteristic(characteristic)
                            if (result.isSuccess) {
                                Timber.i(" Value: ${result.getOrNull()?.parsedValue()}")
                            }
                        }
                    }

                    // Log removed services
                    removedServices.forEach { service ->
                        Timber.i(
                            "Removed Service UUID: ${service.uuid} :: NAME: ${
                                AllGattServices.lookup(
                                    service.uuid
                                )
                            }"
                        )
                    }

                    // Update previous state
                    gattServices = currentServices
                }
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDeviceResource

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