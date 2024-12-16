package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.model.Device
import com.bitfarmsoftware.sweatlog.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {

    /**
     * Stream of [UserData]
     */
    val userData: Flow<UserData>

    suspend fun saveDevice(device: Device)
}