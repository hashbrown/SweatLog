package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.service.BleDeviceService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject constructor(
//    private val bleDeviceService: BleDeviceService // Inject interface
) {

    init{
        Timber.i("Initializing LogsRepo")
    }

//    fun observeLogData(device: BleDeviceResource): Flow<LogData> {
//        Timber.i("Request to observe LogData to Device that is connected? ${device.connected}")
//        return device.bikeData
//            .onEach { Timber.i("bikeData is emitting: ${it}") }
//            .filter { it.isNotEmpty() }
//            .map { data -> data.toLogData() }
//    }
}

