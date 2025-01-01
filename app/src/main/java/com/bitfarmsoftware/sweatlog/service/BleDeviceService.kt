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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.bitfarmsoftware.sweatlog.data.ble.BleDeviceResource
import com.bitfarmsoftware.sweatlog.model.TrainingData
import com.bitfarmsoftware.sweatlog.model.toTrainingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


interface BleDeviceService {
    val scanning: SharedFlow<Boolean>
    val connectedDevice: SharedFlow<BleDeviceResource>
    val availableDevices: SharedFlow<BleDeviceResource>

    suspend fun scanDevices()
    suspend fun stopScan()
    suspend fun connectDevice(device: BleDeviceResource)
    suspend fun startTrainingSession(device: BleDeviceResource): Flow<TrainingData>

}

class BleDeviceServiceProvider @Inject constructor(private val context: Context) {

//    val service: BleDeviceService by lazy {
//        withContext(Dispatchers.Default) {
//            Timber.i("Starting provider service field lazy init")
//
//            bindService().also {
//                Timber.i("Finished provider service field lazy init")
//            }
//        }
//    }
//
//    private val scope = CoroutineScope(Job())

    private lateinit var service: BleDeviceService

    suspend fun getService() = suspendCoroutine<BleDeviceService> { continuation ->
        if (this::service.isInitialized) {
            Timber.i("Service already initialized")
            continuation.resume(service)
        }

        val conn = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as BLEDeviceServiceBinder
                Timber.i("Resuming bindService coroutine")
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


//    sealed interface ServiceState {
//        data object CONNECTING: ServiceState
//        data object DISCONNECTED: ServiceState
//        data class CONNECTED(val service:BleDeviceService): ServiceState
//    }
//
//    private val _serviceState = MutableSharedFlow<ServiceState>(replay = 1)
//    val serviceState: SharedFlow<ServiceState> = _serviceState
//
//    private val _service = MutableSharedFlow<BleDeviceService>(replay = 1)
//    val service: SharedFlow<BleDeviceService> = _service


//    /** Defines callbacks for service binding, passed to bindService().  */
//    private val connection = object : ServiceConnection {
//
//        override fun onServiceConnected(className: ComponentName, service: IBinder) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance.
//            val binder = service as BLEDeviceServiceBinder
//            scope.launch {
//                _serviceState.emit(ServiceState.CONNECTED(binder.service))
//            }
//        }
//
//        override fun onServiceDisconnected(arg0: ComponentName) {
//            scope.launch {
//                _serviceState.emit(ServiceState.DISCONNECTED)
//            }
//        }
//    }
//
//    init {
//        Intent(context, BLEDeviceServiceImpl::class.java).also { intent ->
//            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
//        }
//    }


}

data class BLEDeviceServiceBinder(val service: BleDeviceService): Binder()


@Singleton
class BLEDeviceServiceImpl @Inject constructor() : Service(), BleDeviceService {

    companion object {
        private const val CHANNEL_ID = "BLEDeviceServiceChannel"
        private const val NOTIFICATION_ID = 100
        private var isRunning = false // Singleton flag for the process lifetime
    }

    private val _scanning = MutableSharedFlow<Boolean>(replay = 1)
    override val scanning: SharedFlow<Boolean> = _scanning

    private val _connectedDevice = MutableSharedFlow<BleDeviceResource>(replay = 1)
    override val connectedDevice: SharedFlow<BleDeviceResource> = _connectedDevice

    private val _availableDevices = MutableSharedFlow<BleDeviceResource>(replay = 1)
    override val availableDevices: SharedFlow<BleDeviceResource> = _availableDevices

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var ble: BluetoothLe
    private lateinit var bleDeviceResource: BleDeviceResource
    private var scanJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Timber.i("BLEDeviceService onCreate() called")
        ble = BluetoothLe(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("BLEDeviceService onStartCommand() called")

        // Check if the service is already running
        if (isRunning) {
            Timber.i("BLEDeviceService is already running; skipping initialization")
            return START_STICKY
        }

        // Mark as running only AFTER starting foreground
        startForeground()
        isRunning = true // Set the flag here

        Timber.i("BLEDeviceService successfully started in foreground")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.i("Service ONBIND()")
        return BLEDeviceServiceBinder(this@BLEDeviceServiceImpl)
    }

    @SuppressLint("MissingPermission")
    override suspend fun scanDevices() {

        scanJob?.cancel()
        scanJob = serviceScope.launch {
            Timber.i("Starting scanner")
            _scanning.emit(true)
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

        serviceScope.launch {
            delay(30_000)
            Timber.i("Cancelling Scan Job")
            scanJob?.cancel()
            _scanning.emit(false)
        }
    }

    override suspend fun stopScan() {

    }

    @SuppressLint("MissingPermission")
    override suspend fun connectDevice(device: BleDeviceResource) {
        if (!device.connected.value) {
            serviceScope.launch {
                ble.connectGatt(device.bleDevice) {
                    device.onConnected(this, serviceScope)
                    _connectedDevice.emit(device)
                    updateNotification("Connected to ${device.name}")
                    try {
                        awaitCancellation()
                    } finally {
                        Timber.i("connectDevice coroutine is cancelled")
                        device.onDisconnected()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startTrainingSession(device: BleDeviceResource): Flow<TrainingData> {
        device.startSession()
        return device.bikeData
            .filter { it.isNotEmpty() }
            .map { data -> data.toTrainingData() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bleDeviceResource.isInitialized) {
            bleDeviceResource.onDisconnected()
        }
    }


    private fun startForeground() {
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SweatLog BLE Service")
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
            "BLE Connection Service",
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
            .setContentTitle("SweatLog BLE Service")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

}