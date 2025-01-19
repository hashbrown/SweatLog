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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var userDataRepo: UserDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appState = rememberSLAppState(userDataRepo)
            val startBLEService = {
                val intent = Intent(this, BLEDeviceServiceImpl::class.java)
                ContextCompat.startForegroundService(this, intent)
            }
            SweatLogTheme {
                SLApp(appState, startBLEService)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val serviceIntent = Intent(this, BLEDeviceServiceImpl::class.java)
        stopService(serviceIntent)
    }
}
