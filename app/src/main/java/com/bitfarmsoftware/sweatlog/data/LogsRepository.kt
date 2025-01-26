package com.bitfarmsoftware.sweatlog.data

import androidx.activity.result.contract.ActivityResultContract
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.data.db.SweatLog
import com.bitfarmsoftware.sweatlog.model.RepoResource
import com.bitfarmsoftware.sweatlog.data.db.WorkoutData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


interface LogsRepository  {

    val requiredPermissions: Set<String>
    val hasAllPermissions: StateFlow<Boolean>
    val sweatLogs: Flow<RepoResource<List<SweatLog>>>

    fun getSweatLog(id: Int): Flow<RepoResource<SweatLog>>
    suspend fun checkPermissions()
    fun requestHealthConnectPermissions(): ActivityResultContract<Set<String>, Set<String>>
    suspend fun startNewWorkout(device: BleDeviceResource): Flow<RepoResource<WorkoutData>>
    suspend fun finishWorkout()
}



