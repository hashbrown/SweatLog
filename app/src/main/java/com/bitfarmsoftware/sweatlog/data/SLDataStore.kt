package com.bitfarmsoftware.sweatlog.data

import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.UserData
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class SLPreferencesDataSource @Inject constructor(
//    private val userPreferences: DataStore<UserPreferences>,
) {

    private val savedDevice = SavedDevice(
        name = "DBF915",
        address = "E6:20:6F:F4:37:6E",
        uuid = "random uuid",
        autoConnect = true
    )

    val userData = flowOf(UserData(savedDevices = setOf(savedDevice)))

    fun saveUserData(data: UserData){

    }
}

