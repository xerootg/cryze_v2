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
import com.tencentcs.iotvideo.bitmapstream.CameraToImagePlayerFactory
import com.tencentcs.iotvideo.custom.ServerType
import com.tencentcs.iotvideo.h264streamer.CameraToH264StreamPlayerFactory
import com.tencentcs.iotvideo.iotvideoplayer.ConnectMode
import com.tencentcs.iotvideo.rtsp.CameraToRtspPlayer
import com.tencentcs.iotvideo.rtsp.CameraToRtspPlayerFactory
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
        userInfo["IOT_P2P_PORT_TYPE"] = ConnectMode.Mode.LAN
        IoTVideoSdk.init(this, userInfo)
    }
}

class CameraViewModel : ViewModel() {
    private val _cameraList = MutableLiveData<ArrayList<ICameraStream>>()
    val cameraList: MutableLiveData<ArrayList<ICameraStream>> = _cameraList

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

    fun removeCamera(cameraId: String) {
        val currentList = _cameraList.value
        val removed = currentList?.removeIf {
            it.cameraId == cameraId
        }
        if (removed == true) LogUtils.i("CameraViewModel", "Removed camera $cameraId") else LogUtils.i("CameraViewModel", "Failed to remove camera $cameraId")
        _cameraList.value = currentList!!
    }
}

class MainActivity : ComponentActivity() {

    val viewModel: CameraViewModel by viewModels()

    private val TAG: String = "MainActivityIot"

    var cryzeApi: String = "http://cryze_api:8080" // I really need to find a better way to do this

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
        fixedRateTimer("callbackTimer", initialDelay = 5_000, period = 5_000) {
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
            camera?.stop()
            camera?.release()
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
                    // its a kvp arrangement: [{"Key":"GW_BE1_LONGNAMEHERE","ServerType":"RAW"},{"Key":"GW_BE1_ANOTHERONE","ServerType":"RAW"}]
                    val cameraId = cameraIds.getJSONObject(i).getString("Key")
                    val serverType = ServerType.fromValue(cameraIds.getJSONObject(i).getString("ServerType"))

                    LogUtils.i(TAG, "Camera ID: $cameraId Server Type: $serverType")
                    when(serverType)
                    {
                        ServerType.RAW -> CameraToH264StreamPlayerFactory(cameraId, context).register()
                        ServerType.MJPEG -> CameraToImagePlayerFactory(cameraId, context).register()
                        ServerType.RTSP -> CameraToRtspPlayerFactory(cameraId, context).register()
                        // always when in doubt, register as a rtsp server
                        else -> CameraToRtspPlayerFactory(cameraId, context).register()
                    }
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