package com.tencentcs.iotvideo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tencentcs.iotvideo.ui.theme.CustomNativeIotVideoTheme
import com.tencentcs.iotvideo.utils.LogUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import kotlin.concurrent.fixedRateTimer

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val userInfo = hashMapOf<String, Any>()
        userInfo["IOT_HOST"] = "|wyze-mars-asrv.wyzecam.com"
        IoTVideoSdk.init(this, userInfo)
    }
}

class CameraViewModel : ViewModel() {
    private val _cameraList = MutableLiveData<ArrayList<CameraToRtspPlayer>>()
    val cameraList: MutableLiveData<ArrayList<CameraToRtspPlayer>> = _cameraList

    init {
        _cameraList.value = ArrayList()
    }

    fun addCamera(camera: CameraToRtspPlayer) {
        val currentList = _cameraList.value
        currentList?.add(camera)
        _cameraList.value = currentList
    }

    fun popCamera(): CameraToRtspPlayer? {
        val currentList = _cameraList.value
        return currentList?.removeAt(0)
    }

    fun cameraCount(): Int {
        return _cameraList.value?.size ?: 0
    }

    fun getStatusMessage(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("There are " + cameraCount() + " cameras active\n")
        for (camera in _cameraList.value!!) {
            stringBuilder.append(camera.toString())
        }
        return stringBuilder.toString()
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    private val TAG: String = "MainActivityIot"

    var cryzeApi: String = "http://cryze_api:8080"

    val client = OkHttpClient()
    private val context = this // so i can pass it to the CameraViewer class... maybe not the best idea

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomNativeIotVideoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StatusMessage(":) Version: " + IoTVideoSdk.getP2PVersion())
                }
            }
        }

        viewModel.cameraList.observe(this) { _ ->
            setContent{
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StatusMessage(viewModel.getStatusMessage())
                }
            }
        }

        // every 30 seconds, get the camera ids from the server
        fixedRateTimer("callbackTimer", initialDelay = 1_000, period = 1_000) {
            runOnUiThread {
                setContent{
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StatusMessage(viewModel.getStatusMessage())
                    }
                }
            }
        }

        // start getting frames hooked up
        getCameraIdsFromServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        // stop all the threads
        while (viewModel.cameraCount() > 0)
        {
            val camera = viewModel.popCamera()
            camera?.playerThread?.interrupt()
        }

        IoTVideoSdk.unRegister()
        IoTVideoSdk.getMessageMgr().removeAppLinkListeners()
        IoTVideoSdk.getMessageMgr().removeModelListeners()
    }

    private fun getCameraIdsFromServer(): Unit
    {
        val requestUrl = "$cryzeApi/getCameraIds"
        val request = Request.Builder()
            .url(requestUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                throw IOException("Request failed: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                // the response is a json array of camera ids. parse it into a list
                val cameraIds = JSONArray(responseBody)
                for (i in 0 until cameraIds.length())
                {
                    val cameraId = cameraIds.getString(i)
                    LogUtils.i(TAG, "Camera ID: $cameraId")

                    var camera = CameraToRtspPlayer(cameraId, context)

                    runOnUiThread {

                        viewModel.addCamera(camera)
                    }

                    camera.start()
                }
            }
        })
    }
}

@Composable
fun StatusMessage(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CustomNativeIotVideoTheme {
        StatusMessage("Android")
    }
}