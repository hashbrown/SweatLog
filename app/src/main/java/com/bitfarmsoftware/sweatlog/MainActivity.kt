package com.bitfarmsoftware.sweatlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.bitfarmsoftware.sweatlog.data.UserDataRepository
import com.bitfarmsoftware.sweatlog.service.BLEDeviceServiceImpl
import com.bitfarmsoftware.sweatlog.ui.SLApp
import com.bitfarmsoftware.sweatlog.ui.rememberSLAppState
import com.bitfarmsoftware.sweatlog.ui.theme.SweatLogTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userDataRepo: UserDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Timber.i("MainActivity onCreate()")

        setContent {
            val appState = rememberSLAppState(userDataRepo)
            val appContext = applicationContext
            val startBLEService = {
                val intent = Intent(appContext, BLEDeviceServiceImpl::class.java)
                ContextCompat.startForegroundService(appContext, intent)
            }

            val exitApp = {
                Timber.i("Exiting app")
                val intent = Intent(this, BLEDeviceServiceImpl::class.java)
                stopService(intent)
                Timber.i("Calling finish()")
                finish()
            }
            SweatLogTheme {
                SLApp(appState, startBLEService, exitApp)
            }
        }
    }

    override fun onDestroy() {
        Timber.i("MainActivity onDestroy()")
        super.onDestroy()
    }
}
