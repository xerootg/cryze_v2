package com.github.xerootg.cryze

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.xerootg.cryze.httpclient.CryzeHttpClient
import com.github.xerootg.cryze.restreamer.RestreamingVideoPlayer
import com.github.xerootg.cryze.restreamer.interfaces.ICameraStream
import com.tencentcs.iotvideo.IoTVideoSdk
import com.github.xerootg.cryze.theme.CryzeStatusScreenTheme
import com.tencentcs.iotvideo.messagemgr.MessageMgr
import kotlin.concurrent.fixedRateTimer

class MainApplication : Application() {
}

class CameraViewModel : ViewModel() {
    private val _cameraList = MutableLiveData<ArrayList<ICameraStream>>()

    init {
        _cameraList.value = ArrayList()
    }

    fun addCamera(camera: ICameraStream) {
        val currentList = _cameraList.value
        currentList?.add(camera)
        _cameraList.value = currentList!!
    }

    fun popCamera(): ICameraStream? {
        val currentList = _cameraList.value
        return currentList?.removeAt(0)
    }

    fun cameraCount(): Int {
        return _cameraList.value?.size ?: 0
    }

    fun getStatusMessage(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("There are " + cameraCount() + " cameras active:")
        for (camera in _cameraList.value!!) {
            stringBuilder.appendLine(camera.toString())
        }
        return stringBuilder.toString()
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    private val context = this // so i can pass it to the CameraViewer class... maybe not the best idea

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial content while loading
        setContent {
            CryzeStatusScreenTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(
                        text = ":) Version: ${IoTVideoSdk.p2PVersion} Loading camera streams..."
                    )
                }
            }
        }

        // every 30 seconds, get the camera ids from the server
        fixedRateTimer("UIUpdater", initialDelay = 3_000, period = 1_000) {
            runOnUiThread {
                setContent {
                    CryzeStatusScreenTheme {
                        Surface( // A surface container using the 'background' color from the theme
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column( // a scroll area for the status messages
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(text = viewModel.getStatusMessage())
                            }
                        }
                    }
                }
            }
        }

        // start getting frames hooked up
        CryzeHttpClient.getCameras().forEach {
            Thread {

                // IOTVideoSDK uses context for:
                // - file storage path for p2p CB files (it never works??)
                // - CONNECTIVITY_CHANGE broadcast receiver
                val deviceInfo = CryzeHttpClient.getCameraInfo(it)

                val camera = RestreamingVideoPlayer(deviceInfo, context)
                camera.start()
                runOnUiThread {
                    viewModel.addCamera(camera)
                }
            }.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop all the threads
        while (viewModel.cameraCount() > 0)
        {
            var camera: ICameraStream? = null
            runOnUiThread {
                camera = viewModel.popCamera()
            }
            camera?.stop()
            camera?.release()
        }

        IoTVideoSdk.unRegister()
        MessageMgr.removeAppLinkListeners()
        MessageMgr.removeModelListeners()
    }
}

