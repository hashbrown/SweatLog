package com.bitfarmsoftware.sweatlog.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.bluetooth.BluetoothLe
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.bitfarmsoftware.sweatlog.data.UserDataRepository
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.IndoorBikeData
import com.bitfarmsoftware.sweatlog.model.SavedDevice
import com.bitfarmsoftware.sweatlog.model.WorkoutData
import com.bitfarmsoftware.sweatlog.model.toTrainingData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


interface BleDeviceService {
    val scanning: SharedFlow<Boolean>
    val connectedDevice: SharedFlow<BleDeviceResource>
    val availableDevices: SharedFlow<BleDeviceResource>

    suspend fun scanDevices()
    suspend fun stopScan()
    suspend fun connectDevice(device: BleDeviceResource)
    suspend fun disconnectDevice(device: BleDeviceResource)
    suspend fun startTrainingSession(): Flow<IndoorBikeData>
    suspend fun stopTrainingSession()

}

class BleDeviceServiceProvider @Inject constructor(private val context: Context) {

    private lateinit var service: BleDeviceService

    suspend fun getService() = suspendCoroutine<BleDeviceService> { continuation ->
        if (this::service.isInitialized) {
            Timber.i("Service already initialized")
            continuation.resume(service)
        }

        val conn = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as BLEDeviceServiceBinder
                Timber.i("Binder: onServiceConnected")
                continuation.resume(binder.service)
            }
            override fun onServiceDisconnected(name: ComponentName?) {
            }

        }
        Timber.i("Starting bindService coroutine")
        Intent(context, BLEDeviceServiceImpl::class.java).also { intent ->
            context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        }
    }
}

data class BLEDeviceServiceBinder(val service: BleDeviceService): Binder()


@AndroidEntryPoint
class BLEDeviceServiceImpl @Inject constructor() : Service(), BleDeviceService {

    companion object {
        private const val CHANNEL_ID = "BLEDeviceServiceChannel"
        private const val NOTIFICATION_ID = 100
        private var isRunning = false // Singleton flag for the process lifetime
    }

    @Inject
    lateinit var userDataRepo: UserDataRepository

    private val _scanning = MutableStateFlow<Boolean>(false)
    override val scanning: StateFlow<Boolean> = _scanning

    private val _connectedDevice = MutableSharedFlow<BleDeviceResource>(replay = 1)
    override val connectedDevice: SharedFlow<BleDeviceResource> = _connectedDevice

    private val _availableDevices = MutableSharedFlow<BleDeviceResource>(replay = 1)
    override val availableDevices: SharedFlow<BleDeviceResource> = _availableDevices

    private val _bikeData = MutableSharedFlow<IndoorBikeData>(replay = 1)
    private val bikeData: SharedFlow<IndoorBikeData> = _bikeData

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var ble: BluetoothLe
    private var bleDeviceResource: BleDeviceResource? = null
    private var scanJob: Job? = null
    private var scanCancelJob: Job? = null
    private var connectJob: Job? = null
    private var autoconnectJob: Job? = null
    private var workoutJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("BLEDeviceService onCreate() called")
        ble = BluetoothLe(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("BLEDeviceService onStartCommand() called")

        // Check if the service is already running
        if (isRunning) {
            Timber.i("BLEDeviceService is already running; skipping initialization")
            return START_STICKY
        }

        // Mark as running only AFTER starting foreground
        startForeground()
        isRunning = true

        autoconnectJob = serviceScope.launch {
            runAutoConnect()
        }

        Timber.i("BLEDeviceService successfully started in foreground")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.i("Service ONBIND()")
        return BLEDeviceServiceBinder(this@BLEDeviceServiceImpl)
    }

    override fun onDestroy() {
        Timber.i("Service ONDESTROY()")
        bleDeviceResource?.onDisconnected()
        serviceScope.cancel("BLEService is being destroyed")
        super.onDestroy()
    }


