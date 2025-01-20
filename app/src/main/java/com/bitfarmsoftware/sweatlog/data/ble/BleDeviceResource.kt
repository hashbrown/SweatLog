package com.bitfarmsoftware.sweatlog.data.ble

import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.GattClientScope
import com.bitfarmsoftware.sweatlog.data.ble.reference.GattCharacteristics
import com.bitfarmsoftware.sweatlog.data.ble.reference.GattServices
import com.bitfarmsoftware.sweatlog.data.ble.reference.uuid
import com.bitfarmsoftware.sweatlog.model.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class BleDeviceResource(val bleDevice: BluetoothDevice,
                        override val name: String,
                        override val address: String,
                        override val uuid: String
) : Device {

    var connected = MutableStateFlow<Boolean>(false)
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

    suspend fun getBikeDataFlow(): Flow<ByteArray>{
        Timber.i("getBikeDataFlow()")
        return gattClient.getService(GattServices.FITNESS_MACHINE.uuid())
            ?.getCharacteristic(GattCharacteristics.INDOOR_BIKE_DATA.uuid())
            ?.let { bikeCharacteristic ->

                bikeCharacteristic.fwkCharacteristic
                Timber.i("Found Bike Characteristic, subscribing")
                gattClient.subscribeToCharacteristic(
                    bikeCharacteristic
                )
//                        .onEach { Timber.i("Raw BikeData:: ${it.toHexString()}") }
            }?: emptyFlow() // Return an empty flow if characteristic is null
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