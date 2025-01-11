package com.bitfarmsoftware.sweatlog.data

import androidx.activity.result.contract.ActivityResultContract
import com.bitfarmsoftware.sweatlog.data.healthconnect.HealthConnectDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogsRepository @Inject constructor(
    private val healthConnect: HealthConnectDataStore // Inject interface
) {

    val requiredPermissions = healthConnect.permissions

    private var _hasPermissions = MutableStateFlow(false)
    val hasAllPermissions: StateFlow<Boolean> = _hasPermissions

    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    suspend fun checkPermissions() {
        val hasPermissionsResult = healthConnect.hasAllPermissions()
        Timber.i("checkPermissionsResult=${hasPermissionsResult}")
        _hasPermissions.value = hasPermissionsResult
    }

    fun requestHealthConnectPermissions(): ActivityResultContract<Set<String>, Set<String>> {
        return healthConnect.requestPermissionsActivityContract()
    }

}