    @SuppressLint("MissingPermission")
    override suspend fun scanDevices() {

        // always reset the cancel timer
        scanCancelJob?.cancel()
        if (_scanning.compareAndSet(false, true)) {
            scanJob = serviceScope.launch {
                Timber.i("Starting scanner")
                ble.scan()
    //                .onEach { Timber.d("Raw scan result: ${it.device.name}") }
                    .filter { it.device.name != null }
                    .map { result ->
                        BleDeviceResource(
                            bleDevice = result.device,
                            name = result.device.name!!,
                            address = result.deviceAddress.address,
                            uuid = result.device.id.toString(),
                        )
                    }
                    .collect { device ->
                        _availableDevices.emit(device) // Emit scan result
                    }
            }
        }
        cancelScan(30_000)
    }

    override suspend fun stopScan() {
        Timber.i("Received StopScan request")
        cancelScan(0)
    }

    @SuppressLint("MissingPermission")
    override suspend fun connectDevice(device: BleDeviceResource) {
        if (!device.connected.value && connectJob == null) {
            Timber.i("Connecting device ${device.name} addr=${device.address}")
            connectJob = serviceScope.launch {
                ble.connectGatt(device.bleDevice) {
                    bleDeviceResource = device
                    device.onConnected(this, serviceScope)
                    _connectedDevice.emit(device)
                    updateNotification("Connected to ${device.name}")
                    try {
                        awaitCancellation()
                    } finally {
                        Timber.i("connectDevice coroutine is cancelled")
                        device.onDisconnected()
                        updateNotification("Device Not Connected")
                        bleDeviceResource = null
                    }
                }
            }
        } else {
            Timber.i("Ignoring Connect for Connected device ${device.name}")
        }
    }

    override suspend fun disconnectDevice(device: BleDeviceResource) {
        Timber.i("Disconnecting device ${device.name}")
        connectJob?.cancel()
        updateNotification("Device Not Connected")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    override suspend fun startTrainingSession(): Flow<IndoorBikeData> {

        if (bleDeviceResource == null) {
            throw IllegalStateException("Device not connected")
        }

        val device = bleDeviceResource!!

        workoutJob?.cancel()
        workoutJob = serviceScope.launch {
            device.getBikeDataFlow()
            .filter { it.isNotEmpty() }
            .map { data -> data.toTrainingData() }
            .onStart { Timber.i("BleService:: bikeData flow started") }
            .onCompletion { Timber.i("BleService:: bikeData flow completed") }
            .collect { _bikeData.emit(it) }
        }

        return bikeData
    }

    override suspend fun stopTrainingSession() {
        workoutJob?.cancel()
    }


    private suspend fun cancelScan(delayMs: Long) {
        scanCancelJob?.cancel()
        scanCancelJob = serviceScope.launch {
            delay(delayMs)
            Timber.i("Cancelling Scan Job")
            scanJob?.cancel()
            _scanning.value = false
        }
    }

    private suspend fun runAutoConnect() {
        Timber.i("Invoked runAutoConnect()")
        userDataRepo.userData
            .map { it.savedDevices.filter{savedDevice -> savedDevice.autoConnect}}
            .onEmpty {
                Timber.i("No autoconnect devices found")
                emptyFlow<List<SavedDevice>>()
            }
            .collect {
                devices -> autoconnectDevice(devices.first())
            }
    }

    private suspend fun autoconnectDevice(device: SavedDevice){

        Timber.i("Attempting autoconnect for device ${device.name}")
        scanDevices()
        availableDevices
            .filter { scanDevice -> scanDevice.name == device.name
                    && scanDevice.address == device.address}
            .collect {
                foundDevice -> connectDevice(foundDevice)
            }.also {
                Timber.i("Found device for autoconnect")
                stopScan()
            }
    }


    private fun startForeground() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SweatLog App")
                .setContentText("Waiting for connection...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID, // Notification ID
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )

            Timber.i("StartForeground() completed")
        } catch (e: Exception) {
            if (e is ForegroundServiceStartNotAllowedException) {
                Timber.w("APP not in valid state to start in foreground.", e)
            } else {
                Timber.e("Exception starting foreground! ", e)
            }
        }
    }

    /**
     * Create a notification channel for the foreground service
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Device Connection Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Handles BLE device connections and data collection"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Update the notification with new content
     */
    @SuppressLint("MissingPermission")
    private fun updateNotification(content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SweatLog App")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

}