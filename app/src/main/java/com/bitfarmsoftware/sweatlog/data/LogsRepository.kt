package com.bitfarmsoftware.sweatlog.data

import androidx.compose.runtime.collectAsState
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.LogData
import com.bitfarmsoftware.sweatlog.model.toLogData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject constructor() {

    fun observeLogData(device: BleDeviceResource): Flow<LogData> {
        Timber.i("Request to observe LogData to Device that is connected? ${device.connected}")
        return device.bikeData
            .onEach { Timber.i("bikeData is emitting: ${it}") }
            .filter { it.isNotEmpty() }
            .map { data -> data.toLogData() }
    }
}

